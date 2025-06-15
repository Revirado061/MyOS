package com.group.myos.interrupt.event;

import com.group.myos.interrupt.manager.InterruptManager;
import org.springframework.context.ApplicationEvent;

/**
 * 进程等待事件
 * 用于在进程需要等待时发布事件
 */
public class ProcessWaitingEvent extends ApplicationEvent {
    private final Long processId;
    private final String reason;

    public ProcessWaitingEvent(InterruptManager source, Long processId, String reason) {
        super(source);
        this.processId = processId;
        this.reason = reason;
    }

    public Long getProcessId() {
        return processId;
    }

    public String getReason() {
        return reason;
    }
} 