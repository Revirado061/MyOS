package com.group.myos.timer;

import com.group.myos.device.event.DeviceTimeoutEvent;
import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.InterruptType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 系统时钟管理器
 * 负责系统时钟的启动、停止和定时任务调度
 * 同时负责触发时钟中断
 */
@Slf4j
@Component
public class TimerManager {
    private volatile boolean running = false;
    private long currentTime = 0;
    private static final int CLOCK_INTERRUPT_INTERVAL = 10; // 每10个时钟周期触发一次时钟中断

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private InterruptManager interruptManager;

    /**
     * 启动系统时钟
     */
    public void start() {
        if (!running) {
            running = true;
            currentTime = 0;
            log.info("系统时钟已启动");
        }
    }

    /**
     * 停止系统时钟
     */
    public void stop() {
        running = false;
        log.info("系统时钟已停止");
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
    @Scheduled(fixedRate = 1000) //事件发布者（TimerManager）
    public void tick() {
        if (running) {
            currentTime++;
            
            // 发布设备超时检查事件
            eventPublisher.publishEvent(new DeviceTimeoutEvent(currentTime, null, "设备超时检查"));

            // 触发时钟中断
            if (currentTime % CLOCK_INTERRUPT_INTERVAL == 0) {
                log.info("系统时钟: {}", currentTime);
                triggerClockInterrupt();
            }
        }
    }

    /**
     * 触发时钟中断
     */
    private void triggerClockInterrupt() {
        log.info("触发时钟中断");
        interruptManager.triggerInterrupt(0, InterruptType.CLOCK, null, "系统时钟中断");
    }
}