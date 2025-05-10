package com.group.myos.config;

import com.group.myos.device.manager.DeviceManager;
import com.group.myos.interrupt.handler.InterruptHandler;
import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.InterruptType;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Spring Boot 配置类，注册 Bean 并初始化中断处理程序。
 */
@Configuration
public class AppConfig {
    private final InterruptManager interruptManager;
    private final DeviceManager deviceManager;

    public AppConfig(InterruptManager interruptManager, DeviceManager deviceManager) {
        this.interruptManager = interruptManager;
        this.deviceManager = deviceManager;
    }

    /**
     * 初始化中断处理程序。
     */
    @PostConstruct
    public void initializeInterrupts() {
        // 注册时钟中断处理程序（0号中断）
        interruptManager.registerInterrupt(0, new InterruptHandler() {
            @Override
            public void handleInterrupt(InterruptType type, Long processId, Object data) {
                System.out.println("处理时钟中断，系统时间更新");
            }
        }, 1);

        // 注册I/O中断处理程序（1号中断）
        interruptManager.registerInterrupt(1, new InterruptHandler() {
            @Override
            public void handleInterrupt(InterruptType type, Long processId, Object data) {
                String deviceCode = (String) data;
                System.out.println("处理I/O中断，设备 " + deviceCode + "，进程 " + processId);
                // 释放设备
                deviceManager.releaseDevice(deviceCode, processId);
            }
        }, 2);

        // 注册错误中断处理程序（3号中断）
        interruptManager.registerInterrupt(3, new InterruptHandler() {
            @Override
            public void handleInterrupt(InterruptType type, Long processId, Object data) {
                String deviceCode = (String) data;
                System.out.println("处理错误中断，设备 " + deviceCode + " 超时，进程 " + processId);
            }
        }, 0);
    }
}