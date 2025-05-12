package com.group.myos.interrupt.event;

import com.group.myos.interrupt.model.Interrupt;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class InterruptHandledEvent extends ApplicationEvent {
    private final Interrupt interrupt;
    private final String result;

    public InterruptHandledEvent(Interrupt interrupt, String result) {
        super(interrupt);
        this.interrupt = interrupt;
        this.result = result;
    }
} 