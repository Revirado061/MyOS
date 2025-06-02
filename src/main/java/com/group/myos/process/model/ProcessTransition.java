package com.group.myos.process.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "process_transitions")
public class ProcessTransition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "process_id")
    private Process process;

    @Enumerated(EnumType.STRING)
    private Process.ProcessState fromState;

    @Enumerated(EnumType.STRING)
    private Process.ProcessState toState;

    private LocalDateTime transitionTime;
    private String reason; // 可选的转换原因

    public ProcessTransition(Process process, Process.ProcessState fromState, Process.ProcessState toState) {
        this.process = process;
        this.fromState = fromState;
        this.toState = toState;
        this.transitionTime = LocalDateTime.now();
    }

    public ProcessTransition(Process process, Process.ProcessState fromState, Process.ProcessState toState, String reason) {
        this(process, fromState, toState);
        this.reason = reason;
    }
} 