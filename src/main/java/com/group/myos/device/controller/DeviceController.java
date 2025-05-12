package com.group.myos.device.controller;

import com.group.myos.device.manager.DeviceManager;
import com.group.myos.device.model.Device;
import com.group.myos.device.model.DeviceStatus;
import com.group.myos.device.model.DeviceType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceManager deviceManager;

    public DeviceController(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDevices() {
        List<Device> devices = deviceManager.getAllDevices();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "设备列表已获取",
            "data", devices
        ));
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<Map<String, Object>> getDevice(@PathVariable Long deviceId) {
        Device device = deviceManager.getDevice(deviceId);
        if (device == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "设备信息已获取",
            "data", device
        ));
    }

    @PostMapping("/{deviceId}/request")
    public ResponseEntity<Map<String, Object>> requestDevice(
            @PathVariable Long deviceId,
            @RequestBody Map<String, Object> request) {
        Long processId = Long.valueOf(request.get("processId").toString());
        Integer taskDuration = Integer.valueOf(request.get("taskDuration").toString());
        
        boolean allocated = deviceManager.allocateDevice(deviceId, processId, taskDuration);
        Device device = deviceManager.getDevice(deviceId);
        
        if (allocated) {
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "设备已分配给进程 " + processId,
                "data", device
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "进程 " + processId + " 已加入等待队列",
                "data", device
            ));
        }
    }

    @PostMapping("/{deviceId}/release")
    public ResponseEntity<Map<String, Object>> releaseDevice(@PathVariable Long deviceId) {
        Device device = deviceManager.releaseDevice(deviceId);
        if (device == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "无效的设备ID"
            ));
        }
        
        String message = device.getCurrentProcessId() != null ?
            "设备已释放，分配给进程 " + device.getCurrentProcessId() :
            "设备已释放，无等待进程";
            
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", message,
            "data", device
        ));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Map<String, Object>> getDevicesByType(@PathVariable DeviceType type) {
        List<Device> devices = deviceManager.getAllDevices().stream()
            .filter(device -> device.getType() == type)
            .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "设备列表已获取",
            "data", devices
        ));
    }

    @PostMapping("/{deviceId}/interrupt")
    public ResponseEntity<Map<String, Object>> simulateDeviceInterrupt(@PathVariable Long deviceId) {
        deviceManager.simulateDeviceInterrupt(deviceId);
        Device device = deviceManager.getDevice(deviceId);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "设备中断已触发",
            "data", device
        ));
    }

    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> getAvailableDevices() {
        List<Device> devices = deviceManager.getAllDevices().stream()
            .filter(device -> device.getStatus() == DeviceStatus.IDLE)
            .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "可用设备列表已获取",
            "data", devices
        ));
    }
}