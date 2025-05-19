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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 设备管理器
 * 负责设备的分配、释放、状态管理和中断处理
 */
@Slf4j
@Component
public class DeviceManager {
    private final Map<Long, Device> devices = new ConcurrentHashMap<>();
    @Autowired
    private InterruptManager interruptManager;
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
                        interruptManager.triggerInterrupt(
                            device.getId().intValue(),
                            InterruptType.DEVICE,
                            device.getId(),
                            "设备 " + device.getId() + " 使用时间到，已自动释放"
                        );
                    }
                }
            }
        });
    }

    /**
     * 分配设备给指定进程
     * 功能：将指定设备分配给请求的进程，如果设备忙则将进程加入等待队列
     * @param deviceId 设备ID，指定要分配的设备
     * @param processId 进程ID，指定要使用设备的进程
     * @param taskDuration 任务持续时间（秒），指定设备将被使用的时间
     * @return boolean 分配是否成功，true表示成功分配，false表示加入等待队列
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
}