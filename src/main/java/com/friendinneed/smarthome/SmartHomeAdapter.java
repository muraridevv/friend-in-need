package com.friendinneed.smarthome;
import java.util.List; import java.util.Map;
public interface SmartHomeAdapter { List<DeviceState> listDevices(); DeviceState getDevice(String entityId); void sendCommand(String entityId, String command, Map<String,Object> params); Map<String,Object> getSensorData(String entityId); }
