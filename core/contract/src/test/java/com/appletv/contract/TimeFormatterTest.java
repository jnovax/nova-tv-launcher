package com.appletv.contract;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TimeFormatterTest {

    @Test
    public void formatsZeroAndNegativeSeconds() {
        assertEquals("0s", TimeFormatter.formatShortRemaining(0));
        assertEquals("0s", TimeFormatter.formatShortRemaining(-10));
    }

    @Test
    public void formatsSecondsUnderOneMinute() {
        assertEquals("45s", TimeFormatter.formatShortRemaining(45));
        assertEquals("1s", TimeFormatter.formatShortRemaining(1));
        assertEquals("59s", TimeFormatter.formatShortRemaining(59));
    }

    @Test
    public void formatsMinutesRoundingUp() {
        assertEquals("1m", TimeFormatter.formatShortRemaining(60));
        assertEquals("2m", TimeFormatter.formatShortRemaining(61));
        assertEquals("12m", TimeFormatter.formatShortRemaining(720));
        assertEquals("15m", TimeFormatter.formatShortRemaining(900));
    }
}
