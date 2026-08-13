package br.com.economiamod.server.claim;

import br.com.economiamod.server.account.BankServerIdentityService;
import br.com.economiamod.server.group.ServerActiveClockService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class ClaimAnchorChunkLoaderService {
    public static final ClaimAnchorChunkLoaderService INSTANCE = new ClaimAnchorChunkLoaderService();
    private final Set<ForcedChunk> ownedForcedChunks = new HashSet<>();

    private ClaimAnchorChunkLoaderService() {
    }

    public void refresh(MinecraftServer server) throws SQLException {
        Set<ForcedChunk> desired = loadActive();
        for (ForcedChunk chunk : new HashSet<>(ownedForcedChunks)) {
            if (!desired.contains(chunk)) {
                setForced(server, chunk, false);
                ownedForcedChunks.remove(chunk);
            }
        }
        for (ForcedChunk chunk : desired) {
            if (!ownedForcedChunks.contains(chunk) && !alreadyForced(server, chunk)) {
                setForced(server, chunk, true);
                ownedForcedChunks.add(chunk);
            }
        }
    }

    public void stop(MinecraftServer server) {
        for (ForcedChunk chunk : ownedForcedChunks) {
            setForced(server, chunk, false);
        }
        ownedForcedChunks.clear();
    }

    private Set<ForcedChunk> loadActive() throws SQLException {
        Set<ForcedChunk> chunks = new HashSet<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT a.dimension, a.chunk_x, a.chunk_z
                       FROM economy_claim_anchors a
                       JOIN economy_claim_territories t ON t.id = a.territory_id
                      WHERE a.server_uuid = ? AND a.active = TRUE AND a.removed_at IS NULL
                        AND t.anchor_paid_until_millis > ?
                     """)) {
            statement.setObject(1, BankServerIdentityService.INSTANCE.current());
            statement.setLong(2, ServerActiveClockService.INSTANCE.currentMillis());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    chunks.add(new ForcedChunk(resultSet.getString("dimension"),
                            resultSet.getInt("chunk_x"), resultSet.getInt("chunk_z")));
                }
            }
        }
        return chunks;
    }

    private void setForced(MinecraftServer server, ForcedChunk chunk, boolean value) {
        ResourceKey<net.minecraft.world.level.Level> key = ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(chunk.dimension()));
        ServerLevel level = server.getLevel(key);
        if (level != null) {
            level.setChunkForced(chunk.chunkX(), chunk.chunkZ(), value);
        }
    }

    private boolean alreadyForced(MinecraftServer server, ForcedChunk chunk) {
        ResourceKey<net.minecraft.world.level.Level> key = ResourceKey.create(
                Registries.DIMENSION, ResourceLocation.parse(chunk.dimension()));
        ServerLevel level = server.getLevel(key);
        return level != null && level.getForcedChunks().contains(
                net.minecraft.world.level.ChunkPos.asLong(chunk.chunkX(), chunk.chunkZ()));
    }

    private record ForcedChunk(String dimension, int chunkX, int chunkZ) {
    }
}
