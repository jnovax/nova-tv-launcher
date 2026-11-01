package com.appletv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class KeyMappingTest {

    @Test
    public void parsesKeyMappingsAndRemoteKeys() throws Exception {
        Map<Integer, String> mappings = new HashMap<>();
        String jsonStr = "{"
                + "\"key_mappings\": {\"190\": \"io.gh.reisxd.tizentube.cobalt\", \"178\": \"action:inputs\"},"
                + "\"remote_keys\": {\"229\": \"wallpaper_cycle\", \"170\": \"quick_settings\"}"
                + "}";
        JSONObject json = new JSONObject(jsonStr);
        Config.parseKeyMappings(json, mappings);

        assertEquals("io.gh.reisxd.tizentube.cobalt", mappings.get(190));
        assertEquals("action:inputs", mappings.get(178));
        assertEquals("wallpaper_cycle", mappings.get(229));
        assertEquals("quick_settings", mappings.get(170));
    }

    @Test
    public void remoteKeysOverridesKeyMappings() throws Exception {
        Map<Integer, String> mappings = new HashMap<>();
        String jsonStr = "{"
                + "\"key_mappings\": {\"178\": \"inputs\"},"
                + "\"remote_keys\": {\"178\": \"action:inputs\"}"
                + "}";
        JSONObject json = new JSONObject(jsonStr);
        Config.parseKeyMappings(json, mappings);

        assertEquals("action:inputs", mappings.get(178));
    }

    @Test
    public void ignoresNonNumericOrEmptyKeys() throws Exception {
        Map<Integer, String> mappings = new HashMap<>();
        String jsonStr = "{"
                + "\"remote_keys\": {\"abc\": \"quick_settings\", \"100\": \"  \", \"200\": \"valid.app\"}"
                + "}";
        JSONObject json = new JSONObject(jsonStr);
        Config.parseKeyMappings(json, mappings);

        assertNull(mappings.get(100));
        assertEquals("valid.app", mappings.get(200));
    }

    @Test
    public void resolvesActionTypesCorrectly() {
        assertEquals("quick_settings", HomeKeyService.getActionType("quick_settings"));
        assertEquals("quick_settings", HomeKeyService.getActionType("action:quick_settings"));
        assertEquals("quick_settings", HomeKeyService.getActionType("settings"));
        assertEquals("quick_settings", HomeKeyService.getActionType("action:settings"));

        assertEquals("recent_apps", HomeKeyService.getActionType("recent_apps"));
        assertEquals("recent_apps", HomeKeyService.getActionType("action:recent_apps"));
        assertEquals("recent_apps", HomeKeyService.getActionType("recents"));

        assertEquals("inputs", HomeKeyService.getActionType("inputs"));
        assertEquals("inputs", HomeKeyService.getActionType("action:inputs"));

        assertEquals("wallpaper_cycle", HomeKeyService.getActionType("wallpaper_cycle"));
        assertEquals("wallpaper_cycle", HomeKeyService.getActionType("action:wallpaper_cycle"));
        assertEquals("wallpaper_cycle", HomeKeyService.getActionType("wallpaper"));
        assertEquals("wallpaper_cycle", HomeKeyService.getActionType("action:wallpaper"));

        assertEquals("wallpaper_picker", HomeKeyService.getActionType("wallpaper_picker"));
        assertEquals("wallpaper_picker", HomeKeyService.getActionType("wallpaper_grid"));
        assertEquals("wallpaper_picker", HomeKeyService.getActionType("action:wallpaper_picker"));

        assertEquals("app", HomeKeyService.getActionType("io.gh.reisxd.tizentube.cobalt"));
        assertEquals("app", HomeKeyService.getActionType("vn.mytvnet.b2cott"));
    }
}
