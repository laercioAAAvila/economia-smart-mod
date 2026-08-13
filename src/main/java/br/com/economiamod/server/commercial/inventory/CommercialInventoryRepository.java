package br.com.economiamod.server.commercial.inventory;

import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CommercialInventoryRepository {
    public void ensureSlots(UUID commercialBlockId, CommercialInventoryType inventoryType, int slotCount) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection()) {
            for (int slot = 0; slot < slotCount; slot++) {
                insertEmptySlot(connection, commercialBlockId, inventoryType, slot);
            }
        }
    }

    public List<CommercialInventorySlot> loadSlots(UUID commercialBlockId, CommercialInventoryType inventoryType) throws SQLException {
        String sql = """
                SELECT id, slot_index, item_id, item_count, item_components, item_data_version, version
                  FROM economy_inventory_slots
                 WHERE commercial_block_id = ?
                   AND inventory_type = ?
                 ORDER BY slot_index
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, commercialBlockId);
            statement.setString(2, inventoryType.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CommercialInventorySlot> slots = new ArrayList<>();
                while (resultSet.next()) {
                    slots.add(readSlot(resultSet, commercialBlockId, inventoryType));
                }
                return List.copyOf(slots);
            }
        }
    }

    public boolean updateSlot(UUID slotId, CommercialItemSnapshot item, long expectedVersion) throws SQLException {
        String sql = """
                UPDATE economy_inventory_slots
                   SET item_id = ?,
                       item_count = ?,
                       item_components = ?,
                       item_data_version = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                   AND version = ?
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.itemId());
            statement.setInt(2, item.count());
            statement.setString(3, item.components());
            if (item.dataVersion() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, item.dataVersion());
            }
            statement.setObject(5, slotId);
            statement.setLong(6, expectedVersion);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean[] updateSlots(List<SlotUpdate> updates) throws SQLException {
        if (updates.isEmpty()) {
            return new boolean[0];
        }

        String sql = """
                UPDATE economy_inventory_slots
                   SET item_id = ?,
                       item_count = ?,
                       item_components = ?,
                       item_data_version = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE id = ?
                   AND version = ?
                """;

        try (Connection connection = EconomyDatabase.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (SlotUpdate update : updates) {
                    bindUpdate(statement, update.item(), update.slotId(), update.expectedVersion());
                    statement.addBatch();
                }

                int[] results = statement.executeBatch();
                boolean[] saved = new boolean[results.length];
                boolean allSaved = results.length == updates.size();
                for (int index = 0; index < results.length; index++) {
                    saved[index] = results[index] == 1 || results[index] == Statement.SUCCESS_NO_INFO;
                    allSaved &= saved[index];
                }
                if (allSaved) {
                    connection.commit();
                    return saved;
                }
                connection.rollback();
                throw new SQLException("commercial inventory changed concurrently", "40001");
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    public void updateSlot(UUID commercialBlockId, CommercialInventoryType inventoryType, int slotIndex, CommercialItemSnapshot item) throws SQLException {
        String sql = """
                UPDATE economy_inventory_slots
                   SET item_id = ?,
                       item_count = ?,
                       item_components = ?,
                       item_data_version = ?,
                       updated_at = CURRENT_TIMESTAMP,
                       version = version + 1
                 WHERE commercial_block_id = ?
                   AND inventory_type = ?
                   AND slot_index = ?
                """;

        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.itemId());
            statement.setInt(2, item.count());
            statement.setString(3, item.components());
            if (item.dataVersion() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, item.dataVersion());
            }
            statement.setObject(5, commercialBlockId);
            statement.setString(6, inventoryType.name());
            statement.setInt(7, slotIndex);
            statement.executeUpdate();
        }
    }

    private void bindUpdate(PreparedStatement statement, CommercialItemSnapshot item, UUID slotId, long expectedVersion) throws SQLException {
        statement.setString(1, item.itemId());
        statement.setInt(2, item.count());
        statement.setString(3, item.components());
        if (item.dataVersion() == null) {
            statement.setNull(4, java.sql.Types.INTEGER);
        } else {
            statement.setInt(4, item.dataVersion());
        }
        statement.setObject(5, slotId);
        statement.setLong(6, expectedVersion);
    }

    private void insertEmptySlot(Connection connection, UUID commercialBlockId, CommercialInventoryType inventoryType, int slotIndex) throws SQLException {
        String sql = """
                INSERT INTO economy_inventory_slots(
                    id, commercial_block_id, inventory_type, slot_index, item_count, updated_at, version
                )
                VALUES (?, ?, ?, ?, 0, CURRENT_TIMESTAMP, 1)
                ON CONFLICT (commercial_block_id, inventory_type, slot_index) DO NOTHING
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, commercialBlockId);
            statement.setString(3, inventoryType.name());
            statement.setInt(4, slotIndex);
            statement.executeUpdate();
        }
    }

    private CommercialInventorySlot readSlot(ResultSet resultSet, UUID commercialBlockId, CommercialInventoryType inventoryType) throws SQLException {
        CommercialItemSnapshot item = new CommercialItemSnapshot(
                resultSet.getString("item_id"),
                resultSet.getInt("item_count"),
                resultSet.getString("item_components"),
                (Integer) resultSet.getObject("item_data_version")
        );
        return new CommercialInventorySlot(
                resultSet.getObject("id", UUID.class),
                commercialBlockId,
                inventoryType,
                resultSet.getInt("slot_index"),
                item,
                resultSet.getLong("version")
        );
    }

    public record SlotUpdate(UUID slotId, CommercialItemSnapshot item, long expectedVersion) {
    }
}
