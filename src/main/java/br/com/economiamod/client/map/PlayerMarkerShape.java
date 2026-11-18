package br.com.economiamod.client.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The player on the map as a "0>" arrow, as plain pixel offsets (no Minecraft classes, so it is unit tested).
 * A round body (the "0") and, one pixel ahead, a point (the ">") toward where the player looks, drawn pointing
 * right and turned by {@link #rotationDegrees(float)}.
 */
public final class PlayerMarkerShape {
    /** Pixels of the arrow pointing right, centered on the player: {x, y} pairs. */
    public static final int[][] FILL = fill();
    /** One-pixel dark ring around {@link #FILL}, so the arrow reads over any terrain color. */
    public static final int[][] OUTLINE = outline(FILL);

    private PlayerMarkerShape() {
    }

    /**
     * The map has north up (X to the right, Z down). Minecraft yaw 0 faces south (+Z, down here) and grows
     * clockwise seen from above, so the right-pointing shape is turned by yaw + 90 degrees.
     */
    public static float rotationDegrees(float yawDegrees) {
        return yawDegrees + 90.0F;
    }

    private static int[][] fill() {
        List<int[]> pixels = new ArrayList<>();
        for (int y = -3; y <= 3; y++) {
            for (int x = -3; x <= 3; x++) {
                if (x * x + y * y <= 10) pixels.add(new int[] {x, y}); // the "0"
            }
        }
        for (int x = 5; x <= 8; x++) {
            int half = 8 - x;
            for (int y = -half; y <= half; y++) pixels.add(new int[] {x, y}); // the ">"
        }
        return pixels.toArray(new int[0][]);
    }

    private static int[][] outline(int[][] fill) {
        Set<Long> inside = new HashSet<>();
        for (int[] pixel : fill) inside.add(pack(pixel[0], pixel[1]));
        Set<Long> outline = new LinkedHashSet<>();
        for (int[] pixel : fill) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    long key = pack(pixel[0] + dx, pixel[1] + dy);
                    if (!inside.contains(key)) outline.add(key);
                }
            }
        }
        int[][] pixels = new int[outline.size()][];
        int i = 0;
        for (long key : outline) pixels[i++] = new int[] {(int) (key >> 32), (int) key};
        return pixels;
    }

    private static long pack(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }
}
