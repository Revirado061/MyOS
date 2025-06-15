package com.group.myos.device.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import jakarta.persistence.*;
import lombok.Data;

/**
 * 设备模型，定义设备的基本属性和状态。
 */
@Data
@Entity
@Table(name = "devices")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // 设备ID
    @Column(name = "name")
    private String name;                // 设备名称
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private DeviceType type;           // 设备类型
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeviceStatus status;        // 设备状态
    @Column(name = "current_process_id")
    private Long currentProcessId;      // 当前占用进程ID
    @Column(name = "remaining_time")
    private Integer remainingTime;      // 任务剩余时间（秒）
    @ElementCollection
    @CollectionTable(name = "device_wait_queue", joinColumns = @JoinColumn(name = "device_id"))
    @Column(name = "process_id")
    private List<Long> waitQueue;       // 等待进程ID列表
    @Version
    @Column(name = "version")
    private Long version;               // 乐观锁版本
    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;

    // 构造函数
    public Device() {
        this.waitQueue = new CopyOnWriteArrayList<>();
        this.status = DeviceStatus.IDLE;
        this.version = 0L;
    }

    public Device(Long id, String name, DeviceType type) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
    }

    // Getters and Setters
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}