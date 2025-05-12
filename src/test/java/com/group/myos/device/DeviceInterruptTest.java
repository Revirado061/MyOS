package com.group.myos.device;

import com.group.myos.device.manager.DeviceManager;
import com.group.myos.device.model.Device;
import com.group.myos.device.model.DeviceStatus;
import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.InterruptType;
import com.group.myos.timer.TimerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DeviceInterruptTest {
    @Autowired
    private DeviceManager deviceManager;
    @Autowired
    private InterruptManager interruptManager;
    @Autowired
    private TimerManager timerManager;

    @BeforeEach
    public void setUp() {
        deviceManager.reset();
    }

    @Test
    public void testDeviceAllocationAndRelease() throws InterruptedException {
        // 分配设备给进程1
        boolean allocated = deviceManager.allocateDevice(1L, 1L, 5);
        assertTrue(allocated, "设备 1 应该分配给进程 1");
        
        Device device = deviceManager.getDevice(1L);
        assertEquals(DeviceStatus.BUSY, device.getStatus(), "设备 1 状态应为 BUSY");
        assertEquals(1L, device.getCurrentProcessId(), "设备 1 应被进程 1 占用");

        // 尝试分配设备给进程2
        boolean allocated2 = deviceManager.allocateDevice(1L, 2L, 5);
        assertFalse(allocated2, "设备 1 已占用，进程 2 应加入等待队列");
        assertTrue(device.getWaitQueue().contains(2L), "进程 2 应在等待队列中");

        // 模拟设备中断
        deviceManager.simulateDeviceInterrupt(1L);
        Thread.sleep(500);
        
        // 释放设备
        deviceManager.releaseDevice(1L);
        Thread.sleep(500);
        device = deviceManager.getDevice(1L);
        assertEquals(DeviceStatus.BUSY, device.getStatus(), "设备 1 应分配给进程 2");
        assertEquals(2L, device.getCurrentProcessId(), "设备 1 应被进程 2 占用");
    }

    @Test
    public void testInterruptHandling() throws InterruptedException {
        // 触发设备中断
        interruptManager.triggerInterrupt(1, InterruptType.DEVICE, 1L, "设备1");
        Thread.sleep(200);
        
        // 触发I/O中断
        interruptManager.triggerInterrupt(2, InterruptType.IO, 1L, "文件1");
        Thread.sleep(200);
    }

    @Test
    public void testDeviceTimeout() {
        // 启动系统时钟
        timerManager.start();
        
        // 分配设备，设置 1 秒超时
        boolean allocated = deviceManager.allocateDevice(1L, 1L, 1);
        assertTrue(allocated, "设备 1 应该分配给进程 1");
        
        // 验证初始状态
        Device device = deviceManager.getDevice(1L);
        assertEquals(DeviceStatus.BUSY, device.getStatus(), "设备 1 应为 BUSY");
        assertEquals(1, device.getRemainingTime(), "设备 1 任务时间应为 1 秒");
        
        // 模拟时间推进
        timerManager.tick(); // 减少 remainingTime 到 0
        timerManager.tick(); // 触发超时检查
        
        // 验证超时状态
        device = deviceManager.getDevice(1L);
        assertEquals(DeviceStatus.ERROR, device.getStatus(), "设备 1 应因超时变为 ERROR");
        assertNull(device.getCurrentProcessId(), "设备 1 应无占用进程");
        
        // 停止系统时钟
        timerManager.stop();
    }
}