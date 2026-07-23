package br.com.economiamod.server.commercial.inventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CommercialInventoryMutationRepository {
    private static final int MAX_STACK_SIZE = 64;

    private final CommercialItemMatcher matcher = new CommercialItemMatcher();

    public Optional<CommercialInventorySlot> lockSlot(Connection connection, UUID blockId, CommercialInventoryType type, int slotIndex) throws SQLException {
        String sql = """
                SELECT id, slot_index, item_id, item_count, item_components, item_data_version, version
                  FROM economy_inventory_slots
                 WHERE commercial_block_id = ?
                   AND inventory_type = ?
                   AND slot_index = ?
                 FOR UPDATE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, blockId);
            statement.setString(2, type.name());
            statement.setInt(3, slotIndex);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(readSlot(resultSet, blockId, type)) : Optional.empty();
            }
        }
    }

    public boolean removeFromSlot(Connection connection, CommercialInventorySlot slot, int count) throws SQLException {
        if (slot.item().isEmpty() || slot.item().count() < count) {
            return false;
        }
        int remaining = slot.item().count() - count;
        CommercialItemSnapshot next = remaining == 0 ? CommercialItemSnapshot.empty() : new CommercialItemSnapshot(
                slot.item().itemId(),
                remaining,
                slot.item().components(),
                slot.item().dataVersion()
        );
        updateSlot(connection, slot.id(), next);
        return true;
    }

    public boolean addToSlots(Connection connection, UUID blockId, CommercialInventoryType type, CommercialItemSnapshot item, int count) throws SQLException {
        List<CommercialInventorySlot> slots = lockSlots(connection, blockId, type);
        if (!canAdd(slots, item, count)) {
            return false;
        }
        int remaining = count;
        for (CommercialInventorySlot slot : slots) {
            if (!slot.item().isEmpty() && matcher.stackable(slot.item(), item)) {
                remaining = fillSlot(connection, slot, item, remaining);
            }
        }
        for (CommercialInventorySlot slot : slots) {
            if (slot.item().isEmpty()) {
                remaining = fillSlot(connection, slot, item, remaining);
            }
            if (remaining == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean canAddToSlots(Connection connection, UUID blockId, CommercialInventoryType type, CommercialItemSnapshot item, int count) throws SQLException {
        return canAdd(lockSlots(connection, blockId, type), item, count);
    }

    private boolean canAdd(List<CommercialInventorySlot> slots, CommercialItemSnapshot item, int count) {
        int remaining = count;
        for (CommercialInventorySlot slot : slots) {
            if (slot.item().isEmpty()) {
                remaining -= MAX_STACK_SIZE;
            } else if (matcher.stackable(slot.item(), item)) {
                remaining -= Math.max(0, MAX_STACK_SIZE - slot.item().count());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private int fillSlot(Connection connection, CommercialInventorySlot slot, CommercialItemSnapshot item, int remaining) throws SQLException {
        int current = slot.item().isEmpty() ? 0 : slot.item().count();
        int accepted = Math.min(remaining, MAX_STACK_SIZE - current);
        if (accepted <= 0) {
            return remaining;
        }
        updateSlot(connection, slot.id(), new CommercialItemSnapshot(item.itemId(), current + accepted, item.components(), item.dataVersion()));
        return remaining - accepted;
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

    private void updateSlot(Connection connection, UUID slotId, CommercialItemSnapshot item) throws SQLException {
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
                statement.setNull(4, Types.INTEGER);
            } else {
                statement.setInt(4, item.dataVersion());
            }
            statement.setObject(5, slotId);
            statement.executeUpdate();
        }
    }

    private CommercialInventorySlot readSlot(ResultSet resultSet, UUID blockId, CommercialInventoryType type) throws SQLException {
        CommercialItemSnapshot item = new CommercialItemSnapshot(
                resultSet.getString("item_id"),
                resultSet.getInt("item_count"),
                resultSet.getString("item_components"),
                (Integer) resultSet.getObject("item_data_version")
        );
        return new CommercialInventorySlot(
                resultSet.getObject("id", UUID.class),
                blockId,
                type,
                resultSet.getInt("slot_index"),
                item,
                resultSet.getLong("version")
        );
    }
}
