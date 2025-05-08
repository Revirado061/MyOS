package com.group.myos.interrupt;

import com.group.myos.process.model.Process;
import com.group.myos.process.scheduler.ProcessScheduler;
import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class InterruptHandler {
    private final ProcessScheduler scheduler;
    private final Map<InterruptType, List<InterruptHandler>> handlers;
    
    public InterruptHandler(ProcessScheduler scheduler) {
        this.scheduler = scheduler;
        this.handlers = new EnumMap<>(InterruptType.class);
        initializeHandlers();
    }
    
    private void initializeHandlers() {
        // 初始化各种中断的处理程序
        handlers.put(InterruptType.TIMER, new ArrayList<>());
        handlers.put(InterruptType.IO, new ArrayList<>());
        handlers.put(InterruptType.SYSTEM_CALL, new ArrayList<>());
        handlers.put(InterruptType.ERROR, new ArrayList<>());
    }
    
    public void registerHandler(InterruptType type, InterruptHandler handler) {
        handlers.get(type).add(handler);
    }
    
    public void handleInterrupt(InterruptType type, Process process, Object data) {
        // 根据中断类型进行相应处理
        switch (type) {
            case TIMER:
                handleTimerInterrupt(process);
                break;
            case IO:
                handleIOInterrupt(process, data);
                break;
            case SYSTEM_CALL:
                handleSystemCall(process, data);
                break;
            case ERROR:
                handleError(process, data);
                break;
        }
    }
    
    private void handleTimerInterrupt(Process process) {
        // 时间片到期，重新调度
        scheduler.schedule();
    }
    
    private void handleIOInterrupt(Process process, Object data) {
        // IO操作完成，唤醒等待的进程
        if (process != null) {
            scheduler.addProcess(process);
        }
    }
    
    private void handleSystemCall(Process process, Object data) {
        // 处理系统调用
        if (data instanceof SystemCall) {
            SystemCall call = (SystemCall) data;
            call.execute(process);
        }
    }
    
    private void handleError(Process process, Object data) {
        // 处理错误
        if (process != null) {
            scheduler.terminateProcess(process);
        }
    }
    
    public enum InterruptType {
        TIMER,
        IO,
        SYSTEM_CALL,
        ERROR
    }
    
    @Data
    public static class SystemCall {
        private final String name;
        private final Object[] parameters;
        
        public void execute(Process process) {
            // 实现具体的系统调用逻辑
        }
    }
} 