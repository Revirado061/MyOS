package com.group.myos.process;

import com.group.myos.memory.MemoryManager;
import com.group.myos.process.model.Process;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProcessSwapper {

    private final MemoryManager memoryManager;

    private static final String SWAP_DIRECTORY = "swap";
    private final List<Process> swappedProcesses = new ArrayList<>();

    public ProcessSwapper(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        
        // 创建交换目录
        File swapDir = new File(SWAP_DIRECTORY);
        if (!swapDir.exists()) {
            swapDir.mkdirs();
        }
    }

    /**
     * 将进程从内存交换到磁盘（Swap Out）
     * @param process 要交换出去的进程
     * @return 交换是否成功
     */
    public boolean swapOut(Process process) {
        if (!process.isInMemory() || process.getState() == Process.ProcessState.TERMINATED) {
            return false; // 进程不在内存中或已终止，无法交换
        }

        try {
            // 生成交换文件路径
            String swapFileName = String.format("%s/process_%d.swap", SWAP_DIRECTORY, process.getId());
            Path swapFilePath = Paths.get(swapFileName);

            // 模拟进程数据写入交换文件
            String processData = String.format("Process ID: %d\nName: %s\nPriority: %d\nState: %s\nMemory Size: %d",
                    process.getId(), process.getName(), process.getPriority(), process.getState(), process.getMemorySize());
            Files.write(swapFilePath, processData.getBytes());

            // 更新进程状态
            process.setInMemory(false);
            process.setSwapFilePath(swapFileName);

            // 释放进程占用的内存
            memoryManager.freeMemoryForProcess(process);

            // 添加到已交换进程列表
            swappedProcesses.add(process);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将进程从磁盘加载回内存（Swap In）
     * @param process 要加载回内存的进程
     * @return 加载是否成功
     */
    public boolean swapIn(Process process) {
        if (process.isInMemory() || process.getState() == Process.ProcessState.TERMINATED) {
            return false; // 进程已在内存中或已终止，无法加载
        }

        // 检查内存是否足够
        int freeMemory = memoryManager.getFreeMemorySize();
        if (freeMemory < process.getMemorySize()) {
            return false; // 内存不足，无法加载
        }

        try {
            // 为进程分配内存
            boolean allocated = memoryManager.allocateMemoryForProcess(process, process.getMemorySize());
            if (!allocated) {
                return false; // 内存分配失败
            }

            // 从磁盘加载进程数据（这里只是模拟）
            Path swapFilePath = Paths.get(process.getSwapFilePath());
            if (Files.exists(swapFilePath)) {
                // 实际应用中会从文件加载进程数据到内存
                // 这里只是模拟删除交换文件
                Files.delete(swapFilePath);
            }

            // 更新进程状态
            process.setInMemory(true);
            process.setSwapFilePath(null);

            // 从已交换进程列表中移除
            swappedProcesses.remove(process);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 自动选择合适的进程进行交换
     * @param requiredMemory 需要的内存大小
     * @param inMemoryProcesses 内存中的进程列表
     * @return 交换后释放的内存大小
     */
    public int autoSwapOut(int requiredMemory, List<Process> inMemoryProcesses) {
        int freedMemory = 0;
        int freeMemory = memoryManager.getFreeMemorySize();
        
        if (freeMemory >= requiredMemory) {
            return freedMemory; // 已有足够内存，不需要交换
        }

        // 获取所有可能被交换的进程（排除运行中进程）
        List<Process> processesToSwap = inMemoryProcesses.stream()
                .filter(p -> p.getState() != Process.ProcessState.RUNNING)
                .sorted(Comparator.comparing(Process::getPriority)) // 按优先级排序，低优先级先交换
                .collect(Collectors.toList());

        // 逐个尝试交换进程，直到释放足够内存
        for (Process process : processesToSwap) {
            if (swapOut(process)) {
                freedMemory += process.getMemorySize();
                freeMemory += process.getMemorySize();
                
                if (freeMemory >= requiredMemory) {
                    break; // 释放了足够的内存
                }
            }
        }

        return freedMemory;
    }

    /**
     * 获取所有已交换出去的进程
     * @return 已交换进程列表
     */
    public List<Process> getSwappedProcesses() {
        return new ArrayList<>(swappedProcesses);
    }
} 