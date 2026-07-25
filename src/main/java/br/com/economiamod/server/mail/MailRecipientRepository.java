package br.com.economiamod.server.mail;

import br.com.economiamod.common.block.CommercialBlockType;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MailRecipientRepository {
    public List<MailRecipientRecord> list(UUID originBlockId) throws SQLException {
        String sql = """
                SELECT destination.id AS destination_block_id,
                       destination.owner_name,
                       destination.custom_name AS mail_name,
                       destination.dimension,
                       destination.block_x,
                       destination.block_y,
                       destination.block_z
                  FROM economy_mail_recipients recipient
                  JOIN economy_commercial_blocks destination
                    ON destination.id = recipient.destination_block_id
                 WHERE recipient.origin_block_id = ?
                   AND destination.block_type = ?
                   AND destination.status = 'ACTIVE'
                 ORDER BY destination.owner_name, destination.custom_name, destination.block_x, destination.block_z
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, originBlockId);
            statement.setString(2, CommercialBlockType.MAIL.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MailRecipientRecord> recipients = new ArrayList<>();
                while (resultSet.next()) {
                    recipients.add(read(resultSet));
                }
                return List.copyOf(recipients);
            }
        }
    }

    public void add(UUID originBlockId, UUID destinationBlockId, UUID playerUuid) throws SQLException {
        String sql = """
                INSERT INTO economy_mail_recipients(id, origin_block_id, destination_block_id, added_by_player_uuid, created_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (origin_block_id, destination_block_id) DO NOTHING
                """;
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, originBlockId);
            statement.setObject(3, destinationBlockId);
            statement.setObject(4, playerUuid);
            statement.executeUpdate();
        }
    }

    public void delete(UUID originBlockId, UUID destinationBlockId) throws SQLException {
        String sql = "DELETE FROM economy_mail_recipients WHERE origin_block_id = ? AND destination_block_id = ?";
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, originBlockId);
            statement.setObject(2, destinationBlockId);
            statement.executeUpdate();
        }
    }

    private MailRecipientRecord read(ResultSet resultSet) throws SQLException {
        return new MailRecipientRecord(
                resultSet.getObject("destination_block_id", UUID.class),
                resultSet.getString("owner_name"),
                resultSet.getString("mail_name"),
                resultSet.getString("dimension"),
                resultSet.getInt("block_x"),
                resultSet.getInt("block_y"),
                resultSet.getInt("block_z")
        );
    }
}
