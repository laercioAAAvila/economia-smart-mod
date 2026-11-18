package br.com.economiamod.client.map;

import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.network.MapDataPayload;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Draws the Economia map (loaded terrain, chunk grid, claims, saved places and the player arrow) inside any
 * rectangle, so the full map screen (M) and the GPS minimap show exactly the same map.
 */
public final class EconomyMapRenderer {
    private static final int TERRAIN_CACHE_LIMIT = 131_072;
    private static final int TERRAIN_SAMPLES_PER_FRAME = 2_048;
    /** Terrain is only drawn for chunks this close to the player, as the client only keeps those loaded. */
    private static final int TERRAIN_CHUNK_RADIUS = 16;

    private final Map<Long, Integer> terrainColorCache = new HashMap<>();
    private int terrainSamplesThisFrame;

    /** A map rectangle on screen, centered on world (centerX, centerZ), with {@code scale} pixels per block. */
    public record Viewport(int left, int top, int right, int bottom, double centerX, double centerZ, double scale) {
        public int centerScreenX() {
            return left + (right - left) / 2;
        }

        public int centerScreenY() {
            return top + (bottom - top) / 2;
        }

        public int toScreenX(double worldX) {
            return (int) Math.round(centerScreenX() + (worldX - centerX) * scale);
        }

        public int toScreenY(double worldZ) {
            return (int) Math.round(centerScreenY() + (worldZ - centerZ) * scale);
        }

        public int toWorldX(double screenX) {
            return (int) Math.floor(centerX + (screenX - centerScreenX()) / scale);
        }

        public int toWorldZ(double screenY) {
            return (int) Math.floor(centerZ + (screenY - centerScreenY()) / scale);
        }

        public boolean contains(int x, int y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    /** Forgets sampled terrain colors, so blocks changed since then are drawn again. */
    public void clearTerrain() {
        terrainColorCache.clear();
    }

    /** Terrain, chunk grid, claims and saved places of the current dimension, clipped to the viewport. */
    public void drawBase(GuiGraphics graphics, Minecraft minecraft, Viewport view, MapDataPayload data) {
        int firstChunkX = Math.floorDiv(view.toWorldX(view.left()), 16) - 1;
        int lastChunkX = Math.floorDiv(view.toWorldX(view.right()), 16) + 1;
        int firstChunkZ = Math.floorDiv(view.toWorldZ(view.top()), 16) - 1;
        int lastChunkZ = Math.floorDiv(view.toWorldZ(view.bottom()), 16) + 1;
        graphics.enableScissor(view.left(), view.top(), view.right(), view.bottom());
        drawLoadedTerrain(graphics, minecraft, view, firstChunkX, lastChunkX, firstChunkZ, lastChunkZ);

        for (int chunkX = firstChunkX; chunkX <= lastChunkX; chunkX++) {
            int x = view.toScreenX(chunkX * 16);
            if (x >= view.left() && x < view.right()) {
                graphics.fill(x, view.top(), x + 1, view.bottom(), 0x443A4B53);
            }
        }
        for (int chunkZ = firstChunkZ; chunkZ <= lastChunkZ; chunkZ++) {
            int y = view.toScreenY(chunkZ * 16);
            if (y >= view.top() && y < view.bottom()) {
                graphics.fill(view.left(), y, view.right(), y + 1, 0x443A4B53);
            }
        }

        String dimension = dimension(minecraft);
        for (MapDataPayload.ClaimSummary claim : data.claims()) {
            if (!dimension.equals(claim.dimension())) {
                continue;
            }
            int x1 = view.toScreenX(claim.chunkX() * 16);
            int y1 = view.toScreenY(claim.chunkZ() * 16);
            int x2 = view.toScreenX((claim.chunkX() + 1) * 16);
            int y2 = view.toScreenY((claim.chunkZ() + 1) * 16);
            int claimLeft = Math.max(view.left(), Math.min(x1, x2) + 1);
            int claimTop = Math.max(view.top(), Math.min(y1, y2) + 1);
            int claimRight = Math.min(view.right(), Math.max(x1, x2));
            int claimBottom = Math.min(view.bottom(), Math.max(y1, y2));
            if (claimRight > claimLeft && claimBottom > claimTop) {
                graphics.fill(claimLeft, claimTop, claimRight, claimBottom,
                        claim.type() == GroupType.CLAN ? 0x6650C878 : 0x66D8893B);
            }
        }

        for (MapDataPayload.LocationSummary location : data.locations()) {
            if (!dimension.equals(location.dimension())) {
                continue;
            }
            int x = view.toScreenX(location.x());
            int y = view.toScreenY(location.z());
            if (view.contains(x, y)) {
                graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFFFFD54F);
            }
        }
        graphics.disableScissor();
    }

    /** The player arrow, when the player is inside the viewport. */
    public void drawPlayer(GuiGraphics graphics, Minecraft minecraft, Viewport view) {
        if (minecraft.player == null) {
            return;
        }
        int x = view.toScreenX(minecraft.player.getX());
        int y = view.toScreenY(minecraft.player.getZ());
        if (view.contains(x, y)) {
            drawPlayerMarker(graphics, x, y, minecraft.player.getYRot());
        }
    }

    public static String dimension(Minecraft minecraft) {
        return minecraft.level != null ? minecraft.level.dimension().location().toString() : "";
    }

    /** The "0>" arrow of {@link PlayerMarkerShape}, turned toward where the player looks. */
    public static void drawPlayerMarker(GuiGraphics graphics, int x, int y, float yawDegrees) {
        graphics.pose().pushPose();
        // Turn around the center of the player's pixel, then draw pixels centered on (0, 0).
        graphics.pose().translate(x + 0.5F, y + 0.5F, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(PlayerMarkerShape.rotationDegrees(yawDegrees)));
        graphics.pose().translate(-0.5F, -0.5F, 0.0F);
        for (int[] pixel : PlayerMarkerShape.OUTLINE) {
            graphics.fill(pixel[0], pixel[1], pixel[0] + 1, pixel[1] + 1, 0xFF0B1A22);
        }
        for (int[] pixel : PlayerMarkerShape.FILL) {
            graphics.fill(pixel[0], pixel[1], pixel[0] + 1, pixel[1] + 1, 0xFF4FC3F7);
        }
        graphics.fill(0, 0, 1, 1, 0xFFFFFFFF); // light center of the "0"
        graphics.pose().popPose();
    }

    private void drawLoadedTerrain(GuiGraphics graphics, Minecraft minecraft, Viewport view,
                                   int firstChunkX, int lastChunkX, int firstChunkZ, int lastChunkZ) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        int playerChunkX = minecraft.player.chunkPosition().x;
        int playerChunkZ = minecraft.player.chunkPosition().z;
        int minChunkX = Math.max(firstChunkX, playerChunkX - TERRAIN_CHUNK_RADIUS);
        int maxChunkX = Math.min(lastChunkX, playerChunkX + TERRAIN_CHUNK_RADIUS);
        int minChunkZ = Math.max(firstChunkZ, playerChunkZ - TERRAIN_CHUNK_RADIUS);
        int maxChunkZ = Math.min(lastChunkZ, playerChunkZ + TERRAIN_CHUNK_RADIUS);
        int chunkPixels = Math.max(1, (int) Math.round(16.0D * view.scale()));
        int samples = chunkPixels >= 48 ? 8 : chunkPixels >= 12 ? 4 : 2;
        terrainSamplesThisFrame = 0;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!minecraft.level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                int chunkLeft = view.toScreenX(chunkX * 16);
                int chunkTop = view.toScreenY(chunkZ * 16);
                int chunkRight = view.toScreenX((chunkX + 1) * 16);
                int chunkBottom = view.toScreenY((chunkZ + 1) * 16);
                for (int sampleX = 0; sampleX < samples; sampleX++) {
                    for (int sampleZ = 0; sampleZ < samples; sampleZ++) {
                        int blockX = chunkX * 16 + (sampleX * 16 + 8) / samples;
                        int blockZ = chunkZ * 16 + (sampleZ * 16 + 8) / samples;
                        int color = terrainColor(minecraft, blockX, blockZ);
                        if (color == 0) {
                            continue;
                        }
                        int cellLeft = chunkLeft + (chunkRight - chunkLeft) * sampleX / samples;
                        int cellTop = chunkTop + (chunkBottom - chunkTop) * sampleZ / samples;
                        int cellRight = chunkLeft + (chunkRight - chunkLeft) * (sampleX + 1) / samples;
                        int cellBottom = chunkTop + (chunkBottom - chunkTop) * (sampleZ + 1) / samples;
                        graphics.fill(cellLeft, cellTop, Math.max(cellLeft + 1, cellRight),
                                Math.max(cellTop + 1, cellBottom), 0xFF000000 | color);
                    }
                }
            }
        }
    }

    private int terrainColor(Minecraft minecraft, int blockX, int blockZ) {
        long key = BlockPos.asLong(blockX, 0, blockZ);
        Integer cached = terrainColorCache.get(key);
        if (cached != null) {
            return cached;
        }
        if (terrainColorCache.size() >= TERRAIN_CACHE_LIMIT) {
            terrainColorCache.clear();
        }
        if (terrainSamplesThisFrame >= TERRAIN_SAMPLES_PER_FRAME) {
            return 0;
        }
        terrainSamplesThisFrame++;
        int blockY = minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ) - 1;
        blockY = Math.max(minecraft.level.getMinBuildHeight(), blockY);
        BlockPos samplePos = new BlockPos(blockX, blockY, blockZ);
        var state = minecraft.level.getBlockState(samplePos);
        if (state.isAir()) {
            terrainColorCache.put(key, 0);
            return 0;
        }
        int color = state.getMapColor(minecraft.level, samplePos).col;
        int tint = minecraft.getBlockColors().getColor(state, minecraft.level, samplePos, 0);
        if (tint != -1) {
            color = tint;
        }
        terrainColorCache.put(key, color);
        return color;
    }
}
