package com.group.myos.device.model;

import java.time.LocalDateTime;

/**
 * 设备模型，定义设备的基本属性和状态。
 */
public class Device {
    private Long id;                    // 设备ID
    private String deviceCode;          // 设备编号，例如 A1, A2
    private DeviceType type;           // 设备类型
    private DeviceStatus status;       // 设备状态
    private Long occupiedByProcess;    // 当前占用设备的进程ID
    private Long allocatedTime; // 设备分配时间
    private Boolean isAvailable;       // 设备是否可用
    private Long version;              // 乐观锁版本，用于并发控制

    // 构造函数
    public Device(Long id, String deviceCode, DeviceType type) {
        this.id = id;
        this.deviceCode = deviceCode;
        this.type = type;
        this.status = DeviceStatus.IDLE;
        this.isAvailable = true;
        this.version = 0L;
    }

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }
    public DeviceType getType() { return type; }
    public void setType(DeviceType type) { this.type = type; }
    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }
    public Long getOccupiedByProcess() { return occupiedByProcess; }
    public void setOccupiedByProcess(Long occupiedByProcess) { this.occupiedByProcess = occupiedByProcess; }
    public Long getAllocatedTime() { return allocatedTime; }
    public void setAllocatedTime(Long allocatedTime) { this.allocatedTime = allocatedTime; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}