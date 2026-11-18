package br.com.economiamod.client;

import br.com.economiamod.EconomiaMod;
import br.com.economiamod.client.config.EconomyClientConfig;
import br.com.economiamod.client.map.EconomyMapRenderer;
import br.com.economiamod.client.map.MinimapCorner;
import br.com.economiamod.client.map.MinimapMath;
import br.com.economiamod.client.network.ClientMapDataHandler;
import br.com.economiamod.client.screen.EconomyMapScreen;
import br.com.economiamod.common.gps.GpsPresence;
import br.com.economiamod.common.network.MapAction;
import br.com.economiamod.common.network.MapActionPayload;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * GPS minimap: the same map as the M screen, drawn in a screen corner while the GPS is in the Curios GPS slot
 * (or in a hand, if allowed in the client config). The zoom keys change and save its zoom.
 */
public final class GpsMinimapHud {
    public static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(EconomiaMod.MOD_ID, "gps_minimap");
    /** Sampled terrain is forgotten every 5 seconds, so mined or built blocks show up. */
    private static final int TERRAIN_REFRESH_TICKS = 100;

    private static final EconomyMapRenderer RENDERER = new EconomyMapRenderer();
    private static boolean visible;
    private static boolean everRefreshed;
    private static int ticksSinceRefresh;
    private static int ticksSinceTerrainClear;
    private static int lastRefreshChunkX;
    private static int lastRefreshChunkZ;
    private static String lastDimension = "";

    private GpsMinimapHud() {
    }

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, GpsMinimapHud::render);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        visible = player != null && minecraft.level != null && EconomyClientConfig.MINIMAP_ENABLED.get()
                && (GpsPresence.inCuriosSlot(player)
                || EconomyClientConfig.MINIMAP_SHOW_WHEN_HELD.get() && GpsPresence.inHand(player));
        int zoomSteps = 0;
        while (ModKeyMappings.MINIMAP_ZOOM_IN.consumeClick()) zoomSteps++;
        while (ModKeyMappings.MINIMAP_ZOOM_OUT.consumeClick()) zoomSteps--;
        if (!visible) {
            return;
        }
        if (zoomSteps != 0) {
            double zoom = MinimapMath.step(EconomyClientConfig.MINIMAP_ZOOM.get(), zoomSteps);
            EconomyClientConfig.MINIMAP_ZOOM.set(zoom);
            EconomyClientConfig.MINIMAP_ZOOM.save();
        }

        String dimension = EconomyMapRenderer.dimension(minecraft);
        boolean sameDimension = dimension.equals(lastDimension);
        if (!sameDimension || ++ticksSinceTerrainClear >= TERRAIN_REFRESH_TICKS) {
            RENDERER.clearTerrain();
            ticksSinceTerrainClear = 0;
        }
        ticksSinceRefresh++;
        int chunkX = player.chunkPosition().x;
        int chunkZ = player.chunkPosition().z;
        if (MinimapMath.shouldRefresh(everRefreshed, sameDimension, ticksSinceRefresh,
                lastRefreshChunkX, lastRefreshChunkZ, chunkX, chunkZ)) {
            // Same request as the M map: claims around the player and the player's saved places.
            PacketDistributor.sendToServer(new MapActionPayload(MapAction.REFRESH, "", "", chunkX, 0, chunkZ,
                    new UUID(0L, 0L)));
            everRefreshed = true;
            ticksSinceRefresh = 0;
            lastRefreshChunkX = chunkX;
            lastRefreshChunkZ = chunkZ;
        }
        lastDimension = dimension;
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        visible = false;
        everRefreshed = false;
        lastDimension = "";
        RENDERER.clearTerrain();
        ClientMapDataHandler.clear();
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!visible || player == null || minecraft.level == null || minecraft.options.hideGui
                || minecraft.getDebugOverlay().showDebugScreen() || minecraft.screen instanceof EconomyMapScreen) {
            return;
        }
        int offsetX = EconomyClientConfig.MINIMAP_OFFSET_X.get();
        int offsetY = EconomyClientConfig.MINIMAP_OFFSET_Y.get();
        MinimapCorner corner = EconomyClientConfig.MINIMAP_CORNER.get();
        int size = MinimapMath.fittedSize(EconomyClientConfig.MINIMAP_SIZE.get(),
                graphics.guiWidth(), graphics.guiHeight(), offsetX, offsetY);
        boolean effectsShown = player.getActiveEffects().stream().anyMatch(effect -> effect.showIcon());
        int[] position = MinimapMath.position(corner, graphics.guiWidth(), graphics.guiHeight(), size,
                offsetX, offsetY, effectsShown);
        int x = position[0];
        int y = position[1];

        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xFF0B1A22);
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF4F6B78);
        graphics.fill(x, y, x + size, y + size, 0xFF101417);

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        double centerX = Mth.lerp(partialTick, player.xo, player.getX());
        double centerZ = Mth.lerp(partialTick, player.zo, player.getZ());
        EconomyMapRenderer.Viewport view = new EconomyMapRenderer.Viewport(x, y, x + size, y + size,
                centerX, centerZ, EconomyClientConfig.MINIMAP_ZOOM.get());
        RENDERER.drawBase(graphics, minecraft, view, ClientMapDataHandler.latest());
        EconomyMapRenderer.drawPlayerMarker(graphics, view.centerScreenX(), view.centerScreenY(),
                player.getViewYRot(partialTick));

        // North is always up, like the M map.
        graphics.drawCenteredString(minecraft.font, "N", x + size / 2, y + 2, 0xFFE4ECEF);
        String coordinates = "X " + Mth.floor(player.getX()) + "  Z " + Mth.floor(player.getZ());
        graphics.drawCenteredString(minecraft.font, coordinates, x + size / 2, y + size + 3, 0xFFE4ECEF);
    }
}
