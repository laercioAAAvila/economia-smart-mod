package br.com.economiamod.client.map;

/** Pure rules of the GPS minimap (zoom steps, placement, when to ask the server for fresh claims). */
public final class MinimapMath {
    /** Pixels per block, from far (a wide area) to near. */
    public static final double[] ZOOM_LEVELS = {0.25D, 0.375D, 0.5D, 0.75D, 1.0D, 1.5D, 2.0D, 3.0D, 4.0D};
    public static final double DEFAULT_ZOOM = 1.0D;
    /** Height of the coordinates line under the map. */
    public static final int LABEL_HEIGHT = 11;
    /** Vanilla draws effect icons in two 26-pixel rows at the top right. */
    public static final int EFFECT_ROWS_HEIGHT = 52;
    /** Ask again every 30 seconds, so claims and saved places made meanwhile show up. */
    public static final int REFRESH_INTERVAL_TICKS = 600;
    /** The server answers with claims 64 chunks around the center; refresh well before walking out of them. */
    public static final int REFRESH_DISTANCE_CHUNKS = 16;

    private MinimapMath() {
    }

    /** Index of the zoom level closest to {@code zoom}. */
    public static int nearestLevel(double zoom) {
        int best = 0;
        for (int i = 1; i < ZOOM_LEVELS.length; i++) {
            if (Math.abs(ZOOM_LEVELS[i] - zoom) < Math.abs(ZOOM_LEVELS[best] - zoom)) best = i;
        }
        return best;
    }

    /** The zoom one step nearer ({@code steps > 0}) or farther ({@code steps < 0}), stopping at the ends. */
    public static double step(double zoom, int steps) {
        int index = Math.max(0, Math.min(ZOOM_LEVELS.length - 1, nearestLevel(zoom) + steps));
        return ZOOM_LEVELS[index];
    }

    /** The map side that fits the screen, so a big configured size never pushes the map off a small window. */
    public static int fittedSize(int size, int screenWidth, int screenHeight, int offsetX, int offsetY) {
        int room = Math.min(screenWidth - 2 * offsetX, screenHeight - 2 * offsetY - LABEL_HEIGHT);
        return Math.max(16, Math.min(size, room));
    }

    /**
     * Top-left pixel of the map square. {@code effectsShown} moves a top-right map below the effect icons.
     */
    public static int[] position(MinimapCorner corner, int screenWidth, int screenHeight, int size,
                                 int offsetX, int offsetY, boolean effectsShown) {
        boolean right = corner == MinimapCorner.TOP_RIGHT || corner == MinimapCorner.BOTTOM_RIGHT;
        boolean bottom = corner == MinimapCorner.BOTTOM_LEFT || corner == MinimapCorner.BOTTOM_RIGHT;
        int x = right ? screenWidth - offsetX - size : offsetX;
        int y = bottom ? screenHeight - offsetY - LABEL_HEIGHT - size : offsetY;
        if (corner == MinimapCorner.TOP_RIGHT && effectsShown) {
            y = Math.min(y + EFFECT_ROWS_HEIGHT, Math.max(offsetY, screenHeight - size - LABEL_HEIGHT));
        }
        return new int[] {x, y};
    }

    /** True when the claims on the minimap should be asked for again. */
    public static boolean shouldRefresh(boolean everRefreshed, boolean sameDimension, int ticksSinceRefresh,
                                        int lastChunkX, int lastChunkZ, int chunkX, int chunkZ) {
        if (!everRefreshed || !sameDimension || ticksSinceRefresh >= REFRESH_INTERVAL_TICKS) return true;
        return Math.max(Math.abs(chunkX - lastChunkX), Math.abs(chunkZ - lastChunkZ)) >= REFRESH_DISTANCE_CHUNKS;
    }
}
