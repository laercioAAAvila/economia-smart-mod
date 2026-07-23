package br.com.economiamod.server.commercial.inventory;

import br.com.economiamod.common.pricing.ComparisonMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CommercialInventoryRemovalRepository {
    private final CommercialItemMatcher matcher = new CommercialItemMatcher();

    public boolean remove(Connection connection, UUID blockId, CommercialInventoryType type, CommercialItemSnapshot item, int count) throws SQLException {
        List<CommercialInventorySlot> slots = lockSlots(connection, blockId, type);
        if (available(slots, item) < count) {
            return false;
        }
        int remaining = count;
        for (CommercialInventorySlot slot : slots) {
            if (!matcher.matches(slot.item(), item, ComparisonMode.FULL_COMPONENTS)) {
                continue;
            }
            int removed = Math.min(remaining, slot.item().count());
            updateCount(connection, slot, slot.item().count() - removed);
            remaining -= removed;
            if (remaining == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean canRemove(Connection connection, UUID blockId, CommercialInventoryType type, CommercialItemSnapshot item, int count) throws SQLException {
        return available(lockSlots(connection, blockId, type), item) >= count;
    }

    private int available(List<CommercialInventorySlot> slots, CommercialItemSnapshot item) {
        int total = 0;
        for (CommercialInventorySlot slot : slots) {
            if (matcher.matches(slot.item(), item, ComparisonMode.FULL_COMPONENTS)) {
                total += slot.item().count();
            }
        }
        return total;
    }

    private List<CommercialInventorySlot> lockSlots(Connection connection, UUID blockId, CommercialInventoryType type) throws SQLException {
        String sql = """
                SELECT id, slot_index, item_id, item_count, item_components, item_data_version, version
                  FROM economy_inventory_slots
                 WHERE commercial_block_id = ?
                   AND inventory_type = ?
                 ORDER BY slot_index
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, blockId);
            statement.setString(2, type.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CommercialInventorySlot> slots = new ArrayList<>();
                while (resultSet.next()) {
                    slots.add(readSlot(resultSet, blockId, type));
                }
                return slots;
            }
        }
    }

    private void updateCount(Connection connection, CommercialInventorySlot slot, int count) throws SQLException {
        CommercialItemSnapshot item = count == 0 ? CommercialItemSnapshot.empty() : new CommercialItemSnapshot(
                slot.item().itemId(),
                count,
                slot.item().components(),
                slot.item().dataVersion()
        );
        String sql = """
                UPDATE economy_inventory_slots
                   SET item_id = ?, item_count = ?, item_components = ?, item_data_version = ?,
                       updated_at = CURRENT_TIMESTAMP, version = version + 1
                 WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, item.itemId());
            statement.setInt(2, item.count());
            statement.setString(3, item.components());
            if (item.dataVersion() == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, item.dataVersion());
            }
            statement.setObject(5, slot.id());
            statement.executeUpdate();
        }
    }

    private CommercialInventorySlot readSlot(ResultSet resultSet, UUID blockId, CommercialInventoryType type) throws SQLException {
        return new CommercialInventorySlot(
                resultSet.getObject("id", UUID.class),
                blockId,
                type,
                resultSet.getInt("slot_index"),
                new CommercialItemSnapshot(resultSet.getString("item_id"), resultSet.getInt("item_count"), resultSet.getString("item_components"), (Integer) resultSet.getObject("item_data_version")),
                resultSet.getLong("version")
        );
    }
}
