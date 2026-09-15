package com.friendinneed.smarthome;
import java.util.*; import org.slf4j.*; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Service;
@Service @ConditionalOnProperty(name="smarthome.type", havingValue="mock", matchIfMissing=true)
public class MockSmartHomeAdapter implements SmartHomeAdapter {
 private static final Logger log=LoggerFactory.getLogger(MockSmartHomeAdapter.class); private final Map<String,DeviceState> devices=new LinkedHashMap<>();
 public MockSmartHomeAdapter(){ put("light.living_room","Living room light","on",Map.of("brightness",80));put("climate.thermostat","Thermostat","24",Map.of("unit_of_measurement","°C","temperature",24));put("lock.front_door","Front door lock","locked",Map.of());put("media_player.bedroom_speaker","Bedroom speaker","idle",Map.of()); }
 private void put(String id,String name,String state,Map<String,Object>a){devices.put(id,new DeviceState(id,name,state,a));}
 public List<DeviceState> listDevices(){return List.copyOf(devices.values());} public DeviceState getDevice(String id){DeviceState d=devices.get(id);if(d==null)throw new NoSuchElementException("Device not found: "+id);return d;}
 public void sendCommand(String id,String command,Map<String,Object> params){DeviceState d=getDevice(id);log.info("Mock smart-home command: entityId={}, command={}, params={}",id,command,params);String state=command.equals("turn_on")?"on":command.equals("turn_off")?"off":command.equals("lock")?"locked":command.equals("unlock")?"unlocked":d.state();devices.put(id,new DeviceState(id,d.friendlyName(),state,d.attributes()));}
 public Map<String,Object> getSensorData(String id){DeviceState d=getDevice(id);return new LinkedHashMap<>(d.attributes());}
}
