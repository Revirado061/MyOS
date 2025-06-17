package com.group.myos.interrupt.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ClockInterruptEvent extends ApplicationEvent {
    private final long currentTime;

    public ClockInterruptEvent(Object source, long currentTime) {
        super(source);
        this.currentTime = currentTime;
    }
} 