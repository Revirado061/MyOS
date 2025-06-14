package com.group.myos.interrupt.event;

import com.group.myos.interrupt.manager.InterruptManager;
import org.springframework.context.ApplicationEvent;

public class ProcessTerminationEvent extends ApplicationEvent {
    private final Long processId;

    public ProcessTerminationEvent(InterruptManager source, Long processId) {
        super(source);
        this.processId = processId;
    }

    public Long getProcessId() {
        return processId;
    }
} 