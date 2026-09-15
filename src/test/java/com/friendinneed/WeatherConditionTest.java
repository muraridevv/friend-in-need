package com.friendinneed;

import com.friendinneed.integration.WeatherService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherConditionTest {
    @Test
    void testWmoCodeRanges() {
        assertEquals("clear", WeatherService.condition(0));
        for (int code : new int[]{1, 2, 3}) assertEquals("partly cloudy", WeatherService.condition(code));
        for (int code : new int[]{45, 48}) assertEquals("foggy", WeatherService.condition(code));
        for (int code : new int[]{51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82})
            assertEquals("rainy", WeatherService.condition(code));
        for (int code : new int[]{71, 73, 75, 77, 85, 86}) assertEquals("snowy", WeatherService.condition(code));
        for (int code : new int[]{95, 96, 99}) assertEquals("stormy", WeatherService.condition(code));
        assertEquals("mixed conditions", WeatherService.condition(4));
    }
}
