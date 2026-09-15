package com.friendinneed.smarthome;
import java.util.Map;
public record DeviceState(String entityId, String friendlyName, String state, Map<String, Object> attributes) { }
