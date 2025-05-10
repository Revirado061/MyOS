package com.group.myos.memory;

import com.group.myos.filesystem.model.File;
import com.group.myos.memory.model.MemoryBlock;
import com.group.myos.process.model.Process;

import java.util.List;
import java.util.Map;

public interface MemoryManager {
    boolean allocateMemoryForProcess(Process process, int size); // 为进程分配内存
    void freeMemoryForProcess(Process process); // 释放进程占用的内存
    boolean allocateMemoryForFile(File file, int size); // 为文件分配内存
    void freeMemoryForFile(File file);  // 释放文件占用的内存

    int getFreeMemorySize(); // 获取当前空闲内存的总大小
    List<MemoryBlock> getFreeBlocks(); // 获取空闲内存块列表
    Map<Process, MemoryBlock> getAllocatedMemory(); // 获取已分配内存的进程映射
    Map<File, MemoryBlock> getAllocatedFileSystemMemory(); // 获取已分配内存的文件映射
}