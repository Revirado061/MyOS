package com.group.myos.device.model;

import lombok.Data;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 设备模型，定义设备的基本属性和状态。
 */
@Data
public class Device {
    private Long id;                    // 设备ID
    private String name;                // 设备名称
    private DeviceType type;           // 设备类型
    private DeviceStatus status;        // 设备状态
    private Long currentProcessId;      // 当前占用进程ID
    private Integer remainingTime;      // 任务剩余时间（秒）
    private List<Long> waitQueue;       // 等待进程ID列表
    private Long version;               // 乐观锁版本

    // 构造函数
    public Device(Long id, String name, DeviceType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = DeviceStatus.IDLE;
        this.currentProcessId = null;
        this.remainingTime = 0;
        this.waitQueue = new CopyOnWriteArrayList<>();
        this.version = 0L;
    }
}