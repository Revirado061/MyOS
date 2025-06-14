package com.group.myos.process.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "processes")
public class Process {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer priority;

    @Enumerated(EnumType.STRING)
    private ProcessState state;

    private Integer memorySize;
    private LocalDateTime createTime;
    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;
    private boolean inMemory; // 是否在内存中，用于交换机制
    private String swapFilePath; // 交换文件路径
    @Column(name = "waiting_reason")
    private String waitingReason;

    public enum ProcessState {
        NEW,        // 新建
        READY,      // 就绪
        RUNNING,    // 运行
        WAITING,    // 等待/阻塞
        TERMINATED  // 终止
    }

    // 无参构造函数
    public Process() {
        this.inMemory = true; // 默认进程在内存中
        this.priority = 0; // 默认优先级为0
        this.memorySize = 0; // 默认内存大小为0
        this.state = ProcessState.NEW; // 默认状态为NEW
        this.createTime = LocalDateTime.now(); // 设置创建时间为当前时间
        this.lastUpdateTime = LocalDateTime.now(); // 设置最后更新时间为当前时间
    }

    // 包含 id 的构造函数
    public Process(Long id, String name, Integer priority, ProcessState state, Integer memorySize) {
        this.id = id;
        this.name = name;
        this.priority = priority != null ? priority : 0;
        this.state = state != null ? state : ProcessState.NEW;
        this.memorySize = memorySize != null ? memorySize : 0;
        this.createTime = LocalDateTime.now(); // 设置创建时间为当前时间
        this.lastUpdateTime = LocalDateTime.now(); // 设置最后更新时间为当前时间
        this.inMemory = true; // 默认进程在内存中
    }

    // Getters and Setters
    public String getWaitingReason() {
        return waitingReason;
    }

    public void setWaitingReason(String waitingReason) {
        this.waitingReason = waitingReason;
    }

}