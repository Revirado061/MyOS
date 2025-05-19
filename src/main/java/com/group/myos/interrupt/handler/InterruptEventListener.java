package com.group.myos.interrupt.handler;

import com.group.myos.interrupt.event.InterruptHandledEvent;
import com.group.myos.interrupt.event.InterruptTriggeredEvent;
import com.group.myos.interrupt.model.InterruptLog;
import com.group.myos.interrupt.model.InterruptType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中断事件监听器
 * 用于监听和处理中断相关的事件
 */
@Component
public class InterruptEventListener {
    private static final Logger logger = LoggerFactory.getLogger(InterruptEventListener.class);
    private final AtomicLong logIdGenerator = new AtomicLong(1);
    private final List<InterruptLog> interruptLogs = new CopyOnWriteArrayList<>();

    /**
     * 监听中断触发事件
     * @param event 中断触发事件
     */
    @EventListener
    public void handleInterruptTriggered(InterruptTriggeredEvent event) {
        InterruptLog log = new InterruptLog(
            logIdGenerator.getAndIncrement(),
            event.getInterrupt().getId(),
            event.getInterrupt().getType(),
            System.currentTimeMillis(),
            event.getInterrupt().getMessage(),
            null
        );
        interruptLogs.add(log);
        
        logger.info("中断已触发: 类型={}, 消息={}", 
            event.getInterrupt().getType(),
            event.getInterrupt().getMessage());
    }

    /**
     * 监听中断处理完成事件
     * @param event 中断处理完成事件
     */
    @EventListener
    public void handleInterruptHandled(InterruptHandledEvent event) {
        interruptLogs.stream()
            .filter(log -> log.getInterruptId().equals(event.getInterrupt().getId()))
            .findFirst()
            .ifPresent(log -> log.setResult(event.getResult()));
        
        logger.info("中断处理完成: 类型={}, 结果={}", 
            event.getInterrupt().getType(),
            event.getResult());
    }

    /**
     * 获取中断日志列表
     * @return 中断日志列表
     */
    public List<InterruptLog> getInterruptLogs() {
        return new ArrayList<>(interruptLogs);
    }

    /**
     * 获取指定类型的中断日志
     * @param type 中断类型
     * @return 中断日志列表
     */
    public List<InterruptLog> getInterruptLogsByType(String type) {
        return interruptLogs.stream()
            .filter(log -> log.getType().name().equals(type))
            .toList();
    }
} 