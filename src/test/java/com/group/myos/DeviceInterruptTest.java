package com.group.myos;

import com.group.myos.device.manager.DeviceManager;
import com.group.myos.device.model.DeviceStatus;
import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.timer.TimerManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DeviceInterruptTest {
    @Autowired
    private DeviceManager deviceManager;
    @Autowired
    private InterruptManager interruptManager;
    @Autowired
    private TimerManager timerManager;

    @Test
    public void testDeviceAllocationAndRelease() throws InterruptedException {
        boolean allocated = deviceManager.allocateDevice("A1", 1L); // 更新为两个参数
        assertEquals(true, allocated, "设备 A1 应该分配给进程 1");
        assertEquals(DeviceStatus.BUSY, deviceManager.getDeviceStatus("A1"), "设备 A1 状态应为 BUSY");

        boolean allocated2 = deviceManager.allocateDevice("A1", 2L); // 更新为两个参数
        assertEquals(false, allocated2, "设备 A1 已占用，进程 2 应加入等待队列");

        deviceManager.simulateDeviceInterrupt("A1");
        Thread.sleep(200);
        assertEquals(DeviceStatus.BUSY, deviceManager.getDeviceStatus("A1"), "设备 A1 应分配给进程 2");
    }

    @Test
    public void testInterruptHandling() throws InterruptedException {
        interruptManager.triggerInterrupt(1, com.group.myos.interrupt.model.InterruptType.IO, 1L, "A1");
        Thread.sleep(100);
        interruptManager.setInterruptEnabled(false);
        interruptManager.triggerInterrupt(1, com.group.myos.interrupt.model.InterruptType.IO, 1L, "A1");
        Thread.sleep(100);
    }

    @Test
    public void testDeviceTimeout() throws InterruptedException {
        deviceManager.allocateDevice("A1", 1L); // 更新为两个参数
        Thread.sleep(1200);
        assertEquals(DeviceStatus.ERROR, deviceManager.getDeviceStatus("A1"), "设备 A1 应因超时变为 ERROR");
    }
}