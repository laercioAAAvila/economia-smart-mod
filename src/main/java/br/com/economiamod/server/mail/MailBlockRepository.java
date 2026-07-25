package br.com.economiamod.server.mail;

import br.com.economiamod.common.block.CommercialBlockType;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MailBlockRepository {
    public Optional<MailBlockRecord> findActive(UUID blockId) throws SQLException {
        String sql = """
                SELECT id, owner_player_uuid, linked_account_id, owner_name, owner_account_number,
                       custom_name, dimension, block_x, block_y, block_z
                  FROM economy_commercial_blocks
                 WHERE id = ?
                   AND block_type = ?
                   AND status = 'ACTIVE'
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, blockId);
            statement.setString(2, CommercialBlockType.MAIL.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(read(resultSet)) : Optional.empty();
            }
        }
    }

    public void rename(UUID blockId, String name) throws SQLException {
        String sql = """
                UPDATE economy_commercial_blocks
                   SET custom_name = ?, updated_at = CURRENT_TIMESTAMP
                 WHERE id = ?
                   AND block_type = ?
                   AND status = 'ACTIVE'
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sanitize(name));
            statement.setObject(2, blockId);
            statement.setString(3, CommercialBlockType.MAIL.name());
            statement.executeUpdate();
        }
    }

    public List<MailBlockRecord> findByNameInDimension(String name, String dimension) throws SQLException {
        String sql = """
                SELECT id, owner_player_uuid, linked_account_id, owner_name, owner_account_number,
                       custom_name, dimension, block_x, block_y, block_z
                  FROM economy_commercial_blocks
                 WHERE block_type = ?
                   AND status = 'ACTIVE'
                   AND dimension = ?
                   AND LOWER(custom_name) = LOWER(?)
                 ORDER BY updated_at DESC
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, CommercialBlockType.MAIL.name());
            statement.setString(2, dimension);
            statement.setString(3, sanitize(name));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MailBlockRecord> records = new ArrayList<>();
                while (resultSet.next()) {
                    records.add(read(resultSet));
                }
                return List.copyOf(records);
            }
        }
    }

    private MailBlockRecord read(ResultSet resultSet) throws SQLException {
        return new MailBlockRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("owner_player_uuid", UUID.class),
                resultSet.getObject("linked_account_id", UUID.class),
                resultSet.getString("owner_name"),
                resultSet.getString("owner_account_number"),
                resultSet.getString("custom_name"),
                resultSet.getString("dimension"),
                resultSet.getInt("block_x"),
                resultSet.getInt("block_y"),
                resultSet.getInt("block_z")
        );
    }

    private String sanitize(String name) {
        String value = name == null ? "" : name.trim();
        return value.length() > 64 ? value.substring(0, 64) : value;
    }
}
