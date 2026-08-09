package br.com.economiamod.server.location;

import br.com.economiamod.server.config.EconomyServerConfig;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public final class PlayerLocationRepository {
    public PlayerLocation save(UUID playerUuid, String requestedName, String dimension, int x, int y, int z) throws SQLException {
        String name = validateName(requestedName);
        PlayerLocation location = new PlayerLocation(UUID.randomUUID(), playerUuid, name, dimension, x, y, z);
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO economy_player_locations(
                         id, player_uuid, name, dimension, block_x, block_y, block_z, created_at, updated_at
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     """)) {
            statement.setObject(1, location.id());
            statement.setObject(2, location.playerUuid());
            statement.setString(3, location.name());
            statement.setString(4, location.dimension());
            statement.setInt(5, location.x());
            statement.setInt(6, location.y());
            statement.setInt(7, location.z());
            statement.executeUpdate();
        }
        return location;
    }

    public List<PlayerLocation> list(UUID playerUuid) throws SQLException {
        List<PlayerLocation> locations = new ArrayList<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, player_uuid, name, dimension, block_x, block_y, block_z
                       FROM economy_player_locations WHERE player_uuid = ? ORDER BY created_at, name
                     """)) {
            statement.setObject(1, playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    locations.add(new PlayerLocation(
                            resultSet.getObject("id", UUID.class), resultSet.getObject("player_uuid", UUID.class),
                            resultSet.getString("name"), resultSet.getString("dimension"), resultSet.getInt("block_x"),
                            resultSet.getInt("block_y"), resultSet.getInt("block_z")));
                }
            }
        }
        return locations;
    }

    public boolean update(UUID playerUuid, UUID locationId, String requestedName, String dimension,
                          int x, int y, int z) throws SQLException {
        String name = validateName(requestedName);
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE economy_player_locations
                        SET name = ?, dimension = ?, block_x = ?, block_y = ?, block_z = ?, updated_at = CURRENT_TIMESTAMP
                      WHERE id = ? AND player_uuid = ?
                     """)) {
            statement.setString(1, name);
            statement.setString(2, dimension);
            statement.setInt(3, x);
            statement.setInt(4, y);
            statement.setInt(5, z);
            statement.setObject(6, locationId);
            statement.setObject(7, playerUuid);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(UUID playerUuid, UUID locationId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM economy_player_locations WHERE id = ? AND player_uuid = ?")) {
            statement.setObject(1, locationId);
            statement.setObject(2, playerUuid);
            return statement.executeUpdate() == 1;
        }
    }

    public Optional<PlayerLocation> find(UUID locationId) throws SQLException {
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, player_uuid, name, dimension, block_x, block_y, block_z
                       FROM economy_player_locations WHERE id = ?
                     """)) {
            statement.setObject(1, locationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(new PlayerLocation(
                        resultSet.getObject("id", UUID.class), resultSet.getObject("player_uuid", UUID.class),
                        resultSet.getString("name"), resultSet.getString("dimension"), resultSet.getInt("block_x"),
                        resultSet.getInt("block_y"), resultSet.getInt("block_z"))) : Optional.empty();
            }
        }
    }

    private String validateName(String requestedName) {
        String name = requestedName == null ? "" : requestedName.strip();
        if (name.isEmpty() || name.codePointCount(0, name.length()) > EconomyServerConfig.LOCATION_NAME_MAX_LENGTH.get()) {
            throw new IllegalArgumentException("invalid location name");
        }
        return name;
    }
}
