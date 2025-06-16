package com.group.myos.timer;

import com.group.myos.device.event.DeviceTimeoutEvent;
import com.group.myos.interrupt.model.InterruptType;
import com.group.myos.interrupt.event.ClockInterruptEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 系统时钟管理器
 * 负责系统时钟的启动、停止和定时任务调度
 * 同时负责触发时钟中断
 */
@Slf4j
@Component
public class TimerManager {
    private static final int CLOCK_INTERRUPT_INTERVAL = 1; // 时钟中断间隔（秒）
    private long currentTime = 0;
    private boolean isRunning = false;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @PostConstruct
    public void init() {
        start();
    }

    /**
     * 启动系统时钟
     */
    public void start() {
        if (!isRunning) {
            isRunning = true;
            log.info("系统时钟启动");
        }
    }

    /**
     * 停止系统时钟
     */
    public void stop() {
        if (isRunning) {
            isRunning = false;
            log.info("系统时钟停止");
        }
    }

    /**
     * 获取当前系统时间
     */
    public long getCurrentTime() {
        return currentTime;
    }

    /**
     * 系统时钟滴答
     * 每秒执行一次
     */
    @Scheduled(fixedRate = 1000) // 每秒执行一次
    public void tick() {
        if (!isRunning) {
            return;
        }

        currentTime++;
        
        // 发布设备超时检查事件
        eventPublisher.publishEvent(new DeviceTimeoutEvent(currentTime, null, "设备超时检查"));
        
        // 每CLOCK_INTERRUPT_INTERVAL秒触发一次时钟中断
        if (currentTime % CLOCK_INTERRUPT_INTERVAL == 0) {
            triggerClockInterrupt();
        }
    }

    /**
     * 触发时钟中断
     */
    private void triggerClockInterrupt() {
        // 发布时钟中断事件
        eventPublisher.publishEvent(new ClockInterruptEvent(this, currentTime));
    }
}