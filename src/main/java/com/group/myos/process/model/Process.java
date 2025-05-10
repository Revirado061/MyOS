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

    public enum ProcessState {
        NEW,
        READY,
        RUNNING,
        WAITING,
        TERMINATED
    }

    // 无参构造函数
    public Process() {
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
    }

}