package com.group.myos.interrupt.manager;

import com.group.myos.interrupt.model.Interrupt;
import com.group.myos.interrupt.model.InterruptLog;
import com.group.myos.interrupt.model.InterruptType;
import com.group.myos.interrupt.event.InterruptTriggeredEvent;
import com.group.myos.interrupt.event.InterruptHandledEvent;
import com.group.myos.interrupt.event.ProcessSchedulingEvent;
import com.group.myos.interrupt.event.ProcessTerminationEvent;
import com.group.myos.interrupt.event.ProcessWaitingEvent;
import com.group.myos.interrupt.event.ProcessReadyEvent;
import com.group.myos.interrupt.event.InterruptLogUpdatedEvent;
import com.group.myos.device.manager.DeviceManager;
import com.group.myos.device.event.DeviceTimeoutEvent;
import com.group.myos.process.model.Process;
import com.group.myos.process.repository.ProcessRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.group.myos.interrupt.event.ClockInterruptEvent;

/**
 * 中断管理器
 * 负责中断的注册、触发、处理和日志记录
 */
@Slf4j
@Component
public class InterruptManager {
    // 使用优先级队列，根据中断类型优先级排序
    private final PriorityQueue<Interrupt> interruptQueue = new PriorityQueue<>(
        Comparator.comparingInt(interrupt -> interrupt.getType().getPriority())
    );
    private final List<InterruptLog> interruptLogs = new CopyOnWriteArrayList<>();
    private final AtomicLong interruptIdGenerator = new AtomicLong(0);
    private final AtomicLong logIdGenerator = new AtomicLong(0);
    private static final long PROCESSING_DELAY = 50; // 中断处理延迟改为50ms
    private static final int TIME_SLICE = 1; // 时间片长度（秒）
    private static final int CLOCK_INTERRUPT_HANDLING_TIME = 50; // 时钟中断处理时间（毫秒）
    private static final int CLOCK_LOG_INTERVAL = 10; // 时钟日志输出间隔（秒）
    private long clockInterruptCount = 0; // 时钟中断计数器

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private DeviceManager deviceManager; // 添加设备管理器依赖

    @Autowired
    private ProcessRepository processRepository;

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
            metadata.put("reason", data);  // 改用reason作为key
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
     * 定时任务，每50ms执行一次
     */
    @Scheduled(fixedRate = 50)
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
                    long processingTime = System.currentTimeMillis() - interrupt.getTimestamp();
                    eventPublisher.publishEvent(new InterruptHandledEvent(interrupt, result, processingTime));
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
        String result;
        try {
            // 非时钟中断时增加处理延迟
            if (interrupt.getType() != InterruptType.CLOCK) {
                Thread.sleep(50); // 其他中断处理延迟改为50ms
            }
            
            switch (interrupt.getType()) {
                case ERROR:
                    result = String.format("错误中断处理完成，优先级: %d", interrupt.getType().getPriority());
                    handleProcessTermination(interrupt);
                    break;
                case DEVICE:
                    result = String.format("设备中断处理完成，优先级: %d", interrupt.getType().getPriority());
                    handleDeviceInterrupt(interrupt);
                    break;
                case IO:
                    result = String.format("I/O中断处理完成，优先级: %d", interrupt.getType().getPriority());
                    handleIOInterrupt(interrupt);
                    break;
                case PROCESS:
                    result = String.format("进程中断处理完成，优先级: %d", interrupt.getType().getPriority());
                    handleProcessInterrupt(interrupt);
                    break;
                case CLOCK:
                    result = String.format("时钟中断处理完成，优先级: %d", interrupt.getType().getPriority());
                    handleClockInterrupt(interrupt);
                    break;
                case OTHER:
                    result = String.format("其他中断处理完成，优先级: %d", interrupt.getType().getPriority());
                    handleOtherInterrupt(interrupt);
                    break;
                default:
                    result = "未知中断类型";
            }
        } catch (InterruptedException e) {
            log.error("中断处理被中断", e);
            result = "中断处理被中断";
        }
        return result;
    }

    /**
     * 处理进程终止
     */
    private void handleProcessTermination(Interrupt interrupt) {
        Long processId = (Long) interrupt.getData().get("processId");
        if (processId != null) {
            // 发布进程终止事件
            eventPublisher.publishEvent(new ProcessTerminationEvent(this, processId));
            log.info("发布进程终止事件 - 进程ID: {}", processId);
            
            // 发布进程调度事件，调度下一个进程
            eventPublisher.publishEvent(new ProcessSchedulingEvent(this));
            log.info("发布进程调度事件 - 调度下一个进程");
        }
    }

    /**
     * 处理设备中断
     */
    private void handleDeviceInterrupt(Interrupt interrupt) {
        Long processId = (Long) interrupt.getData().get("processId");
        String deviceType = (String) interrupt.getData().get("deviceType");
        
        if (processId != null && deviceType != null) {
            // 检查设备是否可用
            if (deviceManager.isDeviceAvailable(deviceType)) {
                // 设备可用，分配设备给进程
                deviceManager.allocateDevice(deviceType, processId);
                log.info("设备分配成功 - 进程ID: {}, 设备类型: {}", processId, deviceType);
                
                // 发布进程就绪事件，让进程继续运行
                eventPublisher.publishEvent(new ProcessReadyEvent(this, processId));
                log.info("发布进程就绪事件 - 进程ID: {}, 原因: 设备已分配", processId);
            } else {
                // 设备不可用，进程需要等待
                eventPublisher.publishEvent(new ProcessWaitingEvent(this, processId, "等待设备: " + deviceType));
                log.info("发布设备等待事件 - 进程ID: {}, 原因: 等待设备 {}", processId, deviceType);
                
                // 发布进程调度事件，调度下一个进程
                eventPublisher.publishEvent(new ProcessSchedulingEvent(this));
                log.info("发布进程调度事件 - 调度下一个进程");
            }
        }
    }

    /**
     * 处理I/O中断
     */
    private void handleIOInterrupt(Interrupt interrupt) {
        Long processId = (Long) interrupt.getData().get("processId");
        if (processId != null) {
            // 获取进程
            Process process = processRepository.findById(processId).orElse(null);
            if (process == null) {
                log.warn("I/O中断处理失败 - 进程不存在: {}", processId);
                return;
            }
            
            // 检查进程是否在运行状态
            if (process.getState() != Process.ProcessState.RUNNING) {
                log.warn("I/O中断处理失败 - 进程不在运行状态: {}, 当前状态: {}", processId, process.getState());
                return;
            }
            
            // 发布进程等待事件
            eventPublisher.publishEvent(new ProcessWaitingEvent(this, processId, "等待I/O"));
            log.info("发布I/O等待事件 - 进程ID: {}, 原因: 等待I/O", processId);
            
            // 发布进程调度事件，调度下一个进程
            eventPublisher.publishEvent(new ProcessSchedulingEvent(this));
            log.info("发布进程调度事件 - 调度下一个进程");
        }
    }

    /**
     * 处理进程中断
     */
    private void handleProcessInterrupt(Interrupt interrupt) {
        Long processId = (Long) interrupt.getData().get("processId");
        String reason = (String) interrupt.getData().get("reason");
        if (processId != null) {
            if ("PROCESS".equals(reason)) {
                log.info("处理进程中断 - 类型: PROCESS, 进程ID: {}, 原因: 高优先级进程抢占", processId);
                // 高优先级进程到达，将当前进程设置为就绪状态
                eventPublisher.publishEvent(new ProcessReadyEvent(this, processId));
                log.info("发布进程就绪事件 - 进程ID: {}, 原因: 高优先级进程到达", processId);
                
                // 发布进程调度事件，调度高优先级进程
                eventPublisher.publishEvent(new ProcessSchedulingEvent(this));
                log.info("发布进程调度事件 - 调度高优先级进程");
            } else {
                // 其他进程中断，将进程设置为就绪状态
                eventPublisher.publishEvent(new ProcessReadyEvent(this, processId));
                log.info("发布进程就绪事件 - 进程ID: {}, 原因: {}", processId, reason);
            }
        }
    }

    /**
     * 处理时钟中断
     */
    private void handleClockInterrupt(Interrupt interrupt) {
        clockInterruptCount++; // 增加计数器
        
        // 每10次时钟中断记录一次日志（即每10秒）
        if (clockInterruptCount % 10 == 0) {
            InterruptLog log = new InterruptLog(
                logIdGenerator.incrementAndGet(),
                interrupt.getId(),
                InterruptType.CLOCK,
                System.currentTimeMillis(),
                String.format("时钟中断 - 时间片长度: %d秒, 第%d次时钟中断", 
                    TIME_SLICE, clockInterruptCount),
                "已触发"
            );
            interruptLogs.add(log);
            eventPublisher.publishEvent(new InterruptLogUpdatedEvent(this, log));
        }
        
        try {
            // 模拟时钟中断处理时间
            Thread.sleep(CLOCK_INTERRUPT_HANDLING_TIME);
            
            // 发布进程调度事件
            eventPublisher.publishEvent(new ProcessSchedulingEvent(this));
        } catch (InterruptedException e) {
            log.error("时钟中断处理被中断", e);
        }
    }

    /**
     * 处理其他中断
     */
    private void handleOtherInterrupt(Interrupt interrupt) {
        Long processId = (Long) interrupt.getData().get("processId");
        if (processId != null) {
            // 发布进程就绪事件
            eventPublisher.publishEvent(new ProcessReadyEvent(this, processId));
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
            .filter(log -> type == null || log.getType() == type)  // 移除时钟中断过滤
            .sorted(Comparator.comparing(InterruptLog::getTimestamp).reversed())  // 按时间戳倒序排序
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
        // 对于时钟中断，不记录日志（由handleClockInterrupt方法处理）
        if (interrupt.getType() == InterruptType.CLOCK) {
            return;
        }

        InterruptLog log = new InterruptLog(
            logIdGenerator.incrementAndGet(),
            interrupt.getId(),
            interrupt.getType(),
            interrupt.getTimestamp(),
            interrupt.getMessage(),
            "已触发"  // 修改初始状态为"已触发"
        );
        interruptLogs.add(log);
        
        // 保持日志数量不超过5000条
        if (interruptLogs.size() > 5000) {
            interruptLogs.remove(0);
        }
        
        // 发布日志更新事件
        eventPublisher.publishEvent(new InterruptLogUpdatedEvent(this, log));
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
            .ifPresent(log -> {
                log.setResult(result);
                // 发布日志更新事件
                eventPublisher.publishEvent(new InterruptLogUpdatedEvent(this, log));
            });
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

    @EventListener
    public void handleClockInterruptEvent(ClockInterruptEvent event) {
        // 触发时钟中断
        triggerInterrupt(0, InterruptType.CLOCK, null, "系统时钟中断");
    }

    @EventListener
    public void handleDeviceTimeoutEvent(DeviceTimeoutEvent event) {
        // 检查deviceId是否为空
        if (event.getDeviceId() == null) {
            return;
        }
        
        // 立即触发设备中断，不等待
        triggerInterrupt(
            event.getDeviceId().intValue(),
            InterruptType.DEVICE,
            event.getDeviceId(),
            String.format("设备 %d 使用时间到，已自动释放", event.getDeviceId())
        );
        
        log.info("设备超时事件处理完成 - 设备ID: {}", event.getDeviceId());
    }
}