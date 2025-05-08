package com.group.myos.timer;

import com.group.myos.interrupt.InterruptHandler;
import com.group.myos.process.model.Process;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Timer {
    private final InterruptHandler interruptHandler;
    private static final long TIME_SLICE = 100; // 时间片长度（毫秒）
    
    public Timer(InterruptHandler interruptHandler) {
        this.interruptHandler = interruptHandler;
    }
    
    @Scheduled(fixedRate = TIME_SLICE)
    public void tick() {
        // 每个时间片触发一次时钟中断
        Process currentProcess = null; // 这里需要从进程调度器获取当前运行的进程
        interruptHandler.handleInterrupt(
            InterruptHandler.InterruptType.TIMER,
            currentProcess,
            null
        );
    }
    
    public void startTimer() {
        // 启动时钟
    }
    
    public void stopTimer() {
        // 停止时钟
    }
    
    public void resetTimer() {
        // 重置时钟
    }
} 