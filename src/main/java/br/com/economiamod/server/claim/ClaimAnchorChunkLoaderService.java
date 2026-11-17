package br.com.economiamod.server.claim;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.server.account.BankServerIdentityService;
import br.com.economiamod.server.group.ServerActiveClockService;
import br.com.economiamod.server.persistence.EconomyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * Keeps the chunk of each paid claim anchor loaded with a chunk ticket of the mod's own.
 *
 * <p>Up to 0.5.1 this used {@code setChunkForced}, which is saved in the world. The chunks were only released in
 * {@code ServerStoppedEvent}, after the world had been saved, so after the first restart every anchor chunk was still
 * forced, the service took it for someone else's and never released it - not even after the anchor stopped being paid.
 * A ticket is never saved: {@link #refresh} adds it again after a restart and removes it as soon as the anchor is no
 * longer paid. Anchor chunks forced by older versions are released once, on the first refresh after start.</p>
 */
public final class ClaimAnchorChunkLoaderService {
    public static final ClaimAnchorChunkLoaderService INSTANCE = new ClaimAnchorChunkLoaderService();
    private static final TicketType<ChunkPos> ANCHOR_TICKET =
            TicketType.create("economia_claim_anchor", Comparator.comparingLong(ChunkPos::toLong));
    /** Ticket level 31 - 2: the chunk itself ticks, as a forced chunk did. */
    private static final int TICKET_DISTANCE = 2;

    private final Set<ForcedChunk> ownedChunks = new HashSet<>();
    private boolean legacyForcedChunksReleased;

    private ClaimAnchorChunkLoaderService() {
    }

    public void refresh(MinecraftServer server) throws SQLException {
        if (!legacyForcedChunksReleased) {
            releaseLegacyForcedChunks(server);
            legacyForcedChunksReleased = true;
        }
        Set<ForcedChunk> desired = loadActive();
        for (ForcedChunk chunk : new HashSet<>(ownedChunks)) {
            if (!desired.contains(chunk)) {
                setTicket(server, chunk, false);
                ownedChunks.remove(chunk);
            }
        }
        for (ForcedChunk chunk : desired) {
            if (!ownedChunks.contains(chunk)) {
                setTicket(server, chunk, true);
                ownedChunks.add(chunk);
            }
        }
    }

    public void stop(MinecraftServer server) {
        for (ForcedChunk chunk : ownedChunks) {
            setTicket(server, chunk, false);
        }
        ownedChunks.clear();
        legacyForcedChunksReleased = false;
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

    /** Every chunk that ever held an anchor on this server, paid or not: the chunks older versions may have forced. */
    private Set<ForcedChunk> loadAllAnchorChunks() throws SQLException {
        Set<ForcedChunk> chunks = new HashSet<>();
        try (Connection connection = EconomyDatabase.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT DISTINCT a.dimension, a.chunk_x, a.chunk_z
                       FROM economy_claim_anchors a
                      WHERE a.server_uuid = ?
                     """)) {
            statement.setObject(1, BankServerIdentityService.INSTANCE.current());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    chunks.add(new ForcedChunk(resultSet.getString("dimension"),
                            resultSet.getInt("chunk_x"), resultSet.getInt("chunk_z")));
                }
            }
        }
        return chunks;
    }

    private void releaseLegacyForcedChunks(MinecraftServer server) throws SQLException {
        int released = 0;
        for (ForcedChunk chunk : loadAllAnchorChunks()) {
            ServerLevel level = level(server, chunk);
            if (level != null && level.getForcedChunks().contains(ChunkPos.asLong(chunk.chunkX(), chunk.chunkZ()))) {
                level.setChunkForced(chunk.chunkX(), chunk.chunkZ(), false);
                released++;
            }
        }
        if (released > 0) {
            EconomiaMod.LOGGER.info("Liberados {} chunks de ancoras de claim que versoes anteriores deixaram forcados.", released);
        }
    }

    private void setTicket(MinecraftServer server, ForcedChunk chunk, boolean held) {
        ServerLevel level = level(server, chunk);
        if (level == null) {
            return;
        }
        ChunkPos pos = new ChunkPos(chunk.chunkX(), chunk.chunkZ());
        if (held) {
            level.getChunkSource().addRegionTicket(ANCHOR_TICKET, pos, TICKET_DISTANCE, pos);
        } else {
            level.getChunkSource().removeRegionTicket(ANCHOR_TICKET, pos, TICKET_DISTANCE, pos);
        }
    }

    private static ServerLevel level(MinecraftServer server, ForcedChunk chunk) {
        ResourceLocation id = ResourceLocation.tryParse(chunk.dimension());
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    private record ForcedChunk(String dimension, int chunkX, int chunkZ) {
    }
}
