package com.group.myos.device.event;

import lombok.Getter;

/**
 * 设备超时事件
 */
@Getter
public class DeviceTimeoutEvent {
    private final long currentTime;
    private final Long deviceId;
    private final String message;

    public DeviceTimeoutEvent(long currentTime, Long deviceId, String message) {
        this.currentTime = currentTime;
        this.deviceId = deviceId;
        this.message = message;
    }
} 