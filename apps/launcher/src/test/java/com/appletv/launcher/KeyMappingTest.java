package com.appletv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class KeyMappingTest {

    @Test
    public void parsesRemoteKeysSuccessfully() throws Exception {
        Map<Integer, String> mappings = new HashMap<>();
        String jsonStr = "{"
                + "\"remote_keys\": {"
                + "\"190\": \"io.gh.reisxd.tizentube.cobalt\","
                + "\"178\": \"inputs\","
                + "\"4001\": \"wallpaper_cycle\","
                + "\"83\": \"quick_settings\","
                + "\"150\": \"recent_apps\""
                + "}"
                + "}";
        JSONObject json = new JSONObject(jsonStr);
        Config.parseRemoteKeys(json, mappings);

        assertEquals("io.gh.reisxd.tizentube.cobalt", mappings.get(190));
        assertEquals("inputs", mappings.get(178));
        assertEquals("wallpaper_cycle", mappings.get(4001));
        assertEquals("quick_settings", mappings.get(83));
        assertEquals("recent_apps", mappings.get(150));
    }

    @Test
    public void ignoresNonNumericOrEmptyKeys() throws Exception {
        Map<Integer, String> mappings = new HashMap<>();
        String jsonStr = "{"
                + "\"remote_keys\": {\"abc\": \"quick_settings\", \"100\": \"  \", \"200\": \"valid.app\"}"
                + "}";
        JSONObject json = new JSONObject(jsonStr);
        Config.parseRemoteKeys(json, mappings);

        assertNull(mappings.get(100));
        assertEquals("valid.app", mappings.get(200));
    }

    @Test
    public void resolvesActionTypesCorrectly() {
        assertEquals("quick_settings", HomeKeyService.getActionType("quick_settings"));
        assertEquals("recent_apps", HomeKeyService.getActionType("recent_apps"));
        assertEquals("inputs", HomeKeyService.getActionType("inputs"));
        assertEquals("wallpaper_cycle", HomeKeyService.getActionType("wallpaper_cycle"));
        assertEquals("wallpaper_picker", HomeKeyService.getActionType("wallpaper_picker"));

        assertEquals("app", HomeKeyService.getActionType("io.gh.reisxd.tizentube.cobalt"));
        assertEquals("app", HomeKeyService.getActionType("vn.mytvnet.b2cott"));
    }
}
