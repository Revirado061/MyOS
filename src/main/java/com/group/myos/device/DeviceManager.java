package com.group.myos.device;

import com.group.myos.process.model.Process;
import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DeviceManager {
    private final Map<String, Device> devices;
    private final Map<Process, Set<Device>> processDevices;
    
    public DeviceManager() {
        this.devices = new HashMap<>();
        this.processDevices = new HashMap<>();
        initializeDevices();
    }
    
    private void initializeDevices() {
        // 初始化一些基本设备
        devices.put("printer1", new Device("printer1", DeviceType.PRINTER));
        devices.put("disk1", new Device("disk1", DeviceType.DISK));
        devices.put("keyboard", new Device("keyboard", DeviceType.KEYBOARD));
        devices.put("display", new Device("display", DeviceType.DISPLAY));
    }
    
    public boolean requestDevice(Process process, String deviceName) {
        Device device = devices.get(deviceName);
        if (device == null) {
            return false;
        }
        
        if (device.isAvailable()) {
            device.setCurrentProcess(process);
            processDevices.computeIfAbsent(process, k -> new HashSet<>()).add(device);
            return true;
        }
        
        return false;
    }
    
    public void releaseDevice(Process process, String deviceName) {
        Device device = devices.get(deviceName);
        if (device != null && process.equals(device.getCurrentProcess())) {
            device.setCurrentProcess(null);
            Set<Device> processDevices = this.processDevices.get(process);
            if (processDevices != null) {
                processDevices.remove(device);
            }
        }
    }
    
    public List<Device> getAvailableDevices() {
        return devices.values().stream()
            .filter(Device::isAvailable)
            .toList();
    }
    
    public List<Device> getProcessDevices(Process process) {
        Set<Device> devices = processDevices.get(process);
        return devices != null ? new ArrayList<>(devices) : new ArrayList<>();
    }
    
    @Data
    public static class Device {
        private final String name;
        private final DeviceType type;
        private Process currentProcess;
        
        public Device(String name, DeviceType type) {
            this.name = name;
            this.type = type;
        }
        
        public boolean isAvailable() {
            return currentProcess == null;
        }
    }
    
    public enum DeviceType {
        PRINTER,
        DISK,
        KEYBOARD,
        DISPLAY
    }
} 