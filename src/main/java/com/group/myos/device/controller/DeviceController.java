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
import java.util.HashMap;

/**
 * 设备控制器
 * 提供设备管理的REST API接口，包括设备的查询、分配、释放和中断处理等功能
 */
@RestController
@RequestMapping("/device")
public class DeviceController {
    private final DeviceManager deviceManager;

    /**
     * 构造函数
     * @param deviceManager 设备管理器实例，用于处理具体的设备管理逻辑
     */
    public DeviceController(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    /**
     * 获取所有设备列表
     * 功能：返回系统中所有设备的详细信息
     * @return ResponseEntity 包含设备列表的响应，格式为：
     *         {
     *             "status": "success",
     *             "message": "设备列表已获取",
     *             "data": [设备列表]
     *         }
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDevices() {
        List<Device> devices = deviceManager.getAllDevices();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "设备列表已获取",
            "data", devices
        ));
    }

    /**
     * 获取指定设备信息
     * 功能：根据设备ID获取单个设备的详细信息
     * @param deviceId 设备ID，指定要查询的设备
     * @return ResponseEntity 包含设备信息的响应，如果设备不存在则返回404
     */
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

    /**
     * 请求分配设备
     * 功能：为指定进程请求分配设备，如果设备忙则将进程加入等待队列
     * @param request 请求体，包含：
     *               - deviceId: 设备ID
     *               - processId: 进程ID
     *               - timeout: 超时时间（秒）
     * @return ResponseEntity 包含分配结果的响应，包括设备当前状态
     */
    @PostMapping("/allocate")
    public ResponseEntity<Map<String, Object>> requestDevice(@RequestBody Map<String, Object> request) {
        // 验证请求参数
        if (request == null || !request.containsKey("deviceId") || !request.containsKey("processId") || !request.containsKey("timeout")) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "请求参数不完整，需要 deviceId, processId 和 timeout"
            ));
        }

        try {
            Long deviceId = Long.valueOf(request.get("deviceId").toString());
            Long processId = Long.valueOf(request.get("processId").toString());
            Integer timeout = Integer.valueOf(request.get("timeout").toString());
            
            // 验证超时时间
            if (timeout <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "超时时间必须大于0"
                ));
            }
            
            Device device = deviceManager.getDevice(deviceId);
            if (device == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "设备不存在: " + deviceId
                ));
            }

            boolean allocated = deviceManager.allocateDevice(deviceId, processId, timeout);
            Device updatedDevice = deviceManager.getDevice(deviceId);
            
            if (allocated) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "设备已分配给进程 " + processId,
                    "data", updatedDevice
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "进程 " + processId + " 已加入等待队列",
                    "data", updatedDevice
                ));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "设备ID、进程ID和超时时间必须是数字"
            ));
        }
    }

    /**
     * 释放设备
     * 功能：释放指定设备，如果等待队列不为空则自动分配给下一个进程
     * @param request 请求体，包含：
     *               - deviceId: 设备ID
     * @return ResponseEntity 包含释放结果的响应，如果设备不存在则返回400错误
     */
    @PostMapping("/release")
    public ResponseEntity<Map<String, Object>> releaseDevice(@RequestBody Map<String, Object> request) {
        // 验证请求参数
        if (request == null || !request.containsKey("deviceId")) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "请求参数不完整，需要 deviceId"
            ));
        }

        try {
            Long deviceId = Long.valueOf(request.get("deviceId").toString());
            Device device = deviceManager.getDevice(deviceId);
            
            if (device == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "设备不存在: " + deviceId
                ));
            }
            
            Device releasedDevice = deviceManager.releaseDevice(deviceId);
            if (releasedDevice == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "释放设备失败"
                ));
            }
            
            String message = releasedDevice.getCurrentProcessId() != null ?
                "设备已释放，分配给进程 " + releasedDevice.getCurrentProcessId() :
                "设备已释放，无等待进程";
                
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", message,
                "data", releasedDevice
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "设备ID必须是数字"
            ));
        }
    }

    /**
     * 按类型获取设备列表
     * 功能：获取指定类型的所有设备列表
     * @param type 设备类型（如打印机、磁盘等）
     * @return ResponseEntity 包含指定类型设备列表的响应
     */
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



    /**
     * 获取可用设备列表
     * 功能：获取当前所有空闲状态的设备列表
     * @return ResponseEntity 包含可用设备列表的响应
     */
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

    /**
     * 获取设备状态
     * 功能：获取指定设备的当前状态信息
     * @param deviceId 设备ID，通过查询参数传递
     * @return ResponseEntity 包含设备状态信息的响应
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDeviceStatus(@RequestParam Long deviceId) {
        Device device = deviceManager.getDevice(deviceId);
        if (device == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "设备不存在: " + deviceId
            ));
        }

        Map<String, Object> deviceStatus = new HashMap<>();
        deviceStatus.put("deviceId", device.getId());
        deviceStatus.put("name", device.getName());
        deviceStatus.put("type", device.getType());
        deviceStatus.put("status", device.getStatus());
        deviceStatus.put("currentProcessId", device.getCurrentProcessId());
        deviceStatus.put("remainingTime", device.getRemainingTime());
        deviceStatus.put("waitQueueSize", device.getWaitQueue() != null ? device.getWaitQueue().size() : 0);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "设备状态已获取",
            "data", deviceStatus
        ));
    }
}