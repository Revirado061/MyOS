package com.group.myos.device.manager;

import com.group.myos.device.event.DeviceTimeoutEvent;
import com.group.myos.device.model.Device;
import com.group.myos.device.model.DeviceStatus;
import com.group.myos.device.model.DeviceType;
import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.InterruptType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 设备管理器
 * 负责管理设备的分配和可用性
 */
@Slf4j
@Component
public class DeviceManager {
    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
    
    // 设备类型及其可用数量
    private final Map<String, Integer> devicePool = new ConcurrentHashMap<>();
    
    // 设备分配记录：设备类型 -> 进程ID
    private final Map<String, Long> deviceAllocations = new ConcurrentHashMap<>();
    
    // 设备映射：设备ID -> 设备对象
    private final Map<Long, Device> devices = new ConcurrentHashMap<>();
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    private static final int DEFAULT_TASK_DURATION = 10; // 默认任务时间10秒

    /**
     * 构造函数
     * 功能：初始化系统中的所有设备，包括打印机、磁盘、键盘、鼠标和USB设备
     * 每个设备初始状态设置为空闲(IDLE)，无当前进程，剩余时间为0
     */
    public DeviceManager() {
        // 初始化设备时确保状态为IDLE
        // 打印机1
        Device printer1 = new Device(1L, "打印机1", DeviceType.PRINTER);
        printer1.setStatus(DeviceStatus.IDLE);
        printer1.setCurrentProcessId(null);
        printer1.setRemainingTime(0);
        devices.put(1L, printer1);
        
        // 打印机2
        Device printer2 = new Device(2L, "打印机2", DeviceType.PRINTER);
        printer2.setStatus(DeviceStatus.IDLE);
        printer2.setCurrentProcessId(null);
        printer2.setRemainingTime(0);
        devices.put(2L, printer2);
        
        // 磁盘1
        Device disk1 = new Device(3L, "磁盘1", DeviceType.DISK);
        disk1.setStatus(DeviceStatus.IDLE);
        disk1.setCurrentProcessId(null);
        disk1.setRemainingTime(0);
        devices.put(3L, disk1);

        // 磁盘2
        Device disk2 = new Device(4L, "磁盘2", DeviceType.DISK);
        disk2.setStatus(DeviceStatus.IDLE);
        disk2.setCurrentProcessId(null);
        disk2.setRemainingTime(0);
        devices.put(4L, disk2);

        // 键盘
        Device keyboard = new Device(5L, "键盘", DeviceType.KEYBOARD);
        keyboard.setStatus(DeviceStatus.IDLE);
        keyboard.setCurrentProcessId(null);
        keyboard.setRemainingTime(0);
        devices.put(5L, keyboard);

        // 鼠标
        Device mouse = new Device(6L, "鼠标", DeviceType.MOUSE);
        mouse.setStatus(DeviceStatus.IDLE);
        mouse.setCurrentProcessId(null);
        mouse.setRemainingTime(0);
        devices.put(6L, mouse);

        // USB设备1
        Device usb1 = new Device(7L, "USB设备1", DeviceType.USB);
        usb1.setStatus(DeviceStatus.IDLE);
        usb1.setCurrentProcessId(null);
        usb1.setRemainingTime(0);
        devices.put(7L, usb1);

        // USB设备2
        Device usb2 = new Device(8L, "USB设备2", DeviceType.USB);
        usb2.setStatus(DeviceStatus.IDLE);
        usb2.setCurrentProcessId(null);
        usb2.setRemainingTime(0);
        devices.put(8L, usb2);

        log.info("设备管理器初始化完成");

        // 初始化设备池
        devicePool.put("PRINTER", 2);    // 2台打印机
        devicePool.put("SCANNER", 1);    // 1台扫描仪
        devicePool.put("DISK", 3);       // 3个磁盘
        devicePool.put("NETWORK", 5);    // 5个网络接口
    }

    /**
     * 监听设备超时事件
     * 功能：处理设备超时事件，检查所有设备的使用时间
     * @param event 设备超时事件，包含当前系统时间信息
     */
    @EventListener  // 事件监听者（DeviceManager）
    public void handleDeviceTimeoutEvent(DeviceTimeoutEvent event) {
        checkTimeouts(event.getCurrentTime());
    }

    /**
     * 检查设备超时
     * 功能：检查所有设备的使用时间，如果设备使用时间到期则自动释放
     * 如果设备有等待队列，则自动分配给下一个等待的进程
     * @param currentTime 当前系统时间，用于计算设备剩余使用时间
     */
    private void checkTimeouts(long currentTime) {
        devices.values().forEach(device -> {
            synchronized (device) {
                if (device.getStatus() == DeviceStatus.BUSY && device.getRemainingTime() > 0) {
                    device.setRemainingTime(device.getRemainingTime() - 1);
                    // log.info("设备 {} 剩余时间: {}", device.getId(), device.getRemainingTime());
                    
                    if (device.getRemainingTime() <= 0) {
                        log.info("设备 {} 使用时间到，自动释放", device.getId());
                        // 自动释放设备，状态设置为 ERROR
                        device.setStatus(DeviceStatus.ERROR);
                        device.setCurrentProcessId(null);
                        device.setRemainingTime(0);
                        
                        // 如果有等待队列，分配给下一个进程
                        if (!device.getWaitQueue().isEmpty()) {
                            Long nextProcessId = device.getWaitQueue().remove(0);
                            log.info("从等待队列中取出进程 {} 分配设备 {}", nextProcessId, device.getId());
                            allocateDevice(device.getId(), nextProcessId, DEFAULT_TASK_DURATION);
                        }
                        
                        // 触发设备释放中断
                        eventPublisher.publishEvent(
                            new DeviceTimeoutEvent(
                                currentTime,
                                device.getId(),
                                "设备 " + device.getId() + " 使用时间到，已自动释放"
                            )
                        );
                    }
                }
            }
        });
    }

    /**
     * 分配设备给指定进程（使用默认超时时间）
     * @param deviceId 设备ID
     * @param processId 进程ID
     * @return boolean 分配是否成功
     */
    public boolean allocateDevice(Long deviceId, Long processId) {
        return allocateDevice(deviceId, processId, DEFAULT_TASK_DURATION);
    }

    /**
     * 分配设备给指定进程
     * @param deviceId 设备ID
     * @param processId 进程ID
     * @param taskDuration 任务持续时间（秒）
     * @return boolean 分配是否成功
     */
    public boolean allocateDevice(Long deviceId, Long processId, Integer taskDuration) {
        Device device = devices.get(deviceId);
        if (device == null) {
            log.error("设备 {} 不存在", deviceId);
            return false;
        }

        log.info("尝试分配设备 {} 给进程 {}，当前状态: {}", deviceId, processId, device.getStatus());

        if (device.getStatus() == DeviceStatus.IDLE || device.getStatus() == DeviceStatus.ERROR) {
            device.setStatus(DeviceStatus.BUSY);
            device.setCurrentProcessId(processId);
            device.setRemainingTime(taskDuration);
            device.setVersion(device.getVersion() + 1);
            log.info("设备 {} 成功分配给进程 {}，状态: {}", deviceId, processId, device.getStatus());
            return true;
        } else {
            if (!device.getWaitQueue().contains(processId)) {
                device.getWaitQueue().add(processId);
                log.info("进程 {} 加入设备 {} 的等待队列", processId, deviceId);
            }
            return false;
        }
    }

    /**
     * 释放设备
     * 功能：释放指定设备，将其状态设置为空闲，并检查等待队列
     * 如果等待队列不为空，则自动分配给下一个等待的进程
     * @param deviceId 设备ID，指定要释放的设备
     * @return Device 被释放的设备对象，如果设备不存在则返回null
     */
    public Device releaseDevice(Long deviceId) {
        Device device = devices.get(deviceId);
        if (device == null) return null;

        synchronized (device) {
            log.info("释放设备 {}，当前状态: {}", deviceId, device.getStatus());
            device.setStatus(DeviceStatus.IDLE);
            device.setCurrentProcessId(null);
            device.setRemainingTime(0);
            device.setVersion(device.getVersion() + 1);
            log.info("设备 {} 被释放", deviceId);

            if (!device.getWaitQueue().isEmpty()) {
                Long nextProcessId = device.getWaitQueue().remove(0);
                log.info("从等待队列中取出进程 {} 分配设备 {}", nextProcessId, deviceId);
                allocateDevice(deviceId, nextProcessId, DEFAULT_TASK_DURATION);
            }
            return device;
        }
    }

    /**
     * 获取指定设备
     * 功能：根据设备ID获取设备对象
     * @param deviceId 设备ID，指定要获取的设备
     * @return Device 设备对象，如果不存在则返回null
     */
    public Device getDevice(Long deviceId) {
        return devices.get(deviceId);
    }

    /**
     * 获取所有设备列表
     * 功能：获取系统中所有设备的列表
     * @return List<Device> 所有设备的列表
     */
    public List<Device> getAllDevices() {
        return devices.values().stream().collect(Collectors.toList());
    }

    /**
     * 重置设备管理器
     * 功能：清空所有设备并重新初始化系统设备
     * 将所有设备恢复到初始状态（空闲、无当前进程、剩余时间为0）
     */
    public void reset() {
        devices.clear();
        // 打印机1
        Device printer1 = new Device(1L, "打印机1", DeviceType.PRINTER);
        printer1.setStatus(DeviceStatus.IDLE);
        printer1.setCurrentProcessId(null);
        printer1.setRemainingTime(0);
        devices.put(1L, printer1);
        
        // 打印机2
        Device printer2 = new Device(2L, "打印机2", DeviceType.PRINTER);
        printer2.setStatus(DeviceStatus.IDLE);
        printer2.setCurrentProcessId(null);
        printer2.setRemainingTime(0);
        devices.put(2L, printer2);
        
        // 磁盘1
        Device disk1 = new Device(3L, "磁盘1", DeviceType.DISK);
        disk1.setStatus(DeviceStatus.IDLE);
        disk1.setCurrentProcessId(null);
        disk1.setRemainingTime(0);
        devices.put(3L, disk1);

        // 磁盘2
        Device disk2 = new Device(4L, "磁盘2", DeviceType.DISK);
        disk2.setStatus(DeviceStatus.IDLE);
        disk2.setCurrentProcessId(null);
        disk2.setRemainingTime(0);
        devices.put(4L, disk2);

        // 键盘
        Device keyboard = new Device(5L, "键盘", DeviceType.KEYBOARD);
        keyboard.setStatus(DeviceStatus.IDLE);
        keyboard.setCurrentProcessId(null);
        keyboard.setRemainingTime(0);
        devices.put(5L, keyboard);

        // 鼠标
        Device mouse = new Device(6L, "鼠标", DeviceType.MOUSE);
        mouse.setStatus(DeviceStatus.IDLE);
        mouse.setCurrentProcessId(null);
        mouse.setRemainingTime(0);
        devices.put(6L, mouse);

        // USB设备
        Device usb = new Device(7L, "USB设备", DeviceType.USB);
        usb.setStatus(DeviceStatus.IDLE);
        usb.setCurrentProcessId(null);
        usb.setRemainingTime(0);
        devices.put(7L, usb);
        log.info("设备管理器重置完成");
    }

    /**
     * 根据设备类型分配设备
     * @param deviceType 设备类型
     * @param processId 进程ID
     * @return 分配的设备，如果分配失败返回null
     */
    public Device allocateDeviceByType(String deviceType, Long processId) {
        // 查找指定类型的空闲设备
        Device device = devices.values().stream()
            .filter(d -> d.getType().name().equals(deviceType) && d.getStatus() == DeviceStatus.IDLE)
            .findFirst()
            .orElse(null);
            
        if (device != null) {
            device.setStatus(DeviceStatus.BUSY);
            device.setCurrentProcessId(processId);
            device.setLastUpdateTime(LocalDateTime.now());
            return device;
        }
        
        return null;
    }

    /**
     * 根据设备类型释放设备
     * @param deviceType 设备类型
     * @param processId 进程ID
     * @return 是否成功释放
     */
    public boolean releaseDeviceByType(String deviceType, Long processId) {
        // 查找进程正在使用的指定类型的设备
        Device device = devices.values().stream()
            .filter(d -> d.getType().name().equals(deviceType) && d.getCurrentProcessId() == processId)
            .findFirst()
            .orElse(null);
            
        if (device != null) {
            device.setStatus(DeviceStatus.IDLE);
            device.setCurrentProcessId(null);
            device.setLastUpdateTime(LocalDateTime.now());
            return true;
        }
        
        return false;
    }

    /**
     * 检查设备是否可用
     * @param deviceType 设备类型
     * @return 设备是否可用
     */
    public boolean isDeviceAvailable(String deviceType) {
        Integer availableCount = devicePool.get(deviceType);
        if (availableCount == null) {
            logger.warn("未知设备类型: {}", deviceType);
            return false;
        }
        return availableCount > 0;
    }
    
    /**
     * 分配设备给进程
     * @param deviceType 设备类型
     * @param processId 进程ID
     * @return 是否分配成功
     */
    public boolean allocateDevice(String deviceType, Long processId) {
        if (!isDeviceAvailable(deviceType)) {
            logger.warn("设备不可用 - 类型: {}, 进程ID: {}", deviceType, processId);
            return false;
        }
        
        // 减少可用设备数量
        devicePool.compute(deviceType, (type, count) -> count - 1);
        
        // 记录设备分配
        deviceAllocations.put(deviceType + "_" + processId, processId);
        
        logger.info("设备分配成功 - 类型: {}, 进程ID: {}", deviceType, processId);
        return true;
    }
    
    /**
     * 释放设备
     * @param deviceType 设备类型
     * @param processId 进程ID
     */
    public void releaseDevice(String deviceType, Long processId) {
        String key = deviceType + "_" + processId;
        if (deviceAllocations.remove(key) != null) {
            // 增加可用设备数量
            devicePool.compute(deviceType, (type, count) -> count + 1);
            logger.info("设备释放成功 - 类型: {}, 进程ID: {}", deviceType, processId);
        }
    }
    
    /**
     * 获取设备可用数量
     * @param deviceType 设备类型
     * @return 可用数量
     */
    public int getAvailableDeviceCount(String deviceType) {
        return devicePool.getOrDefault(deviceType, 0);
    }
    
    /**
     * 获取所有设备状态
     * @return 设备状态映射
     */
    public Map<String, Integer> getAllDeviceStatus() {
        return new ConcurrentHashMap<>(devicePool);
    }
}