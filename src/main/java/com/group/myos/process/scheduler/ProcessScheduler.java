package com.group.myos.process.scheduler;

import com.group.myos.process.model.Process;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class ProcessScheduler {
    private final PriorityBlockingQueue<Process> readyQueue;
    private Process currentProcess;
    
    public ProcessScheduler() {
        this.readyQueue = new PriorityBlockingQueue<>(100, 
            Comparator.comparing(Process::getPriority).reversed());
    }
    
    public void addProcess(Process process) {
        process.setState(Process.ProcessState.READY);
        readyQueue.offer(process);
    }
    
    public Process schedule() {
        if (currentProcess != null) {
            currentProcess.setState(Process.ProcessState.READY);
            readyQueue.offer(currentProcess);
        }
        
        currentProcess = readyQueue.poll();
        if (currentProcess != null) {
            currentProcess.setState(Process.ProcessState.RUNNING);
        }
        
        return currentProcess;
    }
    
    public void blockProcess(Process process) {
        if (process.equals(currentProcess)) {
            currentProcess = null;
        }
        process.setState(Process.ProcessState.WAITING);
    }
    
    public void terminateProcess(Process process) {
        if (process.equals(currentProcess)) {
            currentProcess = null;
        }
        process.setState(Process.ProcessState.TERMINATED);
    }
    
    public List<Process> getReadyProcesses() {
        return new ArrayList<>(readyQueue);
    }
    
    public Process getCurrentProcess() {
        return currentProcess;
    }
} 