package com.group.myos.process;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 进程调度器线程
 * 负责自动调度进程
 */
@Slf4j
@Component
public class ProcessSchedulerThread implements Runnable {
    
    private final ProcessScheduler processScheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private static final long SCHEDULING_INTERVAL = 1000; // 调度间隔，单位毫秒
    
    @Autowired
    public ProcessSchedulerThread(ProcessScheduler processScheduler) {
        this.processScheduler = processScheduler;
    }
    
    /**
     * 启动调度器线程
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = new Thread(this);
            worker.setName("ProcessSchedulerThread");
            worker.start();
            log.info("进程调度器线程已启动");
        }
    }
    
    /**
     * 停止调度器线程
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (worker != null) {
                worker.interrupt();
                try {
                    worker.join(5000); // 等待线程结束，最多5秒
                } catch (InterruptedException e) {
                    log.error("等待进程调度器线程结束时被中断", e);
                    Thread.currentThread().interrupt();
                }
            }
            log.info("进程调度器线程已停止");
        }
    }
    
    /**
     * 判断调度器线程是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }
    
    @Override
    public void run() {
        log.info("进程调度器线程开始运行");
        
        while (running.get()) {
            try {
                // 执行进程调度
                log.debug("执行自动调度");
                processScheduler.schedule();
                
                // 等待一段时间
                Thread.sleep(SCHEDULING_INTERVAL);
            } catch (InterruptedException e) {
                log.info("进程调度器线程被中断");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("进程调度器线程发生错误", e);
            }
        }
        
        log.info("进程调度器线程已结束");
    }
} 