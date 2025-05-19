package com.group.myos.process;

import com.group.myos.process.model.Process;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProcessSwapper {
    private final List<Process> swappedProcesses = new ArrayList<>();

    public List<Process> getSwappedProcesses() {
        return swappedProcesses;
    }

    public boolean swapIn(Process process) {
        if (swappedProcesses.remove(process)) {
            process.setInMemory(true);
            return true;
        }
        return false;
    }

    public boolean swapOut(Process process) {
        if (process.isInMemory()) {
            swappedProcesses.add(process);
            process.setInMemory(false);
            return true;
        }
        return false;
    }

    public int autoSwapOut(int requiredMemory, List<Process> inMemoryProcesses) {
        int freedMemory = 0;
        for (Process process : inMemoryProcesses) {
            if (freedMemory >= requiredMemory) break;
            if (swapOut(process)) {
                freedMemory += process.getMemorySize();
            }
        }
        return freedMemory;
    }
} 