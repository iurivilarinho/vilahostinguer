package com.bancada.records;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyValueOutputTest {

    @Test
    void parsesTypedValuesAndKeepsEqualsSignsInValues() {
        KeyValueOutput output = KeyValueOutput.parse("cpuCores=4\nmemoryTotalBytes=2842804224\ncpuPercent=12,5\n"
            + "model=Samsung Galaxy J4+ (SM-J415G)\nversion=a=b\nroot=1\n");

        assertEquals(4, output.intValue("cpuCores"));
        assertEquals(2_842_804_224L, output.longValue("memoryTotalBytes"));
        assertEquals(12.5, output.doubleValue("cpuPercent"));
        assertEquals("Samsung Galaxy J4+ (SM-J415G)", output.text("model"));
        assertEquals("a=b", output.text("version"));
        assertTrue(output.flag("root"));
    }

    @Test
    void ignoresBlankValuesAndNoise() {
        KeyValueOutput output = KeyValueOutput.parse("osName=\nlastlog: warning\n=orphan\nbatteryPercent=abc\n");

        assertNull(output.text("osName"));
        assertNull(output.intValue("batteryPercent"));
        assertFalse(output.flag("root"));
    }
}
