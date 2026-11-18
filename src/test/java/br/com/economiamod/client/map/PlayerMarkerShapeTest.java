package br.com.economiamod.client.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlayerMarkerShapeTest {
    private static Set<String> keys(int[][] pixels) {
        Set<String> set = new HashSet<>();
        for (int[] pixel : pixels) set.add(pixel[0] + "," + pixel[1]);
        return set;
    }

    @Test
    void arrowPointsRightFromARoundBody() {
        Set<String> fill = keys(PlayerMarkerShape.FILL);
        assertTrue(fill.contains("0,0"), "body centered on the player");
        assertTrue(fill.contains("8,0"), "tip of the >");
        assertFalse(fill.contains("4,0"), "one-pixel gap between the 0 and the >");
        assertFalse(fill.contains("-4,0"));
        for (int[] pixel : PlayerMarkerShape.FILL) assertTrue(pixel[0] <= 8 && pixel[0] >= -3);
    }

    @Test
    void outlineSurroundsWithoutCoveringTheFill() {
        Set<String> fill = keys(PlayerMarkerShape.FILL);
        Set<String> outline = keys(PlayerMarkerShape.OUTLINE);
        assertEquals(PlayerMarkerShape.OUTLINE.length, outline.size(), "no duplicate outline pixels");
        for (String pixel : outline) assertFalse(fill.contains(pixel), pixel);
        assertTrue(outline.contains("9,0") && outline.contains("-4,0"));
    }

    @Test
    void yawTurnsTheArrowWithNorthUp() {
        assertEquals(90.0F, PlayerMarkerShape.rotationDegrees(0.0F), "yaw 0 looks south: arrow down");
        assertEquals(180.0F, PlayerMarkerShape.rotationDegrees(90.0F), "yaw 90 looks west: arrow left");
        assertEquals(270.0F, PlayerMarkerShape.rotationDegrees(180.0F), "yaw 180 looks north: arrow up");
        assertEquals(0.0F, PlayerMarkerShape.rotationDegrees(-90.0F), "yaw -90 looks east: arrow right");
    }
}
