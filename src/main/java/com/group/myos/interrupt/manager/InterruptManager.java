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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 中断管理器
 * 负责中断的注册、触发、处理和日志记录
 */
@Component
public class InterruptManager {
    private final Queue<Interrupt> interruptQueue = new ConcurrentLinkedQueue<>();
    private final List<InterruptLog> interruptLogs = new CopyOnWriteArrayList<>();
    private final AtomicLong interruptIdGenerator = new AtomicLong(0);
    private final AtomicLong logIdGenerator = new AtomicLong(0);

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

        Interrupt interrupt = new Interrupt(
            interruptIdGenerator.incrementAndGet(),
            type,
            System.currentTimeMillis(),
            metadata,
            generateMessage(type, processId, data)
        );
        
        interruptQueue.offer(interrupt);
        addLog(interrupt);
        
        // 发布中断触发事件
        eventPublisher.publishEvent(new InterruptTriggeredEvent(interrupt));
    }

    /**
     * 处理中断
     * 定时任务，每100ms执行一次
     */
    @Scheduled(fixedRate = 100)
    public void handleInterrupts() {
        Interrupt interrupt = interruptQueue.poll();
        if (interrupt != null) {
            String result = processInterrupt(interrupt);
            updateLog(interrupt.getId(), result);
            
            // 发布中断处理事件
            eventPublisher.publishEvent(new InterruptHandledEvent(interrupt, result));
        }
    }

    /**
     * 处理中断的具体逻辑
     * @param interrupt 中断对象
     * @return 处理结果
     */
    private String processInterrupt(Interrupt interrupt) {
        switch (interrupt.getType()) {
            case CLOCK:
                return "时钟中断处理完成";
            case DEVICE:
                return "设备中断处理完成";
            case IO:
                return "I/O中断处理完成";
            case PROCESS:
                return "进程中断处理完成";
            case OTHER:
                return "其他中断处理完成";
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
        return List.copyOf(interruptQueue);
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