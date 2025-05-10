package com.group.myos.timer;

import com.group.myos.device.manager.DeviceManager;
import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.InterruptType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 简化的时钟管理器，仅用于触发时钟中断以支持测试。
 */
@Component
public class TimerManager {
    private volatile long systemTime = 0; // 系统时间
    private static final long TIME_SLICE = 100; // 时间片，100ms
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @Autowired
    private InterruptManager interruptManager;
    @Autowired
    private DeviceManager deviceManager;

    /**
     * 启动时钟，定时触发时钟中断。
     */
    @PostConstruct
    public void startSystemClock() {
        scheduler.scheduleAtFixedRate(() -> {
            systemTime++; // 系统时间递增
            // 触发时钟中断（0号中断）
            interruptManager.triggerInterrupt(0, InterruptType.TIMER, null, null);
            // 检查设备超时
            deviceManager.checkTimeouts(systemTime);
        }, 0, TIME_SLICE, TimeUnit.MILLISECONDS);
        System.out.println("时钟启动，时间片 " + TIME_SLICE + "ms");
    }

    /**
     * 停止时钟。
     */
    @PreDestroy
    public void stopSystemClock() {
        scheduler.shutdown();
        System.out.println("时钟停止");
    }

    /**
     * 获取当前系统时间。
     * @return 系统时间
     */
    public long getSystemTime() {
        return systemTime;
    }
}