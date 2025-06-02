package com.group.myos.process;

import com.group.myos.memory.MemoryManager;
import com.group.myos.process.model.Process;
import com.group.myos.process.model.ProcessTransition;
import com.group.myos.process.repository.ProcessTransitionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

@Component
public class ProcessScheduler {
    private final PriorityBlockingQueue<Process> readyQueue;
    private final Map<Long, Process> allProcesses;
    private final List<Process> waitingProcesses;
    private final List<Process> terminatedProcesses;
    private Process currentProcess;
    
    private final MemoryManager memoryManager;
    private final ProcessSwapper processSwapper;
    private final ProcessTransitionRepository transitionRepository;
    
    public ProcessScheduler(
            MemoryManager memoryManager, 
            ProcessSwapper processSwapper,
            ProcessTransitionRepository transitionRepository) {
        this.memoryManager = memoryManager;
        this.processSwapper = processSwapper;
        this.transitionRepository = transitionRepository;
        this.readyQueue = new PriorityBlockingQueue<>(100, 
            Comparator.comparing(Process::getPriority).reversed());
        this.allProcesses = new ConcurrentHashMap<>();
        this.waitingProcesses = new ArrayList<>();
        this.terminatedProcesses = new ArrayList<>();
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
                
                // 记录状态转换：NEW -> WAITING（内存不足）
                if (process.getId() != null) {
                    recordTransition(process, Process.ProcessState.NEW, Process.ProcessState.WAITING, "内存不足");
                }
                
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
            
            // 记录状态转换：NEW -> WAITING（内存分配失败）
            if (process.getId() != null) {
                recordTransition(process, Process.ProcessState.NEW, Process.ProcessState.WAITING, "内存分配失败");
            }
            
            return process;
        }
        
        // 内存分配成功，将进程设置为就绪状态
        process.setState(Process.ProcessState.READY);
        readyQueue.offer(process);
        
        // 防止ID为空导致的NullPointerException
        if (process.getId() == null) {
            // 为进程生成一个临时ID
            Long tempId = generateTempId();
            process.setId(tempId);
        }
        
        allProcesses.put(process.getId(), process);
        
        // 记录状态转换：NEW -> READY
        recordTransition(process, Process.ProcessState.NEW, Process.ProcessState.READY, "进程创建并分配内存成功");
        
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
     * 调度下一个进程执行
     */
    public Process schedule() {
        if (currentProcess != null) {
            if (currentProcess.getState() == Process.ProcessState.RUNNING) {
                // 将当前运行进程设置为就绪状态并放回队列
                Process.ProcessState oldState = currentProcess.getState();
                currentProcess.setState(Process.ProcessState.READY);
                currentProcess.setLastUpdateTime(LocalDateTime.now());
                readyQueue.offer(currentProcess);
                
                // 记录状态转换：RUNNING -> READY
                recordTransition(currentProcess, oldState, Process.ProcessState.READY, "时间片用完");
            }
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
                    return null;
                }
            }
            
            // 设置为运行状态
            Process.ProcessState oldState = currentProcess.getState();
            currentProcess.setState(Process.ProcessState.RUNNING);
            currentProcess.setLastUpdateTime(LocalDateTime.now());
            
            // 记录状态转换：READY -> RUNNING
            recordTransition(currentProcess, oldState, Process.ProcessState.RUNNING, "调度执行");
        }
        
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
        
        // 记录状态转换：oldState -> WAITING
        recordTransition(process, oldState, Process.ProcessState.WAITING, "主动阻塞");
    }
    
    /**
     * 唤醒等待进程
     */
    public void wakeUpProcess(Process process) {
        if (process == null || process.getState() != Process.ProcessState.WAITING) {
            return;
        }
        
        Process.ProcessState oldState = process.getState();
        
        // 从等待列表移除
        waitingProcesses.remove(process);
        
        // 如果进程不在内存中，尝试交换回内存
        if (!process.isInMemory()) {
            int freeMemory = memoryManager.getFreeMemorySize();
            if (freeMemory < process.getMemorySize()) {
                // 内存不足，尝试交换出其他进程
                processSwapper.autoSwapOut(process.getMemorySize(), getAllInMemoryProcesses());
            }
            
            // 尝试加载进程回内存
            boolean swappedIn = processSwapper.swapIn(process);
            if (!swappedIn) {
                // 交换失败，保持等待状态
                waitingProcesses.add(process);
                return;
            }
        }
        
        // 设置为就绪状态
        process.setState(Process.ProcessState.READY);
        process.setLastUpdateTime(LocalDateTime.now());
        readyQueue.offer(process);
        
        // 记录状态转换：WAITING -> READY
        recordTransition(process, oldState, Process.ProcessState.READY, "唤醒");
    }
    
    /**
     * 终止进程
     */
    public void terminateProcess(Process process) {
        if (process == null) {
            return;
        }
        
        Process.ProcessState oldState = process.getState();
        
        if (process.equals(currentProcess)) {
            currentProcess = null;
        } else {
            // 可能在就绪队列或等待队列
            readyQueue.remove(process);
            waitingProcesses.remove(process);
        }
        
        // 释放进程占用的内存（如果在内存中）
        if (process.isInMemory()) {
            memoryManager.freeMemoryForProcess(process);
        } else {
            // 移除交换文件
            try {
                if (process.getSwapFilePath() != null) {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(process.getSwapFilePath()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // 从已交换进程列表移除
            processSwapper.getSwappedProcesses().remove(process);
        }
        
        // 设置为终止状态
        process.setState(Process.ProcessState.TERMINATED);
        process.setLastUpdateTime(LocalDateTime.now());
        terminatedProcesses.add(process);
        
        // 记录状态转换：oldState -> TERMINATED
        recordTransition(process, oldState, Process.ProcessState.TERMINATED, "进程终止");
    }
    
    /**
     * 删除进程（完全删除）
     */
    public void deleteProcess(Long processId) {
        Process process = allProcesses.get(processId);
        if (process != null) {
            // 先终止进程
            terminateProcess(process);
            // 从所有列表中移除
            terminatedProcesses.remove(process);
            allProcesses.remove(processId);
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
        if (oldState == newState) {
            return; // 状态未变化
        }
        
        // 根据新旧状态进行不同处理
        switch (oldState) {
            case RUNNING:
                if (process.equals(currentProcess)) {
                    currentProcess = null;
                }
                break;
            case READY:
                readyQueue.remove(process);
                break;
            case WAITING:
                waitingProcesses.remove(process);
                break;
            case TERMINATED:
                terminatedProcesses.remove(process);
                break;
        }
        
        // 设置新状态
        process.setState(newState);
        process.setLastUpdateTime(LocalDateTime.now());
        
        // 根据新状态添加到相应队列
        switch (newState) {
            case READY:
                readyQueue.offer(process);
                break;
            case RUNNING:
                if (currentProcess != null && currentProcess.getState() == Process.ProcessState.RUNNING) {
                    // 先将当前运行进程设置为就绪
                    currentProcess.setState(Process.ProcessState.READY);
                    readyQueue.offer(currentProcess);
                    
                    // 记录状态转换：RUNNING -> READY（抢占）
                    recordTransition(currentProcess, Process.ProcessState.RUNNING, Process.ProcessState.READY, "被高优先级进程抢占");
                }
                currentProcess = process;
                break;
            case WAITING:
                waitingProcesses.add(process);
                break;
            case TERMINATED:
                terminateProcess(process);
                break;
        }
        
        // 记录状态转换
        recordTransition(process, oldState, newState, "状态更新");
    }
    
    /**
     * 获取就绪进程列表
     */
    public List<Process> getReadyProcesses() {
        return new ArrayList<>(readyQueue);
    }
    
    /**
     * 获取等待进程列表
     */
    public List<Process> getWaitingProcesses() {
        return new ArrayList<>(waitingProcesses);
    }
    
    /**
     * 获取已终止进程列表
     */
    public List<Process> getTerminatedProcesses() {
        return new ArrayList<>(terminatedProcesses);
    }
    
    /**
     * 获取当前运行进程
     */
    public Process getCurrentProcess() {
        return currentProcess;
    }
    
    /**
     * 获取所有进程列表
     */
    public List<Process> getAllProcesses() {
        return new ArrayList<>(allProcesses.values());
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
} 