package br.com.economiamod.client.screen;

import br.com.economiamod.client.ModKeyMappings;
import br.com.economiamod.client.network.ClientMapDataHandler;
import br.com.economiamod.common.group.GroupType;
import br.com.economiamod.common.group.ChatChannel;
import br.com.economiamod.common.network.ChatChannelPayload;
import br.com.economiamod.common.network.MapAction;
import br.com.economiamod.common.network.MapActionPayload;
import br.com.economiamod.common.network.MapDataPayload;
import br.com.economiamod.common.network.OpenSharedLocationPayload;
import br.com.economiamod.common.network.OpenClaimChunkMapPayload;
import br.com.economiamod.common.network.ShareLocationPayload;
import br.com.economiamod.server.claim.ClaimPriceService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EconomyMapScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 176;
    private static final int TERRAIN_CACHE_LIMIT = 131_072;
    private static final int TERRAIN_SAMPLES_PER_FRAME = 2_048;
    private MapDataPayload data = ClientMapDataHandler.empty();
    private ChatChannel selectedChannel = data.selectedChannel();
    private double centerX;
    private double centerZ;
    private double zoom = 1.0D;
    private boolean initializedCenter;
    private boolean showLocations;
    private int locationListOffset;
    private boolean locationModal;
    private int cursorWorldX;
    private int cursorWorldZ;
    private int lastMouseX;
    private int lastMouseY;
    private EditBox locationName;
    private EditBox locationX;
    private EditBox locationY;
    private EditBox locationZ;
    private Button saveLocation;
    private Button cancelLocation;
    private Button locationsButton;
    private final List<AbstractWidget> locationWidgets = new ArrayList<>();
    private final OpenSharedLocationPayload sharedLocation;
    private final OpenClaimChunkMapPayload claimPurchase;
    private final ClaimPriceService claimPrices = new ClaimPriceService();
    private MapDataPayload.LocationSummary pendingShare;
    private boolean shareModal;
    private boolean requestedData;
    private boolean mapDragged;
    private MapDataPayload.LocationSummary editingLocation;
    private boolean sharedResolved;
    private boolean purchaseConfirmation;
    private int selectedPurchaseChunkX;
    private int selectedPurchaseChunkZ;
    private long selectedPurchasePrice;
    private Button confirmPurchaseButton;
    private Button cancelPurchaseButton;
    private final Map<Long, Integer> terrainColorCache = new HashMap<>();
    private int terrainSamplesThisFrame;

    public EconomyMapScreen() {
        this(null, null);
    }

    public EconomyMapScreen(OpenSharedLocationPayload sharedLocation) {
        this(sharedLocation, null);
    }

    public EconomyMapScreen(OpenClaimChunkMapPayload claimPurchase) {
        this(null, claimPurchase);
    }

    private EconomyMapScreen(OpenSharedLocationPayload sharedLocation, OpenClaimChunkMapPayload claimPurchase) {
        super(Component.translatable("screen.economia.map.title"));
        this.sharedLocation = sharedLocation;
        this.claimPurchase = claimPurchase;
        if (sharedLocation != null) {
            centerX = sharedLocation.x();
            centerZ = sharedLocation.z();
            initializedCenter = true;
        } else if (claimPurchase != null) {
            centerX = claimPurchase.centerBlockX();
            centerZ = claimPurchase.centerBlockZ();
            initializedCenter = true;
        }
    }

    @Override
    protected void init() {
        if (!initializedCenter && minecraft != null && minecraft.player != null) {
            centerX = minecraft.player.getX();
            centerZ = minecraft.player.getZ();
            initializedCenter = true;
        }
        if (claimPurchase != null) {
            addRenderableWidget(Button.builder(Component.translatable("screen.economia.common.cancel"),
                    button -> onClose()).bounds(12, height - 32, SIDEBAR_WIDTH - 24, 20).build());
            if (!requestedData) {
                requestedData = true;
                refreshVisibleClaims();
            }
            return;
        }
        locationsButton = addRenderableWidget(Button.builder(Component.translatable("screen.economia.map.locations"),
                button -> {
                    showLocations = !showLocations;
                    if (!showLocations) locationListOffset = 0;
                    rebuildLocationWidgets();
                }).bounds(12, 42, SIDEBAR_WIDTH - 24, 20).build());
        locationsButton.active = !data.locations().isEmpty();
        addChannelButton(ChatChannel.GENERAL, 12);
        addChannelButton(ChatChannel.CLAN, 64);
        addChannelButton(ChatChannel.PRIVATE_PROPERTY, 116);
        addRenderableWidget(Button.builder(Component.translatable("screen.economia.common.exit"), button -> onClose())
                .bounds(12, height - 32, SIDEBAR_WIDTH - 24, 20).build());
        addSharedLocationActions();
        rebuildLocationWidgets();
        if (!requestedData) {
            requestedData = true;
            refreshVisibleClaims();
        }
    }

    public void applyMapData(MapDataPayload payload) {
        data = payload;
        selectedChannel = payload.selectedChannel();
        if (locationsButton != null) {
            locationsButton.active = !payload.locations().isEmpty();
            if (!locationModal && !shareModal) rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        graphics.fill(0, 0, width, height, 0xFF101417);
        graphics.fill(0, 0, SIDEBAR_WIDTH, height, 0xFF1B252B);
        graphics.fill(8, 8, SIDEBAR_WIDTH - 8, 34, 0xFF2F4B5B);
        graphics.drawCenteredString(font, title, SIDEBAR_WIDTH / 2, 17, 0xFFFFFFFF);
        drawMap(graphics);
        if (claimPurchase != null) drawClaimPurchasePanel(graphics);
        if (purchaseConfirmation) drawPurchaseConfirmation(graphics);
        if (showLocations) drawLocations(graphics);
        if (sharedLocation != null && !sharedResolved) drawSharedLocation(graphics);
        if (locationModal) {
            drawLocationModal(graphics);
        }
        if (shareModal) drawShareModal(graphics);
        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawMap(GuiGraphics graphics) {
        int left = SIDEBAR_WIDTH;
        int mapWidth = width - left;
        int centerScreenX = left + mapWidth / 2;
        int centerScreenY = height / 2;
        double pixelsPerBlock = pixelsPerBlock();
        cursorWorldX = screenToWorldX(lastMouseX, centerScreenX, pixelsPerBlock);
        cursorWorldZ = screenToWorldZ(lastMouseY, centerScreenY, pixelsPerBlock);
        int firstChunkX = (int) Math.floor(centerX / 16.0D) - mapWidth / Math.max(1, (int) (16 * pixelsPerBlock)) - 2;
        int lastChunkX = (int) Math.floor(centerX / 16.0D) + mapWidth / Math.max(1, (int) (16 * pixelsPerBlock)) + 2;
        int firstChunkZ = (int) Math.floor(centerZ / 16.0D) - height / Math.max(1, (int) (16 * pixelsPerBlock)) - 2;
        int lastChunkZ = (int) Math.floor(centerZ / 16.0D) + height / Math.max(1, (int) (16 * pixelsPerBlock)) + 2;

        drawLoadedTerrain(graphics, left, centerScreenX, centerScreenY, pixelsPerBlock,
                firstChunkX, lastChunkX, firstChunkZ, lastChunkZ);

        for (int chunkX = firstChunkX; chunkX <= lastChunkX; chunkX++) {
            int x = worldToScreenX(chunkX * 16, centerScreenX, pixelsPerBlock);
            if (x >= left && x < width) {
                graphics.fill(x, 0, x + 1, height, 0x443A4B53);
            }
        }
        for (int chunkZ = firstChunkZ; chunkZ <= lastChunkZ; chunkZ++) {
            int y = worldToScreenY(chunkZ * 16, centerScreenY, pixelsPerBlock);
            graphics.fill(left, y, width, y + 1, 0x443A4B53);
        }

        String dimension = minecraft != null && minecraft.level != null
                ? minecraft.level.dimension().location().toString() : "";
        for (MapDataPayload.ClaimSummary claim : data.claims()) {
            if (!dimension.equals(claim.dimension())) {
                continue;
            }
            int x1 = worldToScreenX(claim.chunkX() * 16, centerScreenX, pixelsPerBlock);
            int y1 = worldToScreenY(claim.chunkZ() * 16, centerScreenY, pixelsPerBlock);
            int x2 = worldToScreenX((claim.chunkX() + 1) * 16, centerScreenX, pixelsPerBlock);
            int y2 = worldToScreenY((claim.chunkZ() + 1) * 16, centerScreenY, pixelsPerBlock);
            int claimLeft = Math.max(left, Math.min(x1, x2) + 1);
            int claimTop = Math.max(0, Math.min(y1, y2) + 1);
            int claimRight = Math.min(width, Math.max(x1, x2));
            int claimBottom = Math.min(height, Math.max(y1, y2));
            if (claimRight > claimLeft && claimBottom > claimTop) {
                graphics.fill(claimLeft, claimTop, claimRight, claimBottom,
                        claim.type() == GroupType.CLAN ? 0x6650C878 : 0x66D8893B);
            }
        }

        if (claimPurchase != null && lastMouseX >= left && lastMouseX < width
                && lastMouseY >= 0 && lastMouseY < height) {
            int selectedChunkX = cursorWorldX >> 4;
            int selectedChunkZ = cursorWorldZ >> 4;
            drawChunkSelection(graphics, left, centerScreenX, centerScreenY, pixelsPerBlock,
                    selectedChunkX, selectedChunkZ);
        }

        for (MapDataPayload.LocationSummary location : data.locations()) {
            if (!dimension.equals(location.dimension())) {
                continue;
            }
            int x = worldToScreenX(location.x(), centerScreenX, pixelsPerBlock);
            int y = worldToScreenY(location.z(), centerScreenY, pixelsPerBlock);
            if (x >= left && x < width && y >= 0 && y < height) {
                graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFFFFD54F);
            }
        }
        if (sharedLocation != null && !sharedResolved && dimension.equals(sharedLocation.dimension())) {
            int sharedX = worldToScreenX(sharedLocation.x(), centerScreenX, pixelsPerBlock);
            int sharedZ = worldToScreenY(sharedLocation.z(), centerScreenY, pixelsPerBlock);
            if (sharedX >= left && sharedX < width && sharedZ >= 0 && sharedZ < height) {
                graphics.fill(sharedX - 4, sharedZ - 4, sharedX + 5, sharedZ + 5, 0xFFE040FB);
            }
        }

        if (minecraft != null && minecraft.player != null) {
            int playerX = worldToScreenX(minecraft.player.getX(), centerScreenX, pixelsPerBlock);
            int playerY = worldToScreenY(minecraft.player.getZ(), centerScreenY, pixelsPerBlock);
            if (playerX >= left && playerX < width && playerY >= 0 && playerY < height) {
                graphics.fill(playerX - 3, playerY - 3, playerX + 4, playerY + 4, 0xFF4FC3F7);
            }
        }
        graphics.drawString(font, Component.translatable("screen.economia.map.coordinates", cursorWorldX, cursorWorldZ),
                left + 8, height - 18, 0xFFE4ECEF, false);
    }

    private void drawLoadedTerrain(GuiGraphics graphics, int left, int centerScreenX, int centerScreenY,
                                   double scale, int firstChunkX, int lastChunkX,
                                   int firstChunkZ, int lastChunkZ) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return;
        }
        int playerChunkX = minecraft.player.chunkPosition().x;
        int playerChunkZ = minecraft.player.chunkPosition().z;
        int minChunkX = Math.max(firstChunkX, playerChunkX - 16);
        int maxChunkX = Math.min(lastChunkX, playerChunkX + 16);
        int minChunkZ = Math.max(firstChunkZ, playerChunkZ - 16);
        int maxChunkZ = Math.min(lastChunkZ, playerChunkZ + 16);
        int chunkPixels = Math.max(1, (int) Math.round(16.0D * scale));
        int samples = chunkPixels >= 48 ? 8 : chunkPixels >= 12 ? 4 : 2;
        terrainSamplesThisFrame = 0;
        graphics.enableScissor(left, 0, width, height);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!minecraft.level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                int chunkLeft = worldToScreenX(chunkX * 16, centerScreenX, scale);
                int chunkTop = worldToScreenY(chunkZ * 16, centerScreenY, scale);
                int chunkRight = worldToScreenX((chunkX + 1) * 16, centerScreenX, scale);
                int chunkBottom = worldToScreenY((chunkZ + 1) * 16, centerScreenY, scale);
                for (int sampleX = 0; sampleX < samples; sampleX++) {
                    for (int sampleZ = 0; sampleZ < samples; sampleZ++) {
                        int blockX = chunkX * 16 + (sampleX * 16 + 8) / samples;
                        int blockZ = chunkZ * 16 + (sampleZ * 16 + 8) / samples;
                        int color = terrainColor(blockX, blockZ);
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
        graphics.disableScissor();
    }

    private int terrainColor(int blockX, int blockZ) {
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

    private void drawChunkSelection(GuiGraphics graphics, int left, int centerScreenX, int centerScreenY,
                                    double scale, int chunkX, int chunkZ) {
        int x1 = Math.max(left, worldToScreenX(chunkX * 16, centerScreenX, scale));
        int y1 = Math.max(0, worldToScreenY(chunkZ * 16, centerScreenY, scale));
        int x2 = Math.min(width, worldToScreenX((chunkX + 1) * 16, centerScreenX, scale));
        int y2 = Math.min(height, worldToScreenY((chunkZ + 1) * 16, centerScreenY, scale));
        if (x2 > x1 && y2 > y1) {
            graphics.fill(x1, y1, x2, y2, 0x66FFD54F);
            graphics.fill(x1, y1, x2, y1 + 2, 0xFFFFD54F);
            graphics.fill(x1, y2 - 2, x2, y2, 0xFFFFD54F);
            graphics.fill(x1, y1, x1 + 2, y2, 0xFFFFD54F);
            graphics.fill(x2 - 2, y1, x2, y2, 0xFFFFD54F);
        }
    }

    private void drawClaimPurchasePanel(GuiGraphics graphics) {
        graphics.drawCenteredString(font, Component.translatable("screen.economia.map.chunk_select_title"),
                SIDEBAR_WIDTH / 2, 45, 0xFFFFFFFF);
        int instructionY = 68;
        for (var line : font.split(Component.translatable("screen.economia.map.chunk_select_instruction"),
                SIDEBAR_WIDTH - 28)) {
            graphics.drawString(font, line, 14, instructionY, 0xFFD6E0E4, false);
            instructionY += 10;
        }
        graphics.drawString(font, Component.translatable("screen.economia.map.chunk_selected",
                cursorWorldX >> 4, cursorWorldZ >> 4), 14, 100, 0xFFB0BEC5, false);
        graphics.drawString(font, Component.translatable("screen.economia.map.chunk_price", selectedChunkPrice()),
                14, 118, 0xFFFFD180, false);
    }

    private void openPurchaseConfirmation() {
        selectedPurchaseChunkX = cursorWorldX >> 4;
        selectedPurchaseChunkZ = cursorWorldZ >> 4;
        selectedPurchasePrice = selectedChunkPrice();
        purchaseConfirmation = true;
        int panelX = width / 2 - 135;
        int panelY = height / 2 - 60;
        confirmPurchaseButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.economia.map.confirm_purchase"), ignored -> confirmPurchase())
                .bounds(panelX + 18, panelY + 92, 112, 20).build());
        cancelPurchaseButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.economia.common.cancel"), ignored -> closePurchaseConfirmation())
                .bounds(panelX + 140, panelY + 92, 112, 20).build());
    }

    private void drawPurchaseConfirmation(GuiGraphics graphics) {
        int panelX = width / 2 - 135;
        int panelY = height / 2 - 60;
        graphics.fill(panelX, panelY, panelX + 270, panelY + 122, 0xFA171B1D);
        graphics.fill(panelX + 6, panelY + 6, panelX + 264, panelY + 30, 0xFF2F4B5B);
        graphics.drawCenteredString(font, Component.translatable("screen.economia.map.purchase_title"),
                width / 2, panelY + 14, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("screen.economia.map.purchase_chunk",
                selectedPurchaseChunkX, selectedPurchaseChunkZ), panelX + 18, panelY + 40, 0xFFE1E8EB, false);
        graphics.drawString(font, Component.translatable("screen.economia.map.purchase_value",
                selectedPurchasePrice), panelX + 18, panelY + 55, 0xFFFFD180, false);
        int warningY = panelY + 70;
        for (var line : font.split(Component.translatable("screen.economia.map.purchase_irreversible"), 234)) {
            graphics.drawString(font, line, panelX + 18, warningY, 0xFFFF8A80, false);
            warningY += 10;
        }
    }

    private void confirmPurchase() {
        PacketDistributor.sendToServer(new MapActionPayload(MapAction.PURCHASE_CLAIM,
                claimPurchase.groupType().name(), claimPurchase.dimension(),
                selectedPurchaseChunkX, 0, selectedPurchaseChunkZ, claimPurchase.anchorId()));
        onClose();
    }

    private void closePurchaseConfirmation() {
        purchaseConfirmation = false;
        removeWidget(confirmPurchaseButton);
        removeWidget(cancelPurchaseButton);
        confirmPurchaseButton = null;
        cancelPurchaseButton = null;
    }

    private long selectedChunkPrice() {
        if (claimPurchase == null || lastMouseX < SIDEBAR_WIDTH) {
            return claimPurchase == null ? 0L : claimPurchase.initialChunkPrice();
        }
        return claimPrices.landPrice(claimPurchase.dimension(),
                (cursorWorldX >> 4) * 16 + 8, (cursorWorldZ >> 4) * 16 + 8);
    }

    private void drawLocations(GuiGraphics graphics) {
        graphics.fill(8, 92, SIDEBAR_WIDTH - 8, Math.min(height - 40, 104 + data.locations().size() * 24), 0xEE111719);
        int y = 99;
        int end = Math.min(data.locations().size(), locationListOffset + visibleLocationRows());
        for (int index = locationListOffset; index < end; index++) {
            MapDataPayload.LocationSummary location = data.locations().get(index);
            graphics.drawString(font, location.name(), 14, y, 0xFFFFFFFF, false);
            graphics.drawString(font, "X:" + location.x() + " Z:" + location.z(), 14, y + 10, 0xFFB0BEC5, false);
            y += 24;
        }
    }

    private void addChannelButton(ChatChannel channel, int x) {
        Button button = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.economia.map.channel." + channel.translationSuffix()), ignored -> {
                    selectedChannel = channel;
                    PacketDistributor.sendToServer(new ChatChannelPayload(channel));
                })
                .bounds(x, 66, 48, 20).build());
        button.active = channel == ChatChannel.GENERAL
                || channel == ChatChannel.CLAN && data.hasClan()
                || channel == ChatChannel.PRIVATE_PROPERTY && data.hasPrivateProperty();
    }

    private void rebuildLocationWidgets() {
        for (AbstractWidget widget : locationWidgets) removeWidget(widget);
        locationWidgets.clear();
        if (!showLocations) return;
        int y = 96;
        int end = Math.min(data.locations().size(), locationListOffset + visibleLocationRows());
        for (int index = locationListOffset; index < end; index++) {
            MapDataPayload.LocationSummary location = data.locations().get(index);
            Button center = Button.builder(Component.literal("•"), ignored -> {
                centerX = location.x();
                centerZ = location.z();
            }).bounds(108, y, 13, 18).build();
            Button edit = Button.builder(Component.literal("E"), ignored -> openLocationModal(location))
                    .bounds(122, y, 13, 18).build();
            Button share = Button.builder(Component.literal("↗"), ignored -> openShareModal(location))
                    .bounds(136, y, 13, 18).build();
            Button delete = Button.builder(Component.literal("×"), ignored -> {
                PacketDistributor.sendToServer(new MapActionPayload(MapAction.DELETE_LOCATION, "", "", 0, 0, 0,
                        location.id()));
            }).bounds(150, y, 13, 18).build();
            locationWidgets.add(addRenderableWidget(center));
            locationWidgets.add(addRenderableWidget(edit));
            locationWidgets.add(addRenderableWidget(share));
            locationWidgets.add(addRenderableWidget(delete));
            y += 24;
        }
        int maxOffset = Math.max(0, data.locations().size() - visibleLocationRows());
        if (locationListOffset > 0) {
            Button previous = Button.builder(Component.literal("▲"), ignored -> {
                locationListOffset = Math.max(0, locationListOffset - visibleLocationRows());
                rebuildLocationWidgets();
            }).bounds(62, height - 58, 24, 18).build();
            locationWidgets.add(addRenderableWidget(previous));
        }
        if (locationListOffset < maxOffset) {
            Button next = Button.builder(Component.literal("▼"), ignored -> {
                locationListOffset = Math.min(maxOffset, locationListOffset + visibleLocationRows());
                rebuildLocationWidgets();
            }).bounds(90, height - 58, 24, 18).build();
            locationWidgets.add(addRenderableWidget(next));
        }
    }

    private void openShareModal(MapDataPayload.LocationSummary location) {
        pendingShare = location;
        shareModal = true;
        rebuildWidgets();
    }

    private void addShareButton(ChatChannel channel, int x, int y) {
        Button button = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.economia.map.channel." + channel.translationSuffix()), ignored -> {
                    PacketDistributor.sendToServer(new ShareLocationPayload(pendingShare.id(), channel));
                    pendingShare = null;
                    shareModal = false;
                    rebuildWidgets();
                }).bounds(x, y, 70, 20).build());
        button.active = channel == ChatChannel.GENERAL
                || channel == ChatChannel.CLAN && data.hasClan()
                || channel == ChatChannel.PRIVATE_PROPERTY && data.hasPrivateProperty();
    }

    private void drawShareModal(GuiGraphics graphics) {
        int x = width / 2 - 125;
        int y = height / 2 - 45;
        graphics.fill(x, y, x + 250, y + 90, 0xF51A2328);
        graphics.drawCenteredString(font, Component.translatable("screen.economia.map.confirm_share",
                pendingShare == null ? "" : pendingShare.name()), width / 2, y + 14, 0xFFFFFFFF);
    }

    private void drawSharedLocation(GuiGraphics graphics) {
        int x = SIDEBAR_WIDTH + 12;
        int panelWidth = Math.max(100, Math.min(230, width - x - 8));
        graphics.fill(x, 10, x + panelWidth, 48, 0xDD1A2328);
        Component sharedTitle = Component.translatable("screen.economia.map.shared", sharedLocation.name());
        graphics.drawString(font, font.plainSubstrByWidth(sharedTitle.getString(), panelWidth - 16),
                x + 8, 17, 0xFF80DEEA, false);
        String coordinates = sharedLocation.dimension() + "  X:" + sharedLocation.x() + " Y:"
                + sharedLocation.y() + " Z:" + sharedLocation.z();
        graphics.drawString(font, font.plainSubstrByWidth(coordinates, panelWidth - 16),
                x + 8, 31, 0xFFFFFFFF, false);
    }

    private void addSharedLocationActions() {
        if (sharedLocation == null || sharedResolved) {
            return;
        }
        int x = SIDEBAR_WIDTH + 12;
        addRenderableWidget(Button.builder(Component.translatable("screen.economia.common.save"), ignored -> {
            PacketDistributor.sendToServer(new MapActionPayload(MapAction.SAVE_LOCATION,
                    sharedLocation.name(), sharedLocation.dimension(), sharedLocation.x(), sharedLocation.y(),
                    sharedLocation.z(), new UUID(0L, 0L)));
            sharedResolved = true;
            rebuildWidgets();
        }).bounds(x, 52, 72, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.economia.common.cancel"), ignored -> {
            sharedResolved = true;
            rebuildWidgets();
        }).bounds(x + 78, 52, 72, 20).build());
    }

    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();
        if (shareModal) {
            int y = height / 2;
            addShareButton(ChatChannel.GENERAL, width / 2 - 110, y);
            addShareButton(ChatChannel.CLAN, width / 2 - 35, y);
            addShareButton(ChatChannel.PRIVATE_PROPERTY, width / 2 + 40, y);
            addRenderableWidget(Button.builder(Component.translatable("screen.economia.common.cancel"), ignored -> {
                pendingShare = null;
                shareModal = false;
                rebuildWidgets();
            }).bounds(width / 2 - 35, y + 25, 70, 20).build());
        }
    }

    private void openLocationModal() {
        openLocationModal(null);
    }

    private void openLocationModal(MapDataPayload.LocationSummary location) {
        if (locationModal) {
            return;
        }
        locationModal = true;
        editingLocation = location;
        int panelX = width / 2 - 130;
        int panelY = height / 2 - 80;
        locationName = addRenderableWidget(new EditBox(font, panelX + 20, panelY + 34, 220, 20,
                Component.translatable("screen.economia.map.location_name")));
        locationName.setMaxLength(64);
        if (location != null) locationName.setValue(location.name());
        locationX = coordinateField(panelX + 20, panelY + 64, location == null ? cursorWorldX : location.x());
        locationY = coordinateField(panelX + 94, panelY + 64,
                location == null ? minecraft != null && minecraft.player != null ? minecraft.player.getBlockY() : 64 : location.y());
        locationZ = coordinateField(panelX + 168, panelY + 64, location == null ? cursorWorldZ : location.z());
        saveLocation = addRenderableWidget(Button.builder(Component.translatable(location == null
                        ? "screen.economia.common.create" : "screen.economia.common.save"),
                button -> saveLocation()).bounds(panelX + 134, panelY + 112, 106, 20).build());
        cancelLocation = addRenderableWidget(Button.builder(Component.translatable("screen.economia.common.cancel"),
                button -> closeLocationModal()).bounds(panelX + 20, panelY + 112, 106, 20).build());
        setInitialFocus(locationName);
    }

    private EditBox coordinateField(int x, int y, int value) {
        EditBox field = addRenderableWidget(new EditBox(font, x, y, 66, 20, Component.empty()));
        field.setMaxLength(12);
        field.setFilter(text -> text.matches("-?[0-9]*"));
        field.setValue(Integer.toString(value));
        return field;
    }

    private void saveLocation() {
        try {
            String dimension = editingLocation != null ? editingLocation.dimension()
                    : minecraft != null && minecraft.level != null
                    ? minecraft.level.dimension().location().toString() : "minecraft:overworld";
            PacketDistributor.sendToServer(new MapActionPayload(editingLocation == null
                    ? MapAction.SAVE_LOCATION : MapAction.UPDATE_LOCATION, locationName.getValue(), dimension,
                    Integer.parseInt(locationX.getValue()), Integer.parseInt(locationY.getValue()),
                    Integer.parseInt(locationZ.getValue()), editingLocation == null
                    ? new UUID(0L, 0L) : editingLocation.id()));
            closeLocationModal();
        } catch (NumberFormatException ignored) {
        }
    }

    private void closeLocationModal() {
        locationModal = false;
        removeWidget(locationName);
        removeWidget(locationX);
        removeWidget(locationY);
        removeWidget(locationZ);
        removeWidget(saveLocation);
        removeWidget(cancelLocation);
        locationName = null;
        editingLocation = null;
    }

    private void drawLocationModal(GuiGraphics graphics) {
        int panelX = width / 2 - 130;
        int panelY = height / 2 - 80;
        graphics.fill(panelX, panelY, panelX + 260, panelY + 150, 0xF51A2328);
        graphics.drawCenteredString(font, Component.translatable(editingLocation == null
                ? "screen.economia.map.new_location" : "screen.economia.map.edit_location"), width / 2, panelY + 12,
                0xFFFFFFFF);
        graphics.drawString(font, "X", panelX + 20, panelY + 56, 0xFFB0BEC5, false);
        graphics.drawString(font, "Y", panelX + 94, panelY + 56, 0xFFB0BEC5, false);
        graphics.drawString(font, "Z", panelX + 168, panelY + 56, 0xFFB0BEC5, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (purchaseConfirmation && keyCode == 256) {
            closePurchaseConfirmation();
            return true;
        }
        if (!locationModal && !shareModal && ModKeyMappings.CREATE_LOCATION.matches(keyCode, scanCode)) {
            openLocationModal();
            return true;
        }
        if (shareModal && keyCode == 256) {
            pendingShare = null;
            shareModal = false;
            rebuildWidgets();
            return true;
        }
        if (locationModal && keyCode == 256) {
            closeLocationModal();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (purchaseConfirmation) {
            return false;
        }
        int dragButton = claimPurchase == null ? 0 : 1;
        if (!locationModal && !shareModal && button == dragButton && mouseX >= SIDEBAR_WIDTH) {
            centerX -= dragX / pixelsPerBlock();
            centerZ -= dragY / pixelsPerBlock();
            mapDragged = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        int dragButton = claimPurchase == null ? 0 : 1;
        if (button == dragButton && mapDragged) {
            mapDragged = false;
            refreshVisibleClaims();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (purchaseConfirmation) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (claimPurchase != null && button == 0 && mouseX >= SIDEBAR_WIDTH) {
            openPurchaseConfirmation();
            return true;
        }
        if (claimPurchase != null && button == 1 && mouseX >= SIDEBAR_WIDTH) {
            return true;
        }
        if (!locationModal && !shareModal && button == 1 && mouseX >= SIDEBAR_WIDTH) {
            PacketDistributor.sendToServer(new MapActionPayload(MapAction.TOGGLE_CLAIM,
                    selectedChannel.name(), "",
                    cursorWorldX >> 4, 0, cursorWorldZ >> 4, new UUID(0L, 0L)));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (purchaseConfirmation) {
            return false;
        }
        if (!locationModal && !shareModal && mouseX >= SIDEBAR_WIDTH) {
            zoom = Math.max(0.25D, Math.min(4.0D, zoom * (scrollY > 0 ? 1.2D : 1.0D / 1.2D)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private double pixelsPerBlock() {
        return Math.max(0.25D, 1.25D * zoom);
    }

    private int visibleLocationRows() {
        return Math.max(1, (height - 158) / 24);
    }

    private void refreshVisibleClaims() {
        PacketDistributor.sendToServer(new MapActionPayload(MapAction.REFRESH, "", "",
                ((int) Math.floor(centerX)) >> 4, 0, ((int) Math.floor(centerZ)) >> 4,
                new UUID(0L, 0L)));
    }

    private int worldToScreenX(double worldX, int screenCenter, double scale) {
        return (int) Math.round(screenCenter + (worldX - centerX) * scale);
    }

    private int worldToScreenY(double worldZ, int screenCenter, double scale) {
        return (int) Math.round(screenCenter + (worldZ - centerZ) * scale);
    }

    private int screenToWorldX(double screenX, int screenCenter, double scale) {
        return (int) Math.floor(centerX + (screenX - screenCenter) / scale);
    }

    private int screenToWorldZ(double screenY, int screenCenter, double scale) {
        return (int) Math.floor(centerZ + (screenY - screenCenter) / scale);
    }
}
