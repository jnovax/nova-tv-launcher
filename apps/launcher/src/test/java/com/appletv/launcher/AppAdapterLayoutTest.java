package com.appletv.launcher;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppAdapterLayoutTest {

    @Test
    public void fiveColumnsYieldsExactThreeHundredPixelIconAt1080p() {
        float density = 2.0f; // 1080p base at 320dpi: 1dp = 2px
        int screenWidthPx = 1920;
        int safeMarginPx = 90 * 2; // left + right safe area (45dp * 2 * 2) = 180px
        int innerWidthPx = screenWidthPx - safeMarginPx; // 1740px

        int tileWidth = AppAdapter.tileWidthPx(innerWidthPx, 5, density);
        assertEquals(300, tileWidth);

        int tileHeight = AppAdapter.tileHeightPx(tileWidth);
        assertEquals(180, tileHeight);
    }

    @Test
    public void sixColumnsShrinksProportionallyMaintainingFiveThreeAspectRatio() {
        float density = 2.0f;
        int innerWidthPx = 1740;

        int tileWidth = AppAdapter.tileWidthPx(innerWidthPx, 6, density);
        assertEquals(242, tileWidth);

        int tileHeight = AppAdapter.tileHeightPx(tileWidth);
        assertEquals(145, tileHeight);
        // Aspect ratio 5:3: 242 * 3 / 5 = 145.2 -> 145
    }

    @Test
    public void cornerRadiusMatchesSpec() {
        float density = 2.0f;
        float radius = AppAdapter.cornerRadius(300, density);
        assertEquals(24.0f, radius, 0.001f);
    }

    @Test
    public void focusedDimensionsMatchAppleTvSpec() {
        int unfocusedW = 300;
        int unfocusedH = 180;
        float focusedW = unfocusedW * AppAdapter.FOCUS_SCALE;
        float focusedH = unfocusedH * AppAdapter.FOCUS_SCALE;

        assertEquals(370f, focusedW, 0.5f);
        assertEquals(222f, focusedH, 0.5f);
    }
}
