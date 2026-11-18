package br.com.economiamod.client.config;

import br.com.economiamod.client.map.MinimapCorner;
import br.com.economiamod.client.map.MinimapMath;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Per-player options of the GPS minimap (config/economia-client.toml). */
public final class EconomyClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MINIMAP_ENABLED;
    public static final ModConfigSpec.BooleanValue MINIMAP_SHOW_WHEN_HELD;
    public static final ModConfigSpec.IntValue MINIMAP_SIZE;
    public static final ModConfigSpec.EnumValue<MinimapCorner> MINIMAP_CORNER;
    public static final ModConfigSpec.IntValue MINIMAP_OFFSET_X;
    public static final ModConfigSpec.IntValue MINIMAP_OFFSET_Y;
    public static final ModConfigSpec.DoubleValue MINIMAP_ZOOM;

    static {
        BUILDER.push("gps_minimap");
        MINIMAP_ENABLED = BUILDER
                .comment("Show the Economia map as a minimap while the GPS is in the Curios GPS slot.")
                .define("enabled", true);
        MINIMAP_SHOW_WHEN_HELD = BUILDER
                .comment("Also show the minimap while the GPS is held in either hand (works without Curios).")
                .define("showWhenHeld", true);
        MINIMAP_SIZE = BUILDER
                .comment("Side of the minimap square, in GUI pixels. It shrinks on its own if the window is smaller.")
                .defineInRange("size", 100, 48, 256);
        MINIMAP_CORNER = BUILDER
                .comment("Screen corner of the minimap. TOP_RIGHT moves below the effect icons when they show.")
                .defineEnum("corner", MinimapCorner.TOP_RIGHT);
        MINIMAP_OFFSET_X = BUILDER
                .comment("Distance from the left or right edge of the screen, in GUI pixels.")
                .defineInRange("offsetX", 6, 0, 512);
        MINIMAP_OFFSET_Y = BUILDER
                .comment("Distance from the top or bottom edge of the screen, in GUI pixels.")
                .defineInRange("offsetY", 6, 0, 512);
        MINIMAP_ZOOM = BUILDER
                .comment("Minimap zoom in pixels per block (0.25 = widest, 4 = closest). Saved by the zoom keys,"
                        + " numeric keypad + and - by default.")
                .defineInRange("zoom", MinimapMath.DEFAULT_ZOOM,
                        MinimapMath.ZOOM_LEVELS[0], MinimapMath.ZOOM_LEVELS[MinimapMath.ZOOM_LEVELS.length - 1]);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private EconomyClientConfig() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, SPEC);
    }
}
