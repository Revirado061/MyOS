package com.group.myos.memory;

import com.group.myos.memory.model.MemoryBlock;
import com.group.myos.memory.model.PageEntry;
import com.group.myos.process.model.Process;

import java.util.List;

public interface MemoryManager {
    // 内存分配与释放
    boolean allocateMemoryForProcess(Process process, int size);
    void freeMemoryForProcess(Process process);

    // 获取内存空闲空间大小
    int getFreeMemorySize();
    // 获取内存空闲块列表
    List<MemoryBlock> getFreeBlocks();
    // 获取内存使用率
    double getMemoryUsage();
    // 获取每个内存块状态
    long[] getMemoryStatus();
    
    // 分页管理
    boolean allocatePage(Process process, int virtualPageNumber);
    void freePage(Process process, int virtualPageNumber);
    PageEntry getPageEntry(Process process, int virtualPageNumber);
    boolean isPageValid(Process process, int virtualPageNumber);
    
    // 内存访问
    boolean writeMemory(Process process, int virtualAddress, Object content);
    Object readMemory(Process process, int virtualAddress);
    
    // 页面置换
    void handlePageFault(Process process, int virtualAddress);
    void swapOutPage(Process process, int virtualPageNumber);
    void swapInPage(Process process, int virtualPageNumber);
}