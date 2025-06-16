package com.group.myos.interrupt.handler;

import com.group.myos.interrupt.event.InterruptLogUpdatedEvent;
import com.group.myos.interrupt.model.InterruptLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InterruptLogEventListener {
    private static final Logger logger = LoggerFactory.getLogger(InterruptLogEventListener.class);

    @EventListener
    public void handleInterruptLogUpdated(InterruptLogUpdatedEvent event) {
        InterruptLog log = event.getLog();
        logger.info("中断日志更新 - ID: {}, 类型: {}, 状态: {}, 消息: {}", 
            log.getId(),
            log.getType(),
            log.getResult(),
            log.getMessage());
    }
} 