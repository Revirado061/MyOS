package com.group.myos.interrupt.handler;

import com.group.myos.interrupt.event.InterruptHandledEvent;
import com.group.myos.interrupt.event.InterruptTriggeredEvent;
import com.group.myos.interrupt.model.Interrupt;
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
        Interrupt interrupt = event.getInterrupt();
        logger.info("中断触发 - 类型: {}, 进程ID: {}, 原因: {}, 优先级: {}", 
            interrupt.getType(),
            interrupt.getData().get("processId"),
            interrupt.getData().get("reason"),
            interrupt.getType().getPriority());
    }

    /**
     * 监听中断处理完成事件
     * @param event 中断处理完成事件
     */
    @EventListener
    public void handleInterruptHandled(InterruptHandledEvent event) {
        Interrupt interrupt = event.getInterrupt();
        logger.info("中断处理完成 - 类型: {}, 进程ID: {}, 原因: {}, 处理时间: {}ms", 
            interrupt.getType(),
            interrupt.getData().get("processId"),
            interrupt.getData().get("reason"),
            event.getProcessingTime());
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