package com.group.myos.interrupt.event;

import com.group.myos.interrupt.manager.InterruptManager;
import org.springframework.context.ApplicationEvent;

/**
 * 进程调度事件
 * 用于在需要进程调度时发布事件
 */
public class ProcessSchedulingEvent extends ApplicationEvent {
    private final InterruptManager source;

    public ProcessSchedulingEvent(InterruptManager source) {
        super(source);
        this.source = source;
    }

    public InterruptManager getSource() {
        return source;
    }
} 