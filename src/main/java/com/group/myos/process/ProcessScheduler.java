package com.group.myos.process;

import com.group.myos.memory.MemoryManager;
import com.group.myos.process.model.Process;
import com.group.myos.process.model.ProcessTransition;
import com.group.myos.process.repository.ProcessRepository;
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
    private final ProcessRepository processRepository;
    
    public ProcessScheduler(
            MemoryManager memoryManager, 
            ProcessSwapper processSwapper,
            ProcessTransitionRepository transitionRepository,
            ProcessRepository processRepository) {
        this.memoryManager = memoryManager;
        this.processSwapper = processSwapper;
        this.transitionRepository = transitionRepository;
        this.processRepository = processRepository;
        this.readyQueue = new PriorityBlockingQueue<>(100, 
            Comparator.comparing(Process::getPriority).reversed());
        this.allProcesses = new ConcurrentHashMap<>();
        this.waitingProcesses = new ArrayList<>();
        this.terminatedProcesses = new ArrayList<>();
        
        // 初始化时从数据库加载所有进程
        loadProcessesFromDatabase();
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
        
        readyQueue.offer(process);
        allProcesses.put(process.getId(), process);
        
        // 更新进程状态到数据库
        processRepository.save(process);
        
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
                
                // 保存到数据库
                processRepository.save(currentProcess);
                
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
            
            // 保存到数据库
            processRepository.save(currentProcess);
            
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
    }
    
    /**
     * 终止进程
     */
    public void terminateProcess(Process process) {
        if (process == null) {
            return;
        }
        
        Process.ProcessState oldState = process.getState();
        
        // 从相应队列中移除
        if (process.equals(currentProcess)) {
            currentProcess = null;
        } else if (process.getState() == Process.ProcessState.READY) {
            readyQueue.remove(process);
        } else if (process.getState() == Process.ProcessState.WAITING) {
            waitingProcesses.remove(process);
        }
        
        // 设置为终止状态
        process.setState(Process.ProcessState.TERMINATED);
        process.setLastUpdateTime(LocalDateTime.now());
        terminatedProcesses.add(process);
        
        // 释放进程占用的内存
        memoryManager.freeMemoryForProcess(process);
        
        // 保存到数据库
        processRepository.save(process);
        
        // 记录状态转换
        recordTransition(process, oldState, Process.ProcessState.TERMINATED, "进程终止");
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
                if (oldState == Process.ProcessState.WAITING) {
                    waitingProcesses.remove(process);
                } else if (oldState == Process.ProcessState.RUNNING) {
                    currentProcess = null;
                } else if (oldState == Process.ProcessState.TERMINATED) {
                    terminatedProcesses.remove(process);
                }
                process.setState(Process.ProcessState.READY);
                readyQueue.offer(process);
                break;
            
            case WAITING:
                if (oldState == Process.ProcessState.READY) {
                    readyQueue.remove(process);
                } else if (oldState == Process.ProcessState.RUNNING) {
                    currentProcess = null;
                } else if (oldState == Process.ProcessState.TERMINATED) {
                    terminatedProcesses.remove(process);
                }
                process.setState(Process.ProcessState.WAITING);
                waitingProcesses.add(process);
                break;
            
            case RUNNING:
                if (currentProcess != null && currentProcess.getState() == Process.ProcessState.RUNNING) {
                    // 将当前运行进程设置为就绪
                    currentProcess.setState(Process.ProcessState.READY);
                    readyQueue.offer(currentProcess);
                }
                
                if (oldState == Process.ProcessState.READY) {
                    readyQueue.remove(process);
                } else if (oldState == Process.ProcessState.WAITING) {
                    waitingProcesses.remove(process);
                } else if (oldState == Process.ProcessState.TERMINATED) {
                    terminatedProcesses.remove(process);
                }
                
                process.setState(Process.ProcessState.RUNNING);
                currentProcess = process;
                break;
            
            case TERMINATED:
                if (oldState == Process.ProcessState.READY) {
                    readyQueue.remove(process);
                } else if (oldState == Process.ProcessState.WAITING) {
                    waitingProcesses.remove(process);
                } else if (oldState == Process.ProcessState.RUNNING) {
                    currentProcess = null;
                }
                
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
} 