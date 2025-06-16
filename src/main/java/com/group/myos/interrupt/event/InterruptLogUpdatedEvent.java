package com.group.myos.interrupt.event;

import com.group.myos.interrupt.model.InterruptLog;
import org.springframework.context.ApplicationEvent;

/**
 * 中断日志更新事件
 * 当中断日志状态发生变化时触发
 */
public class InterruptLogUpdatedEvent extends ApplicationEvent {
    private final InterruptLog log;

    public InterruptLogUpdatedEvent(Object source, InterruptLog log) {
        super(source);
        this.log = log;
    }

    public InterruptLog getLog() {
        return log;
    }
} 