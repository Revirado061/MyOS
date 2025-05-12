package com.group.myos.timer;

import com.group.myos.device.event.DeviceTimeoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 系统时钟管理器
 * 负责系统时钟的启动、停止和定时任务调度
 */
@Slf4j
@Component
public class TimerManager {
    private volatile boolean running = false;
    private long currentTime = 0;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
    @Scheduled(fixedRate = 1000)
    public void tick() {
        if (running) {
            currentTime++;
            log.debug("系统时钟: {}", currentTime);
            
            // 发布设备超时检查事件
            eventPublisher.publishEvent(new DeviceTimeoutEvent(this, currentTime));
        }
    }
}