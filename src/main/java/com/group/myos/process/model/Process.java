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
    private LocalDateTime lastUpdateTime;
    private boolean inMemory; // 是否在内存中，用于交换机制
    private String swapFilePath; // 交换文件路径

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
    }

    // 包含 id 的构造函数
    public Process(Long id, String name, Integer priority, ProcessState state, Integer memorySize) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.state = state;
        this.memorySize = memorySize;
        this.createTime = LocalDateTime.now(); // 设置创建时间为当前时间
        this.lastUpdateTime = LocalDateTime.now(); // 设置最后更新时间为当前时间
        this.inMemory = true; // 默认进程在内存中
    }

}