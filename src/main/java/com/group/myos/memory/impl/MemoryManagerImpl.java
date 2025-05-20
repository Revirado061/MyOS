package com.group.myos.memory.impl;

import com.group.myos.memory.MemoryManager;
import com.group.myos.memory.model.MemoryBlock;
import com.group.myos.memory.model.PageEntry;
import com.group.myos.process.model.Process;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MemoryManagerImpl implements MemoryManager {
    private static final Logger logger = LogManager.getLogger(MemoryManagerImpl.class);
    private static final int TOTAL_MEMORY = 1024; // 总内存大小（MB）
    private static final int PAGE_SIZE = 4; // 页大小（MB）
    private static final int MAX_PAGES_PER_PROCESS = 256; // 每个进程最大页数

    private final Object[] memory; // 物理内存
    private final Map<Process, MemoryBlock> allocatedMemoryForProcess; // 已分配内存的进程映射
    private final List<MemoryBlock> freeBlocks; // 空闲内存块列表
    private final Map<Process, PageEntry[]> pageTables; // 进程页表
    private final int[] pageBitmap; // 物理页使用情况
    private final int[] pageLastVisit; // 页面最后访问时间
    private final List<SwappedOutPage> swappedPages; // 被换出的页面
    private int currentTime; // 当前时间，用于LRU算法

    public MemoryManagerImpl() {
        this.memory = new Object[TOTAL_MEMORY];
        this.allocatedMemoryForProcess = new HashMap<>();
        this.freeBlocks = new ArrayList<>();
        this.pageTables = new HashMap<>();
        this.pageBitmap = new int[TOTAL_MEMORY / PAGE_SIZE];
        this.pageLastVisit = new int[TOTAL_MEMORY / PAGE_SIZE];
        this.swappedPages = new ArrayList<>();
        this.currentTime = 0;
        
        // 初始化空闲内存块
        this.freeBlocks.add(new MemoryBlock(0, TOTAL_MEMORY));
    }

    @Override
    public boolean allocateMemoryForProcess(Process process, int size) {
        // 计算需要的页数（向上取整）
        int requiredPages = (size + PAGE_SIZE - 1) / PAGE_SIZE;
        if (requiredPages > MAX_PAGES_PER_PROCESS) {
            logger.error("Process {} requires too many pages: {}", process.getName(), requiredPages);
            return false;
        }

        // 检查是否有足够的空闲内存
        if (getFreeMemorySize() < size) {
            logger.error("Not enough free memory for process {}: required {}, available {}", 
                process.getName(), size, getFreeMemorySize());
            return false;
        }

        // 为进程创建页表
        PageEntry[] pageTable = new PageEntry[MAX_PAGES_PER_PROCESS];
        for (int i = 0; i < MAX_PAGES_PER_PROCESS; i++) {
            pageTable[i] = new PageEntry();
        }
        pageTables.put(process, pageTable);

        // 分配物理页
        for (int i = 0; i < requiredPages; i++) {
            if (!allocatePage(process, i)) {
                // 分配失败，回滚已分配的页
                for (int j = 0; j < i; j++) {
                    freePage(process, j);
                }
                pageTables.remove(process);
                return false;
            }
        }

        // 更新已分配内存映射
        allocatedMemoryForProcess.put(process, new MemoryBlock(0, requiredPages * PAGE_SIZE));
        return true;
    }

    @Override
    public void freeMemoryForProcess(Process process) {
        if (process == null) {
            return;
        }

        // 获取进程的页表
        PageEntry[] pageTable = pageTables.get(process);
        if (pageTable != null) {
            // 释放所有页
            for (int i = 0; i < MAX_PAGES_PER_PROCESS; i++) {
                if (pageTable[i] != null && pageTable[i].isValid()) {
                    int physicalPage = pageTable[i].getPhysicalPageNumber();
                    // 清除页表位图
                    pageBitmap[physicalPage] = 0;
                    // 清除页表项
                    pageTable[i].setValid(false);
                    // 清除内存内容
                    if (memory != null) {
                        for (int j = 0; j < PAGE_SIZE; j++) {
                            memory[physicalPage * PAGE_SIZE + j] = null;
                        }
                    }
                }
            }
            // 移除页表
            pageTables.remove(process);
        }

        // 移除已分配内存记录
        allocatedMemoryForProcess.remove(process);

        // 重新计算空闲内存块
        recalculateFreeBlocks();
    }

    private void recalculateFreeBlocks() {
        // 清空当前空闲块列表
        freeBlocks.clear();
        
        // 遍历页表位图，找出所有连续的空闲区域
        int start = 0;
        int size = 0;
        
        for (int i = 0; i < pageBitmap.length; i++) {
            if (pageBitmap[i] == 0) {
                // 空闲页
                if (size == 0) {
                    start = i * PAGE_SIZE;
                }
                size += PAGE_SIZE;
            } else {
                // 已分配页
                if (size > 0) {
                    freeBlocks.add(new MemoryBlock(start, size));
                    size = 0;
                }
            }
        }
        
        // 处理最后一个空闲块
        if (size > 0) {
            freeBlocks.add(new MemoryBlock(start, size));
        }
        
        // 如果没有空闲块，添加一个全空的内存块
        if (freeBlocks.isEmpty()) {
            freeBlocks.add(new MemoryBlock(0, TOTAL_MEMORY));
        }
    }

    @Override
    public boolean allocatePage(Process process, int virtualPageNumber) {
        // 查找空闲物理页
        int physicalPage = findFreePhysicalPage();
        if (physicalPage == -1) {
            // 没有空闲页，需要页面置换
            physicalPage = findPageToSwap();
            if (physicalPage == -1) {
                return false;
            }
            // 将选中的页换出
            swapOutPage(process, virtualPageNumber);
        }

        // 更新页表
        PageEntry pageEntry = new PageEntry(physicalPage);
        pageTables.get(process)[virtualPageNumber] = pageEntry;
        
        // 更新页表位图
        pageBitmap[physicalPage] = process.getId().intValue();
        pageLastVisit[physicalPage] = currentTime++;
        
        return true;
    }

    @Override
    public void freePage(Process process, int virtualPageNumber) {
        PageEntry pageEntry = pageTables.get(process)[virtualPageNumber];
        if (pageEntry != null && pageEntry.isValid()) {
            int physicalPage = pageEntry.getPhysicalPageNumber();
            // 清除页表位图
            pageBitmap[physicalPage] = 0;
            // 清除页表项
            pageEntry.setValid(false);
        }
    }

    @Override
    public PageEntry getPageEntry(Process process, int virtualPageNumber) {
        PageEntry[] pageTable = pageTables.get(process);
        return pageTable != null ? pageTable[virtualPageNumber] : null;
    }

    @Override
    public boolean isPageValid(Process process, int virtualPageNumber) {
        PageEntry pageEntry = getPageEntry(process, virtualPageNumber);
        return pageEntry != null && pageEntry.isValid();
    }

    @Override
    public boolean writeMemory(Process process, int virtualAddress, Object content) {
        if (virtualAddress < 0 || virtualAddress >= TOTAL_MEMORY) {
            logger.error("Invalid virtual address: {} for process {}", virtualAddress, process.getName());
            return false;
        }

        int virtualPageNumber = virtualAddress / PAGE_SIZE;
        int offset = virtualAddress % PAGE_SIZE;
        
        PageEntry pageEntry = getPageEntry(process, virtualPageNumber);
        if (pageEntry == null || !pageEntry.isValid()) {
            // 触发缺页中断
            handlePageFault(process, virtualAddress);
            return false;
        }

        if (!pageEntry.isWrite()) {
            logger.error("Write permission denied for process {} at address {}", process.getName(), virtualAddress);
            return false;
        }

        // 写入内存
        int physicalAddress = pageEntry.getPhysicalPageNumber() * PAGE_SIZE + offset;
        if (physicalAddress >= memory.length) {
            logger.error("Physical address out of bounds: {} for process {}", physicalAddress, process.getName());
            return false;
        }
        memory[physicalAddress] = content;
        pageEntry.setDirty(true);
        pageLastVisit[pageEntry.getPhysicalPageNumber()] = currentTime++;
        
        return true;
    }

    @Override
    public Object readMemory(Process process, int virtualAddress) {
        if (virtualAddress < 0 || virtualAddress >= TOTAL_MEMORY) {
            logger.error("Invalid virtual address: {} for process {}", virtualAddress, process.getName());
            return null;
        }

        int virtualPageNumber = virtualAddress / PAGE_SIZE;
        int offset = virtualAddress % PAGE_SIZE;
        
        PageEntry pageEntry = getPageEntry(process, virtualPageNumber);
        if (pageEntry == null || !pageEntry.isValid()) {
            // 触发缺页中断
            handlePageFault(process, virtualAddress);
            return null;
        }

        if (!pageEntry.isRead()) {
            logger.error("Read permission denied for process {} at address {}", process.getName(), virtualAddress);
            return null;
        }

        // 读取内存
        int physicalAddress = pageEntry.getPhysicalPageNumber() * PAGE_SIZE + offset;
        if (physicalAddress >= memory.length) {
            logger.error("Physical address out of bounds: {} for process {}", physicalAddress, process.getName());
            return null;
        }
        pageLastVisit[pageEntry.getPhysicalPageNumber()] = currentTime++;
        return memory[physicalAddress];
    }

    @Override
    public int getFreeMemorySize() {
        int freePages = 0;
        for (int i = 0; i < pageBitmap.length; i++) {
            if (pageBitmap[i] == 0) {
                freePages++;
            }
        }
        return freePages * PAGE_SIZE;
    }

    @Override
    public List<MemoryBlock> getFreeBlocks() {
        recalculateFreeBlocks();
        return new ArrayList<>(freeBlocks);
    }

    @Override
    public double getMemoryUsage() {
        int used = 0;
        for (int i = 0; i < pageBitmap.length; i++) {
            if (pageBitmap[i] != 0) {
                used++;
            }
        }
        return (double) used / pageBitmap.length;
    }

    @Override
    public void handlePageFault(Process process, int virtualAddress) {
        int virtualPageNumber = virtualAddress / PAGE_SIZE;
        // 检查是否在交换区
        SwappedOutPage swappedPage = findSwappedPage(process, virtualPageNumber);
        if (swappedPage != null) {
            // 从交换区换入
            swapInPage(process, virtualPageNumber);
        } else {
            // 分配新页
            allocatePage(process, virtualPageNumber);
        }
    }

    @Override
    public void swapOutPage(Process process, int virtualPageNumber) {
        PageEntry pageEntry = getPageEntry(process, virtualPageNumber);
        if (pageEntry != null && pageEntry.isValid()) {
            int physicalPage = pageEntry.getPhysicalPageNumber();
            // 保存页面内容
            Object[] pageContent = new Object[PAGE_SIZE];
            if (memory != null) {
            System.arraycopy(memory, physicalPage * PAGE_SIZE, pageContent, 0, PAGE_SIZE);
            }
            swappedPages.add(new SwappedOutPage(process.getId().intValue(), virtualPageNumber, pageContent));
            // 清除页表项
            pageEntry.setValid(false);
            // 清除页表位图
            pageBitmap[physicalPage] = 0;
        }
    }

    @Override
    public void swapInPage(Process process, int virtualPageNumber) {
        SwappedOutPage swappedPage = findSwappedPage(process, virtualPageNumber);
        if (swappedPage != null) {
            // 分配新的物理页
            int physicalPage = findFreePhysicalPage();
            if (physicalPage == -1) {
                physicalPage = findPageToSwap();
                if (physicalPage == -1) {
                    return;
                }
            }
            // 恢复页面内容
            if (swappedPage.getContent() != null && memory != null) {
            System.arraycopy(swappedPage.getContent(), 0, memory, physicalPage * PAGE_SIZE, PAGE_SIZE);
            }
            // 更新页表
            PageEntry pageEntry = new PageEntry(physicalPage);
            pageTables.get(process)[virtualPageNumber] = pageEntry;
            // 更新页表位图
            pageBitmap[physicalPage] = process.getId().intValue();
            pageLastVisit[physicalPage] = currentTime++;
            // 从交换区移除
            swappedPages.remove(swappedPage);
        }
    }

    private int findFreePhysicalPage() {
        for (int i = 0; i < pageBitmap.length; i++) {
            if (pageBitmap[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private int findPageToSwap() {
        // LRU算法：选择最久未使用的页
        int oldestTime = Integer.MAX_VALUE;
        int oldestPage = -1;
        for (int i = 0; i < pageLastVisit.length; i++) {
            if (pageBitmap[i] != 0 && pageLastVisit[i] < oldestTime) {
                oldestTime = pageLastVisit[i];
                oldestPage = i;
            }
        }
        return oldestPage;
    }

    private SwappedOutPage findSwappedPage(Process process, int virtualPageNumber) {
        for (SwappedOutPage page : swappedPages) {
            if (page.getProcessId() == process.getId().intValue() && page.getVirtualPageNumber() == virtualPageNumber) {
                return page;
            }
        }
        return null;
    }

    @Override
    public int[] getMemoryStatus() {
        int[] memoryStatus = new int[TOTAL_MEMORY / PAGE_SIZE];
        Arrays.fill(memoryStatus, 0);
        
        // 遍历页表位图，设置已分配的内存块
        for (int i = 0; i < pageBitmap.length; i++) {
            if (pageBitmap[i] != 0) {
                memoryStatus[i] = pageBitmap[i];
            }
        }
        
        return memoryStatus;
    }

    @Getter
    private static class SwappedOutPage {
        private final int processId;
        private final int virtualPageNumber;
        private final Object[] content;

        public SwappedOutPage(int processId, int virtualPageNumber, Object[] content) {
            this.processId = processId;
            this.virtualPageNumber = virtualPageNumber;
            this.content = content;
        }

    }
}