package com.group.myos.device.controller;

import com.group.myos.device.manager.DeviceManager;
import com.group.myos.device.model.Device;
import com.group.myos.device.model.DeviceStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/device")
public class DeviceController {
    private final DeviceManager deviceManager;

    public DeviceController(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @GetMapping("/all")
    public List<Device> getAllDevices() {
        return deviceManager.getAllDevices();
    }

    @GetMapping("/status")
    public DeviceStatus getDeviceStatus(@RequestParam String deviceCode) {
        return deviceManager.getDeviceStatus(deviceCode);
    }

    @PostMapping("/allocate")
    public boolean allocateDevice(@RequestParam String deviceCode, @RequestParam Long processId) {
        return deviceManager.allocateDevice(deviceCode, processId);
    }

    @PostMapping("/release")
    public void releaseDevice(@RequestParam String deviceCode, @RequestParam Long processId) {
        deviceManager.releaseDevice(deviceCode, processId);
    }
}