package br.com.economiamod.client.map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinimapMathTest {
    @Test
    void zoomStepsWalkTheLevelsAndStopAtTheEnds() {
        assertEquals(1.5D, MinimapMath.step(1.0D, 1));
        assertEquals(0.75D, MinimapMath.step(1.0D, -1));
        assertEquals(4.0D, MinimapMath.step(4.0D, 1), "nearest end stays");
        assertEquals(0.25D, MinimapMath.step(0.25D, -3), "farthest end stays");
        assertEquals(4.0D, MinimapMath.step(0.25D, 100));
    }

    @Test
    void anOddSavedZoomSnapsToTheNearestLevel() {
        assertEquals(1.0D, MinimapMath.step(1.1D, 0));
        assertEquals(2.0D, MinimapMath.step(1.1D, 2));
        assertEquals(0.25D, MinimapMath.step(-5.0D, 0));
    }

    @Test
    void levelsGrowAndContainTheDefault() {
        for (int i = 1; i < MinimapMath.ZOOM_LEVELS.length; i++) {
            assertTrue(MinimapMath.ZOOM_LEVELS[i] > MinimapMath.ZOOM_LEVELS[i - 1]);
        }
        assertEquals(MinimapMath.DEFAULT_ZOOM, MinimapMath.ZOOM_LEVELS[MinimapMath.nearestLevel(MinimapMath.DEFAULT_ZOOM)]);
    }

    @Test
    void cornersPlaceTheMapInsideTheScreen() {
        assertArrayEquals(new int[] {314, 6}, MinimapMath.position(MinimapCorner.TOP_RIGHT, 420, 240, 100, 6, 6, false));
        assertArrayEquals(new int[] {6, 6}, MinimapMath.position(MinimapCorner.TOP_LEFT, 420, 240, 100, 6, 6, false));
        assertArrayEquals(new int[] {314, 123}, MinimapMath.position(MinimapCorner.BOTTOM_RIGHT, 420, 240, 100, 6, 6, false));
        assertArrayEquals(new int[] {6, 123}, MinimapMath.position(MinimapCorner.BOTTOM_LEFT, 420, 240, 100, 6, 6, false));
    }

    @Test
    void topRightMapMovesBelowEffectIconsOnlyThere() {
        assertArrayEquals(new int[] {314, 58}, MinimapMath.position(MinimapCorner.TOP_RIGHT, 420, 240, 100, 6, 6, true));
        assertArrayEquals(new int[] {6, 6}, MinimapMath.position(MinimapCorner.TOP_LEFT, 420, 240, 100, 6, 6, true));
        int[] tight = MinimapMath.position(MinimapCorner.TOP_RIGHT, 420, 130, 100, 6, 6, true);
        assertTrue(tight[1] + 100 + MinimapMath.LABEL_HEIGHT <= 130, "never pushed off the bottom");
    }

    @Test
    void bigSizesShrinkToFitSmallWindows() {
        assertEquals(100, MinimapMath.fittedSize(100, 420, 240, 6, 6));
        assertEquals(256 - 12 - MinimapMath.LABEL_HEIGHT, MinimapMath.fittedSize(256, 640, 256, 6, 6));
        assertEquals(16, MinimapMath.fittedSize(100, 10, 10, 6, 6));
    }

    @Test
    void claimsAreAskedForAtStartOnNewDimensionAfterTimeOrAfterWalkingFar() {
        assertTrue(MinimapMath.shouldRefresh(false, true, 0, 0, 0, 0, 0));
        assertTrue(MinimapMath.shouldRefresh(true, false, 0, 0, 0, 0, 0));
        assertTrue(MinimapMath.shouldRefresh(true, true, MinimapMath.REFRESH_INTERVAL_TICKS, 0, 0, 0, 0));
        assertTrue(MinimapMath.shouldRefresh(true, true, 1, 0, 0, MinimapMath.REFRESH_DISTANCE_CHUNKS, 0));
        assertTrue(MinimapMath.shouldRefresh(true, true, 1, 0, 0, 0, -MinimapMath.REFRESH_DISTANCE_CHUNKS));
        assertFalse(MinimapMath.shouldRefresh(true, true, 1, 0, 0, 15, -15));
    }
}
