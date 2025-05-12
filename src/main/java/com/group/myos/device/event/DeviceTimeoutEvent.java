package com.group.myos.device.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 设备超时事件
 * 用于通知设备管理器检查设备超时
 */
@Getter
public class DeviceTimeoutEvent extends ApplicationEvent {
    private final long currentTime;

    public DeviceTimeoutEvent(Object source, long currentTime) {
        super(source);
        this.currentTime = currentTime;
    }
} 