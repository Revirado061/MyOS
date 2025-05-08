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
} 