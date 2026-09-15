package com.friendinneed.smarthome;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockSmartHomeAdapterTest {
    @Test
    void exposesDevicesAndUpdatesStateForLoggedCommand() {
        MockSmartHomeAdapter home = new MockSmartHomeAdapter();
        assertEquals(4, home.listDevices().size());
        home.sendCommand("light.living_room", "turn_off", Map.of());
        assertEquals("off", home.getDevice("light.living_room").state());
    }
}
