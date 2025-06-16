package com.group.myos.process;

import com.group.myos.memory.MemoryManager;
import com.group.myos.process.model.Process;
import com.group.myos.process.model.ProcessTransition;
import com.group.myos.process.repository.ProcessRepository;
import com.group.myos.process.repository.ProcessTransitionRepository;
import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.InterruptType;
import com.group.myos.interrupt.event.ProcessSchedulingEvent;
import com.group.myos.interrupt.event.ProcessTerminationEvent;
import com.group.myos.interrupt.event.ProcessWaitingEvent;
import com.group.myos.interrupt.event.ProcessReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class ProcessScheduler {
    private PriorityBlockingQueue<Process> readyQueue;
    private final Map<Long, Process> allProcesses;
    private final List<Process> waitingProcesses;
    private final List<Process> terminatedProcesses;
    private Process currentProcess;
    
    private final MemoryManager memoryManager;
    private final ProcessSwapper processSwapper;
    private final ProcessTransitionRepository transitionRepository;
    private final ProcessRepository processRepository;
    private final InterruptManager interruptManager;
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessScheduler.class);
    
    // 添加调度算法枚举
    public enum SchedulingAlgorithm {
        FCFS,    // 先到先服务
        PRIORITY // 优先级调度
    }
    
    private SchedulingAlgorithm currentAlgorithm = SchedulingAlgorithm.PRIORITY; // 默认使用优先级调度
    
    public ProcessScheduler(
            MemoryManager memoryManager, 
            ProcessSwapper processSwapper,
            ProcessTransitionRepository transitionRepository,
            ProcessRepository processRepository,
            InterruptManager interruptManager) {
        this.memoryManager = memoryManager;
        this.processSwapper = processSwapper;
        this.transitionRepository = transitionRepository;
        this.processRepository = processRepository;
        this.interruptManager = interruptManager;
        
        // 根据当前调度算法初始化就绪队列
        this.readyQueue = new PriorityBlockingQueue<>(100, 
            currentAlgorithm == SchedulingAlgorithm.PRIORITY ?
            (p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority()) : // 优先级调度，数字大的优先级高
            Comparator.comparing(Process::getCreateTime)); // FCFS调度
            
        this.allProcesses = new ConcurrentHashMap<>();
        this.waitingProcesses = new ArrayList<>();
        this.terminatedProcesses = new ArrayList<>();
        
        // 初始化时从数据库加载所有进程
        loadProcessesFromDatabase();
    }
    
    /**
     * 设置调度算法
     */
    public void setSchedulingAlgorithm(SchedulingAlgorithm algorithm) {
        if (this.currentAlgorithm != algorithm) {
            this.currentAlgorithm = algorithm;
            
            // 重新创建就绪队列
            PriorityBlockingQueue<Process> newQueue = new PriorityBlockingQueue<>(100,
                algorithm == SchedulingAlgorithm.PRIORITY ?
                (p1, p2) -> Integer.compare(p2.getPriority(), p1.getPriority()) : // 优先级调度，数字大的优先级高
                Comparator.comparing(Process::getCreateTime));
                
            // 将现有进程转移到新队列
            readyQueue.drainTo(newQueue);
            this.readyQueue = newQueue;
        }
    }
    
    /**
     * 获取当前调度算法
     */
    public SchedulingAlgorithm getCurrentAlgorithm() {
        return currentAlgorithm;
    }
    
    /**
     * 自动调度进程
     * 当进程进入就绪队列时自动调用
     */
    private void autoSchedule() {
        // 如果当前没有运行中的进程，立即调度
        if (currentProcess == null) {
            schedule();
        }
    }
    
    /**
     * 从数据库加载所有进程
     */
    private void loadProcessesFromDatabase() {
        List<Process> processes = processRepository.findAll();
        for (Process process : processes) {
            allProcesses.put(process.getId(), process);
            
            // 根据进程状态放入相应队列
            switch (process.getState()) {
                case READY:
                    readyQueue.offer(process);
                    break;
                case WAITING:
                    waitingProcesses.add(process);
                    break;
                case TERMINATED:
                    terminatedProcesses.add(process);
                    break;
                case RUNNING:
                    // 系统重启，之前运行的进程设置为就绪
                    process.setState(Process.ProcessState.READY);
                    readyQueue.offer(process);
                    break;
                default:
                    break;
            }
        }
    }
    
    /**
     * 添加新进程
     */
    public Process addProcess(Process process) {
        // 设置创建时间和最后更新时间
        LocalDateTime now = LocalDateTime.now();
        process.setCreateTime(now);
        process.setLastUpdateTime(now);
        process.setState(Process.ProcessState.NEW);
        
        // 先保存进程到数据库以获取ID
        process = processRepository.save(process);
        
        // 检查内存是否足够
        int freeMemory = memoryManager.getFreeMemorySize();
        if (freeMemory < process.getMemorySize()) {
            // 内存不足，尝试交换出其他进程
            int freedMemory = processSwapper.autoSwapOut(process.getMemorySize(), getAllInMemoryProcesses());
            
            // 如果释放后还是不够
            if (freedMemory + freeMemory < process.getMemorySize()) {
                // 无法分配内存，设置进程为等待状态
                process.setInMemory(false);
                process.setState(Process.ProcessState.WAITING);
                
                waitingProcesses.add(process);
                allProcesses.put(process.getId(), process);
                
                // 更新进程状态到数据库
                processRepository.save(process);
                
                // 记录状态转换：NEW -> WAITING（内存不足）
                recordTransition(process, Process.ProcessState.NEW, Process.ProcessState.WAITING, "内存不足");
                
                return process;
            }
        }
        
        // 为进程分配内存
        boolean allocated = memoryManager.allocateMemoryForProcess(process, process.getMemorySize());
        if (!allocated) {
            // 内存分配失败，设置进程为等待状态
            process.setInMemory(false);
            process.setState(Process.ProcessState.WAITING);
            
            waitingProcesses.add(process);
            allProcesses.put(process.getId(), process);
            
            // 更新进程状态到数据库
            processRepository.save(process);
            
            // 记录状态转换：NEW -> WAITING（内存分配失败）
            recordTransition(process, Process.ProcessState.NEW, Process.ProcessState.WAITING, "内存分配失败");
            
            return process;
        }
        
        // 内存分配成功，将进程设置为就绪状态
        process.setState(Process.ProcessState.READY);
        
        // 检查是否需要中断当前运行的进程
        if (currentProcess != null && process.getPriority() > currentProcess.getPriority()) {
            logger.info("高优先级进程 {} (优先级: {}) 到达，中断当前进程 {} (优先级: {})", 
                process.getId(), process.getPriority(), 
                currentProcess.getId(), currentProcess.getPriority());
                
            // 将当前进程设置为就绪状态
            Process.ProcessState oldState = currentProcess.getState();
            currentProcess.setState(Process.ProcessState.READY);
            currentProcess.setLastUpdateTime(LocalDateTime.now());
            readyQueue.offer(currentProcess);
            
            // 保存到数据库
            processRepository.save(currentProcess);
            
            // 记录状态转换
            recordTransition(currentProcess, oldState, Process.ProcessState.READY, "被高优先级进程中断");
            
            // 清空当前进程
            currentProcess = null;
        }
        
        readyQueue.offer(process);
        allProcesses.put(process.getId(), process);
        
        // 更新进程状态到数据库
        processRepository.save(process);
        
        // 记录状态转换：NEW -> READY
        recordTransition(process, Process.ProcessState.NEW, Process.ProcessState.READY, "进程创建并分配内存成功");
        
        // 自动调度
        autoSchedule();
        
        return process;
    }
    
    /**
     * 生成临时ID
     * 在实际生产环境中，应该使用持久化的ID生成策略
     */
    private Long generateTempId() {
        // 简单实现：获取当前时间戳作为ID
        // 在多线程环境下可能会有冲突，实际使用中应该用更可靠的ID生成方式
        Long newId = System.currentTimeMillis();
        
        // 确保ID不会重复
        while (allProcesses.containsKey(newId)) {
            newId++;
        }
        
        return newId;
    }
    
    /**
     * 调度进程
     */
    public Process schedule() {
        // 如果当前有运行进程，将其设置为就绪状态
        if (currentProcess != null) {
            if (currentProcess.getState() == Process.ProcessState.RUNNING) {
                Process.ProcessState oldState = currentProcess.getState();
                currentProcess.setState(Process.ProcessState.READY);
                currentProcess.setLastUpdateTime(LocalDateTime.now());
                readyQueue.offer(currentProcess);
                processRepository.save(currentProcess);
                recordTransition(currentProcess, oldState, Process.ProcessState.READY, "时间片用完");
            }
            currentProcess = null;
        }
        
        // 从就绪队列获取最高优先级进程
        currentProcess = readyQueue.poll();
        if (currentProcess != null) {
            // 如果进程不在内存中，先加载回内存
            if (!currentProcess.isInMemory()) {
                boolean swappedIn = processSwapper.swapIn(currentProcess);
                if (!swappedIn) {
                    // 交换失败，放回队列末尾
                    readyQueue.offer(currentProcess);
                    currentProcess = null;
                    logger.info("进程 {} 加载到内存失败，放回就绪队列", currentProcess.getId());
                    return null;
                }
            }
            
            // 设置为运行状态
            Process.ProcessState oldState = currentProcess.getState();
            currentProcess.setState(Process.ProcessState.RUNNING);
            currentProcess.setLastUpdateTime(LocalDateTime.now());
            processRepository.save(currentProcess);
            recordTransition(currentProcess, oldState, Process.ProcessState.RUNNING, "调度执行");
            
            logger.info("调度进程 {} 执行，优先级: {}", currentProcess.getId(), currentProcess.getPriority());
        } else {
            logger.info("就绪队列为空，没有进程可调度");
        }
        
        // 打印每个队列的进程号
        logger.info("就绪队列中的进程: {}", readyQueue.stream().map(Process::getId).collect(Collectors.toList()));
        logger.info("等待队列中的进程: {}", waitingProcesses.stream().map(Process::getId).collect(Collectors.toList()));
        logger.info("终止队列中的进程: {}", terminatedProcesses.stream().map(Process::getId).collect(Collectors.toList()));
        
        return currentProcess;
    }
    
    /**
     * 阻塞进程
     */
    public void blockProcess(Process process) {
        if (process == null) {
            return;
        }
        
        Process.ProcessState oldState = process.getState();
        
        if (process.equals(currentProcess)) {
            currentProcess = null;
        } else {
            // 如果在就绪队列中，需要移除
            readyQueue.remove(process);
        }
        
        // 设置为等待状态
        process.setState(Process.ProcessState.WAITING);
        process.setLastUpdateTime(LocalDateTime.now());
        waitingProcesses.add(process);
        
        // 保存到数据库
        processRepository.save(process);
        
        // 记录状态转换
        recordTransition(process, oldState, Process.ProcessState.WAITING, "主动阻塞");
    }
    
    /**
     * 唤醒等待进程
     */
    public void wakeUpProcess(Process process) {
        if (process == null || process.getState() != Process.ProcessState.WAITING) {
            return;
        }
        
        // 从等待队列移除
        waitingProcesses.remove(process);
        
        // 如果进程不在内存中，需要先加载回内存
        if (!process.isInMemory()) {
            boolean swappedIn = processSwapper.swapIn(process);
            if (!swappedIn) {
                // 交换失败，仍保持等待状态
                waitingProcesses.add(process);
                return;
            }
        }
        
        // 设置为就绪状态
        Process.ProcessState oldState = process.getState();
        process.setState(Process.ProcessState.READY);
        process.setLastUpdateTime(LocalDateTime.now());
        readyQueue.offer(process);
        
        // 保存到数据库
        processRepository.save(process);
        
        // 记录状态转换
        recordTransition(process, oldState, Process.ProcessState.READY, "进程唤醒");
        
        // 自动调度
        autoSchedule();
    }
    
    /**
     * 终止进程
     */
    public void terminateProcess(Process process) {
        if (process == null) return;
        
        // 记录终止前的状态
        Process.ProcessState oldState = process.getState();
        
        // 从所有队列中移除进程
        readyQueue.remove(process);
        waitingProcesses.remove(process);
        terminatedProcesses.remove(process); // 确保不在终止队列中
        
        // 如果进程是当前运行进程，清除当前进程
        if (currentProcess != null && process.getId().equals(currentProcess.getId())) {
            currentProcess = null;
            logger.info("清除当前运行进程: {}", process.getId());
        }
        
        // 设置进程状态为终止
        process.setState(Process.ProcessState.TERMINATED);
        process.setLastUpdateTime(LocalDateTime.now());
        
        // 添加到终止队列
        terminatedProcesses.add(process);
        
        // 释放进程占用的内存
        if (process.isInMemory()) {
            memoryManager.freeMemoryForProcess(process);
            process.setInMemory(false);
            logger.info("释放进程 {} 占用的内存: {} MB", process.getId(), process.getMemorySize());
        }
        
        // 保存到数据库
        processRepository.save(process);
        
        // 记录状态转换
        recordTransition(process, oldState, Process.ProcessState.TERMINATED, "进程终止");
        
        logger.info("进程 {} 已终止", process.getId());
        
        // 自动调度下一个进程
        autoSchedule();
    }
    
    /**
     * 删除进程
     */
    public void deleteProcess(Long processId) {
        Process process = allProcesses.remove(processId);
        if (process != null) {
            // 从相应队列中移除
            if (process.equals(currentProcess)) {
                currentProcess = null;
            } else if (process.getState() == Process.ProcessState.READY) {
                readyQueue.remove(process);
            } else if (process.getState() == Process.ProcessState.WAITING) {
                waitingProcesses.remove(process);
            } else if (process.getState() == Process.ProcessState.TERMINATED) {
                terminatedProcesses.remove(process);
            }
            
            // 释放进程占用的内存
            memoryManager.freeMemoryForProcess(process);
            
            // 从数据库中删除
            processRepository.deleteById(processId);
        }
    }
    
    /**
     * 更新进程状态
     */
    public void updateProcessState(Long processId, Process.ProcessState newState) {
        Process process = allProcesses.get(processId);
        if (process == null) {
            return;
        }
        
        Process.ProcessState oldState = process.getState();
        
        // 如果状态没有变化，直接返回
        if (oldState == newState) {
            return;
        }
        
        // 根据新状态更新进程
        switch (newState) {
            case READY:
                // 确保进程从其他队列中移除
                if (oldState == Process.ProcessState.WAITING) {
                    waitingProcesses.remove(process);
                } else if (oldState == Process.ProcessState.RUNNING) {
                    currentProcess = null;
                } else if (oldState == Process.ProcessState.TERMINATED) {
                    terminatedProcesses.remove(process);
                }
                
                // 确保进程不在就绪队列中
                readyQueue.remove(process);
                
                // 设置新状态并添加到就绪队列
                process.setState(Process.ProcessState.READY);
                readyQueue.offer(process);
                break;
            
            case WAITING:
                // 确保进程从其他队列中移除
                if (oldState == Process.ProcessState.READY) {
                    readyQueue.remove(process);
                } else if (oldState == Process.ProcessState.RUNNING) {
                    currentProcess = null;
                } else if (oldState == Process.ProcessState.TERMINATED) {
                    terminatedProcesses.remove(process);
                }
                
                // 确保进程不在等待队列中
                waitingProcesses.remove(process);
                
                // 设置新状态并添加到等待队列
                process.setState(Process.ProcessState.WAITING);
                waitingProcesses.add(process);
                break;
            
            case RUNNING:
                if (currentProcess != null && currentProcess.getState() == Process.ProcessState.RUNNING) {
                    // 将当前运行进程设置为就绪
                    currentProcess.setState(Process.ProcessState.READY);
                    readyQueue.offer(currentProcess);
                }
                
                // 确保进程从其他队列中移除
                if (oldState == Process.ProcessState.READY) {
                    readyQueue.remove(process);
                } else if (oldState == Process.ProcessState.WAITING) {
                    waitingProcesses.remove(process);
                } else if (oldState == Process.ProcessState.TERMINATED) {
                    terminatedProcesses.remove(process);
                }
                
                // 设置新状态并设置为当前运行进程
                process.setState(Process.ProcessState.RUNNING);
                currentProcess = process;
                break;
            
            case TERMINATED:
                // 确保进程从其他队列中移除
                if (oldState == Process.ProcessState.READY) {
                    readyQueue.remove(process);
                } else if (oldState == Process.ProcessState.WAITING) {
                    waitingProcesses.remove(process);
                } else if (oldState == Process.ProcessState.RUNNING) {
                    currentProcess = null;
                }
                
                // 确保进程不在终止队列中
                terminatedProcesses.remove(process);
                
                // 设置新状态并添加到终止队列
                process.setState(Process.ProcessState.TERMINATED);
                terminatedProcesses.add(process);
                
                // 释放进程占用的内存
                memoryManager.freeMemoryForProcess(process);
                break;
            
            default:
                break;
        }
        
        // 更新进程的最后更新时间
        process.setLastUpdateTime(LocalDateTime.now());
        
        // 更新进程到数据库
        processRepository.save(process);
        
        // 记录状态转换
        recordTransition(process, oldState, newState, "状态更新");
    }
    
    /**
     * 获取就绪进程列表
     */
    public List<Process> getReadyProcesses() {
        // 从数据库获取最新状态
        List<Process> processes = processRepository.findAll();
        List<Process> readyProcesses = new ArrayList<>();
        
        // 更新内存中的就绪队列
        readyQueue.clear();
        for (Process process : processes) {
            if (process.getState() == Process.ProcessState.READY) {
                // 检查进程是否已经在就绪队列中
                if (!readyQueue.contains(process)) {
                    readyQueue.offer(process);
                }
                readyProcesses.add(process);
            }
        }
        
        return readyProcesses;
    }
    
    /**
     * 获取等待进程列表
     */
    public List<Process> getWaitingProcesses() {
        // 从数据库获取最新状态
        List<Process> processes = processRepository.findAll();
        List<Process> waitingProcesses = new ArrayList<>();
        
        // 更新内存中的等待队列
        this.waitingProcesses.clear();
        for (Process process : processes) {
            if (process.getState() == Process.ProcessState.WAITING) {
                this.waitingProcesses.add(process);
                waitingProcesses.add(process);
            }
        }
        
        return waitingProcesses;
    }
    
    /**
     * 获取已终止进程列表
     */
    public List<Process> getTerminatedProcesses() {
        // 从数据库获取最新状态
        List<Process> processes = processRepository.findAll();
        List<Process> terminatedProcesses = new ArrayList<>();
        
        // 更新内存中的终止队列
        this.terminatedProcesses.clear();
        for (Process process : processes) {
            if (process.getState() == Process.ProcessState.TERMINATED) {
                this.terminatedProcesses.add(process);
                terminatedProcesses.add(process);
            }
        }
        
        return terminatedProcesses;
    }
    
    /**
     * 获取当前运行的进程
     */
    public Process getCurrentProcess() {
        return currentProcess;
    }
    
    /**
     * 获取所有进程
     */
    public List<Process> getAllProcesses() {
        // 从数据库获取最新进程列表
        List<Process> processes = processRepository.findAll();
        
        // 更新内存中的进程映射
        allProcesses.clear();
        for (Process process : processes) {
            allProcesses.put(process.getId(), process);
        }
        
        return processes;
    }
    
    /**
     * 获取内存中的所有进程
     */
    public List<Process> getAllInMemoryProcesses() {
        return allProcesses.values().stream()
                .filter(Process::isInMemory)
                .collect(Collectors.toList());
    }
    
    /**
     * 记录进程状态转换
     */
    private void recordTransition(Process process, Process.ProcessState fromState, Process.ProcessState toState, String reason) {
        ProcessTransition transition = new ProcessTransition(process, fromState, toState, reason);
        transitionRepository.save(transition);
    }
    
    /**
     * 获取进程状态转换历史
     */
    public List<ProcessTransition> getProcessTransitionHistory(Long processId) {
        return transitionRepository.findByProcessId(processId);
    }
    
    public ProcessRepository getProcessRepository() {
        return processRepository;
    }
    
    /**
     * 触发进程中断
     */
    public void interruptProcess(Long processId, String reason) {
        Process process = allProcesses.get(processId);
        if (process == null) {
            return;
        }
        
        // 检查进程是否处于运行状态
        if (process.getState() != Process.ProcessState.RUNNING) {
            logger.info("进程 {} 不在运行状态，无法触发中断", processId);
            return;
        }
        
        // 触发进程中断
        interruptManager.triggerInterrupt(1, InterruptType.valueOf(reason), processId, reason);
        
        // 根据中断类型处理进程状态
        switch (reason) {
            case "PROCESS":
                if (process.equals(currentProcess)) {
                    Process.ProcessState oldState = process.getState();
                    process.setState(Process.ProcessState.READY);
                    process.setLastUpdateTime(LocalDateTime.now());
                    readyQueue.offer(process);
                    currentProcess = null;
                    processRepository.save(process);
                    recordTransition(process, oldState, Process.ProcessState.READY, "进程中断");
                    // 检查是否需要抢占当前进程
                    if (!readyQueue.isEmpty() && readyQueue.peek().getPriority() > process.getPriority()) {
                        logger.info("高优先级进程 {} 进入就绪队列，触发抢占", readyQueue.peek().getId());
                        autoSchedule();
                    }
                }
                break;
            case "IO":
            case "DEVICE":
                if (process.equals(currentProcess)) {
                    // 确保进程不在就绪队列中
                    readyQueue.remove(process);
                    
                    Process.ProcessState oldState = process.getState();
                    process.setState(Process.ProcessState.WAITING);
                    process.setLastUpdateTime(LocalDateTime.now());
                    process.setWaitingReason(reason.equals("IO") ? "等待IO完成" : "等待设备");
                    waitingProcesses.add(process);
                    currentProcess = null;
                    processRepository.save(process);
                    recordTransition(process, oldState, Process.ProcessState.WAITING, process.getWaitingReason());
                    
                    // 启动一个异步任务，模拟IO/设备操作完成
                    CompletableFuture.runAsync(() -> {
                        try {
                            // 随机等待5-10秒
                            Thread.sleep(5000 + (long)(Math.random() * 5000));
                            // 将进程从等待队列移到就绪队列
                            if (waitingProcesses.remove(process)) {
                                // 确保进程不在就绪队列中
                                readyQueue.remove(process);
                                
                                process.setState(Process.ProcessState.READY);
                                process.setLastUpdateTime(LocalDateTime.now());
                                process.setWaitingReason(null);
                                readyQueue.offer(process);
                                processRepository.save(process);
                                recordTransition(process, Process.ProcessState.WAITING, Process.ProcessState.READY, 
                                    reason.equals("IO") ? "IO操作完成" : "设备操作完成");
                                logger.info("进程 {} {}操作完成，进入就绪队列", processId, reason.equals("IO") ? "IO" : "设备");
                                // 检查是否需要抢占当前进程
                                if (!readyQueue.isEmpty() && readyQueue.peek().getPriority() > process.getPriority()) {
                                    logger.info("高优先级进程 {} 进入就绪队列，触发抢占", readyQueue.peek().getId());
                                    autoSchedule();
                                }
                            }
                        } catch (InterruptedException e) {
                            logger.error("等待IO/设备操作完成时被中断", e);
                        }
                    });
                }
                break;
            case "ERROR":
                if (process.equals(currentProcess)) {
                    terminateProcess(process);
                }
                break;
            case "CLOCK":
                // 时钟中断一般不直接改变进程状态
                break;
            case "OTHER":
                if (process.equals(currentProcess)) {
                    Process.ProcessState oldState = process.getState();
                    process.setState(Process.ProcessState.READY);
                    process.setLastUpdateTime(LocalDateTime.now());
                    readyQueue.offer(process);
                    currentProcess = null;
                    processRepository.save(process);
                    recordTransition(process, oldState, Process.ProcessState.READY, "其他中断");
                    // 检查是否需要抢占当前进程
                    if (!readyQueue.isEmpty() && readyQueue.peek().getPriority() > process.getPriority()) {
                        logger.info("高优先级进程 {} 进入就绪队列，触发抢占", readyQueue.peek().getId());
                        autoSchedule();
                    }
                }
                break;
        }
    }
    
    /**
     * 处理进程中断
     */
    public void handleProcessInterrupt(Long processId, String reason) {
        Process process = allProcesses.get(processId);
        if (process == null) {
            return;
        }
        switch (reason) {
            case "PROCESS":
                if (process.equals(currentProcess)) {
                    Process.ProcessState oldState = process.getState();
                    process.setState(Process.ProcessState.READY);
                    process.setLastUpdateTime(LocalDateTime.now());
                    readyQueue.offer(process);
                    currentProcess = null;
                    processRepository.save(process);
                    recordTransition(process, oldState, Process.ProcessState.READY, "进程中断");
                    autoSchedule();
                }
                break;
            case "IO":
            case "DEVICE":
                if (process.equals(currentProcess)) {
                    Process.ProcessState oldState = process.getState();
                    process.setState(Process.ProcessState.WAITING);
                    process.setLastUpdateTime(LocalDateTime.now());
                    waitingProcesses.add(process);
                    currentProcess = null;
                    processRepository.save(process);
                    recordTransition(process, oldState, Process.ProcessState.WAITING, reason.equals("IO") ? "等待IO完成" : "等待设备");
                    autoSchedule();
                }
                break;
            case "ERROR":
                if (process.equals(currentProcess)) {
                    terminateProcess(process);
                }
                break;
            case "CLOCK":
                // 时钟中断一般不直接改变进程状态
                break;
            case "OTHER":
                if (process.equals(currentProcess)) {
                    Process.ProcessState oldState = process.getState();
                    process.setState(Process.ProcessState.READY);
                    process.setLastUpdateTime(LocalDateTime.now());
                    readyQueue.offer(process);
                    currentProcess = null;
                    processRepository.save(process);
                    recordTransition(process, oldState, Process.ProcessState.READY, "其他中断");
                    autoSchedule();
                }
                break;
        }
    }

    /**
     * 根据ID获取进程
     */
    public Process getProcessById(Long id) {
        return allProcesses.get(id);
    }

    /**
     * 阻塞进程
     * @param processId 进程ID
     * @param reason 阻塞原因
     */
    public void blockProcess(Long processId, String reason) {
        Process process = getProcessById(processId);
        if (process != null) {
            process.setState(Process.ProcessState.WAITING);
            process.setWaitingReason(reason);
            process.setLastUpdateTime(LocalDateTime.now());
            processRepository.save(process);
            
            // 记录状态转换
            recordTransition(process, Process.ProcessState.RUNNING, Process.ProcessState.WAITING, reason);
        }
    }

    /**
     * 唤醒进程
     * @param processId 进程ID
     */
    public void wakeupProcess(Long processId) {
        Process process = getProcessById(processId);
        if (process != null && process.getState() == Process.ProcessState.WAITING) {
            process.setState(Process.ProcessState.READY);
            process.setWaitingReason(null);
            process.setLastUpdateTime(LocalDateTime.now());
            processRepository.save(process);
            
            // 记录状态转换
            recordTransition(process, Process.ProcessState.WAITING, Process.ProcessState.READY, "设备可用，进程被唤醒");
            
            // 将进程加入就绪队列
            readyQueue.add(process);
        }
    }

    @EventListener
    public void handleProcessSchedulingEvent(ProcessSchedulingEvent event) {
        // 立即调度下一个进程
        if (currentProcess == null) {
            schedule();
        }
    }

    @EventListener
    public void handleProcessTerminationEvent(ProcessTerminationEvent event) {
        Long processId = event.getProcessId();
        Process process = getProcessById(processId);
        if (process != null) {
            // 如果进程是当前运行进程，需要将其从运行状态移除
            if (process.equals(currentProcess)) {
                currentProcess = null;
            }
            // 如果进程在就绪队列中，需要移除
            readyQueue.remove(process);
            // 如果进程在等待队列中，需要移除
            waitingProcesses.remove(process);
            
            // 设置进程状态为终止
            process.setState(Process.ProcessState.TERMINATED);
            process.setLastUpdateTime(LocalDateTime.now());
            terminatedProcesses.add(process);
            
            // 释放进程占用的内存
            memoryManager.freeMemoryForProcess(process);
            
            // 保存到数据库
            processRepository.save(process);
            
            // 记录状态转换
            recordTransition(process, Process.ProcessState.RUNNING, Process.ProcessState.TERMINATED, "进程被终止");
            
            logger.info("进程 {} 已终止", processId);
            
            // 自动调度下一个进程
            autoSchedule();
        }
    }

    @EventListener
    public void handleProcessWaitingEvent(ProcessWaitingEvent event) {
        Long processId = event.getProcessId();
        Process process = getProcessById(processId);
        if (process != null) {
            logger.info("处理进程等待事件 - 进程ID: {}, 当前状态: {}", processId, process.getState());
            
            // 如果进程是当前运行进程，需要将其从运行状态移除
            if (process.equals(currentProcess)) {
                currentProcess = null;
                logger.info("进程 {} 从运行状态移除", processId);
            }
            
            // 如果进程在就绪队列中，需要移除
            if (readyQueue.remove(process)) {
                logger.info("进程 {} 从就绪队列移除", processId);
            }
            
            // 如果进程已经在等待队列中，不需要重复添加
            if (waitingProcesses.contains(process)) {
                logger.info("进程 {} 已在等待队列中", processId);
                return;
            }
            
            // 设置进程状态为等待
            Process.ProcessState oldState = process.getState();
            process.setState(Process.ProcessState.WAITING);
            process.setLastUpdateTime(LocalDateTime.now());
            waitingProcesses.add(process);
            processRepository.save(process);
            
            // 记录状态转换
            recordTransition(process, oldState, Process.ProcessState.WAITING, event.getReason());
            
            logger.info("进程 {} 进入等待状态，原因: {}", processId, event.getReason());
            
            // 立即调度下一个进程
            schedule();
        }
    }

    @EventListener
    public void handleProcessReadyEvent(ProcessReadyEvent event) {
        Long processId = event.getProcessId();
        Process process = getProcessById(processId);
        if (process != null) {
            // 如果进程在等待队列中，需要移除
            waitingProcesses.remove(process);
            
            // 如果进程是当前运行进程，需要先将其从运行状态移除
            if (process.equals(currentProcess)) {
                currentProcess = null;
            }
            
            // 设置进程状态为就绪
            Process.ProcessState oldState = process.getState();
            process.setState(Process.ProcessState.READY);
            process.setLastUpdateTime(LocalDateTime.now());
            readyQueue.offer(process);
            processRepository.save(process);
            
            // 记录状态转换
            recordTransition(process, oldState, Process.ProcessState.READY, "进程就绪");
            
            // 检查是否需要立即调度
            if (currentProcess == null || process.getPriority() > currentProcess.getPriority()) {
                // 如果当前没有运行进程，或者新就绪进程优先级更高，立即调度
                schedule();
            }
        }
    }

    @Scheduled(fixedRate = 5000) // 每5秒执行一次
    public void printQueueStatus() {
        // 检查当前运行进程是否有效
        if (currentProcess != null && currentProcess.getState() != Process.ProcessState.RUNNING) {
            logger.info("发现无效的当前运行进程，正在清除: {}", currentProcess.getId());
            currentProcess = null;
        }
        
        // 只打印队列状态，不进行调度
        logger.info("当前运行进程: {}", currentProcess != null ? currentProcess.getId() : "无");
        logger.info("就绪队列中的进程: {}", readyQueue.stream().map(Process::getId).collect(Collectors.toList()));
        logger.info("等待队列中的进程: {}", waitingProcesses.stream().map(Process::getId).collect(Collectors.toList()));
        logger.info("终止队列中的进程: {}", terminatedProcesses.stream().map(Process::getId).collect(Collectors.toList()));
    }

    @Scheduled(fixedRate = 50) // 每0.05秒执行一次
    public void checkAndSchedule() {
        // 如果当前没有运行进程，且就绪队列不为空，则进行调度
        if (currentProcess == null && !readyQueue.isEmpty()) {
            logger.info("定时检查：当前无运行进程，就绪队列不为空，开始调度");
            schedule();
        }
    }

    /**
     * 设置当前运行进程（仅用于测试）
     */
    public void setCurrentProcess(Process process) {
        this.currentProcess = process;
    }

    /**
     * 获取就绪队列
     */
    public PriorityBlockingQueue<Process> getReadyQueue() {
        return readyQueue;
    }

    /**
     * 更新进程优先级
     */
    public void updateProcessPriority(Long processId, Integer priority) {
        Process process = getProcessById(processId);
        if (process != null) {
            // 保存旧状态
            Process.ProcessState oldState = process.getState();
            
            // 如果进程在就绪队列中，需要先移除
            if (oldState == Process.ProcessState.READY) {
                readyQueue.remove(process);
            }
            
            // 更新优先级
            process.setPriority(priority);
            process.setLastUpdateTime(LocalDateTime.now());
            
            // 如果进程之前在就绪状态，重新加入就绪队列
            if (oldState == Process.ProcessState.READY) {
                readyQueue.offer(process);
            }
            
            // 保存到数据库
            processRepository.save(process);
        }
    }
} 