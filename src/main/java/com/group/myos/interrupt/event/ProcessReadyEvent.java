package com.group.myos.interrupt.event;

import com.group.myos.interrupt.manager.InterruptManager;
import org.springframework.context.ApplicationEvent;

/**
 * 进程就绪事件
 * 用于在进程进入就绪状态时发布事件
 */
public class ProcessReadyEvent extends ApplicationEvent {
    private final Long processId;

    public ProcessReadyEvent(InterruptManager source, Long processId) {
        super(source);
        this.processId = processId;
    }

    public Long getProcessId() {
        return processId;
    }
} 