package com.group.myos.device.manager;

import com.group.myos.device.model.Device;
import com.group.myos.device.model.DeviceStatus;
import com.group.myos.device.model.DeviceType;
import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.InterruptType;
import com.group.myos.timer.TimerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Component
public class DeviceManager {
    private final Map<String, Device> devices = new ConcurrentHashMap<>();
    private final Map<String, Queue<Long>> waitingQueues = new ConcurrentHashMap<>();
    private final Map<String, Runnable> interruptHandlers = new ConcurrentHashMap<>();
    @Autowired
    private InterruptManager interruptManager;
    @Autowired
    @Lazy
    private TimerManager timerManager;

    public DeviceManager() {
        devices.put("A1", new Device(1L, "A1", DeviceType.PRINTER));
        devices.put("A2", new Device(2L, "A2", DeviceType.DISK));
        waitingQueues.put("A1", new ConcurrentLinkedQueue<>());
        waitingQueues.put("A2", new ConcurrentLinkedQueue<>());
    }

    public boolean allocateDevice(String deviceCode, Long processId) {
        Device device = devices.get(deviceCode);
        if (device == null || !device.getIsAvailable()) {
            return false;
        }

        synchronized (device) {
            if (device.getStatus() == DeviceStatus.IDLE) {
                device.setStatus(DeviceStatus.BUSY);
                device.setOccupiedByProcess(processId);
                device.setAllocatedTime(timerManager.getSystemTime());
                device.setVersion(device.getVersion() + 1);
                System.out.println("设备 " + deviceCode + " 分配给进程 " + processId);
                return true;
            } else {
                waitingQueues.get(deviceCode).offer(processId);
                System.out.println("进程 " + processId + " 加入设备 " + deviceCode + " 等待队列");
                return false;
            }
        }
    }

    public void releaseDevice(String deviceCode, Long processId) {
        Device device = devices.get(deviceCode);
        if (device == null) return;

        synchronized (device) {
            if (device.getOccupiedByProcess() != null && device.getOccupiedByProcess().equals(processId)) {
                device.setStatus(DeviceStatus.IDLE);
                device.setOccupiedByProcess(null);
                device.setAllocatedTime(null);
                device.setVersion(device.getVersion() + 1);
                System.out.println("设备 " + deviceCode + " 被进程 " + processId + " 释放");

                Queue<Long> queue = waitingQueues.get(deviceCode);
                Long nextProcessId = queue.poll();
                if (nextProcessId != null) {
                    allocateDevice(deviceCode, nextProcessId);
                }
            }
        }
    }

    public void simulateDeviceInterrupt(String deviceCode) {
        Device device = devices.get(deviceCode);
        if (device == null) return;

        synchronized (device) {
            if (device.getStatus() == DeviceStatus.BUSY) {
                interruptManager.triggerInterrupt(1, InterruptType.IO, device.getOccupiedByProcess(), deviceCode);
                System.out.println("设备 " + deviceCode + " 完成I/O操作，触发中断");
            }
        }
    }

    public DeviceStatus getDeviceStatus(String deviceCode) {
        Device device = devices.get(deviceCode);
        return device != null ? device.getStatus() : null;
    }

    public List<Device> getAllDevices() {
        return devices.values().stream().collect(Collectors.toList());
    }

    public void checkTimeouts(long currentTime) {
        devices.values().forEach(device -> {
            synchronized (device) {
                if (device.getStatus() == DeviceStatus.BUSY && device.getAllocatedTime() != null) {
                    long diff = currentTime - device.getAllocatedTime();
                    System.out.println("设备 " + device.getDeviceCode() + " 时间差=" + diff);
                    if (diff > 10) {
                        device.setStatus(DeviceStatus.ERROR);
                        System.out.println("设备 " + device.getDeviceCode() + " 超时，状态变为 ERROR");
                        interruptManager.triggerInterrupt(3, InterruptType.ERROR, device.getOccupiedByProcess(), device.getDeviceCode());
                    }
                }
            }
        });
    }
}