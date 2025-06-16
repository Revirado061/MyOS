package com.group.myos.interrupt.handler;

import com.group.myos.interrupt.event.InterruptLogUpdatedEvent;
import com.group.myos.interrupt.model.InterruptLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class InterruptLogWebSocketHandler {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    public InterruptLogWebSocketHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @EventListener
    public void handleInterruptLogUpdated(InterruptLogUpdatedEvent event) {
        InterruptLog log = event.getLog();
        messagingTemplate.convertAndSend("/topic/interrupts/logs", log);
    }
} 