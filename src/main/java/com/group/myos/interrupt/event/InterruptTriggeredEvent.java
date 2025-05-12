package com.group.myos.interrupt.event;

import com.group.myos.interrupt.model.Interrupt;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class InterruptTriggeredEvent extends ApplicationEvent {
    private final Interrupt interrupt;

    public InterruptTriggeredEvent(Interrupt interrupt) {
        super(interrupt);
        this.interrupt = interrupt;
    }
} 