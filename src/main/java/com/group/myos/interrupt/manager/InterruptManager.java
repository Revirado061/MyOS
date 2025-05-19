package com.group.myos.interrupt.manager;

import com.group.myos.interrupt.model.Interrupt;
import com.group.myos.interrupt.model.InterruptLog;
import com.group.myos.interrupt.model.InterruptType;
import com.group.myos.interrupt.event.InterruptTriggeredEvent;
import com.group.myos.interrupt.event.InterruptHandledEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 中断管理器
 * 负责中断的注册、触发、处理和日志记录
 */
@Component
public class InterruptManager {
    // 使用优先级队列，根据中断类型优先级排序
    private final PriorityQueue<Interrupt> interruptQueue = new PriorityQueue<>(
        Comparator.comparingInt(interrupt -> interrupt.getType().getPriority())
    );
    private final List<InterruptLog> interruptLogs = new CopyOnWriteArrayList<>();
    private final AtomicLong interruptIdGenerator = new AtomicLong(0);
    private final AtomicLong logIdGenerator = new AtomicLong(0);
    private static final long PROCESSING_DELAY = 3000; // 中断处理延迟3秒

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 触发中断
     * @param vector 中断向量号
     * @param type 中断类型
     * @param processId 相关进程ID
     * @param data 中断数据
     */
    public void triggerInterrupt(int vector, InterruptType type, Long processId, String data) {
        Map<String, Object> metadata = new HashMap<>();
        if (processId != null) {
            metadata.put("processId", processId);
        }
        if (data != null) {
            metadata.put("data", data);
        }
        // 1. 创建中断对象
        Interrupt interrupt = new Interrupt(
            interruptIdGenerator.incrementAndGet(),
            type,
            System.currentTimeMillis(),
            metadata,
            generateMessage(type, processId, data)
        );
        
        // 2. 加入中断队列
        interruptQueue.offer(interrupt);
        addLog(interrupt);
        
        // 3. 发布中断触发事件
        eventPublisher.publishEvent(new InterruptTriggeredEvent(interrupt));
    }

    /**
     * 处理中断
     * 定时任务，每100ms执行一次
     */
    @Scheduled(fixedRate = 100)
    public void handleInterrupts() {
        Interrupt interrupt = interruptQueue.peek(); // 只查看不移除
        if (interrupt != null) {
            long currentTime = System.currentTimeMillis();
            // 如果中断在队列中超过延迟时间，则处理它
            if (currentTime - interrupt.getTimestamp() >= PROCESSING_DELAY) {
                interrupt = interruptQueue.poll(); // 现在移除它
                if (interrupt != null) {
                    // 1. 处理中断
                    String result = processInterrupt(interrupt);
                    
                    // 2. 记录处理结果
                    updateLog(interrupt.getId(), result);
                    
                    // 3. 发布处理完成事件
                    eventPublisher.publishEvent(new InterruptHandledEvent(interrupt, result));
                }
            }
        }
    }

    /**
     * 处理中断的具体逻辑
     * @param interrupt 中断对象
     * @return 处理结果
     */
    private String processInterrupt(Interrupt interrupt) {
        switch (interrupt.getType()) {
            case ERROR:
                return String.format("错误中断处理完成，优先级: %d", interrupt.getType().getPriority());
            case DEVICE:
                return String.format("设备中断处理完成，优先级: %d", interrupt.getType().getPriority());
            case IO:
                return String.format("I/O中断处理完成，优先级: %d", interrupt.getType().getPriority());
            case PROCESS:
                return String.format("进程中断处理完成，优先级: %d", interrupt.getType().getPriority());
            case CLOCK:
                return String.format("时钟中断处理完成，优先级: %d", interrupt.getType().getPriority());
            case OTHER:
                return String.format("其他中断处理完成，优先级: %d", interrupt.getType().getPriority());
            default:
                return "未知中断类型";
        }
    }

    /**
     * 获取中断日志
     * @param limit 返回的最大日志数量
     * @param type 中断类型过滤
     * @return 中断日志列表
     */
    public List<InterruptLog> getInterruptLogs(int limit, InterruptType type) {
        return interruptLogs.stream()
            .filter(log -> type == null || log.getType() == type)
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取当前中断队列
     * @return 中断队列的副本
     */
    public List<Interrupt> getInterruptQueue() {
        return new ArrayList<>(interruptQueue);
    }

    /**
     * 添加中断日志
     * @param interrupt 中断对象
     */
    private void addLog(Interrupt interrupt) {
        InterruptLog log = new InterruptLog(
            logIdGenerator.incrementAndGet(),
            interrupt.getId(),
            interrupt.getType(),
            interrupt.getTimestamp(),
            interrupt.getMessage(),
            "待处理"
        );
        interruptLogs.add(log);
        
        // 保持日志数量不超过100条
        if (interruptLogs.size() > 100) {
            interruptLogs.remove(0);
        }
    }

    /**
     * 更新中断日志的处理结果
     * @param interruptId 中断ID
     * @param result 处理结果
     */
    private void updateLog(Long interruptId, String result) {
        interruptLogs.stream()
            .filter(log -> log.getInterruptId().equals(interruptId))
            .findFirst()
            .ifPresent(log -> log.setResult(result));
    }

    /**
     * 生成中断消息
     * @param type 中断类型
     * @param processId 进程ID
     * @param data 中断数据
     * @return 格式化的消息字符串
     */
    private String generateMessage(InterruptType type, Long processId, String data) {
        StringBuilder message = new StringBuilder();
        message.append("中断类型: ").append(type);
        message.append(" (优先级: ").append(type.getPriority()).append(")");
        if (processId != null) {
            message.append(", 进程ID: ").append(processId);
        }
        if (data != null) {
            message.append(", 数据: ").append(data);
        }
        return message.toString();
    }

    /**
     * 重置中断管理器
     * 清空中断队列和日志
     */
    public void reset() {
        interruptQueue.clear();
        interruptLogs.clear();
        interruptIdGenerator.set(0);
        logIdGenerator.set(0);
    }
}