package com.appletv.launcher;

import static org.junit.Assert.assertEquals;
import com.appletv.contract.TimeFormatter;
import org.junit.Test;

public class ModeManagerTimeTest {

    @Test
    public void pillTextFormattedCorrectlyForKidMode() {
        assertEquals("15m", TimeFormatter.formatShortRemaining(900));
        assertEquals("12m", TimeFormatter.formatShortRemaining(720));
        assertEquals("45s", TimeFormatter.formatShortRemaining(45));
    }

    @Test
    public void trafficLightColorThresholds() {
        assertEquals(0xFFFFFFFF, ModeManager.getTimePillColor(600)); // > 5m: white
        assertEquals(0xFFFFD60A, ModeManager.getTimePillColor(240)); // 2-5m: gold
        assertEquals(0xFFFF453A, ModeManager.getTimePillColor(90));  // < 2m: red
    }
}
