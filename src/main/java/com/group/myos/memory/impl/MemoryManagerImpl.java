package com.group.myos.memory.impl;

import com.group.myos.filesystem.model.File;
import com.group.myos.memory.model.MemoryBlock;
import com.group.myos.process.model.Process;
import com.group.myos.memory.MemoryManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MemoryManagerImpl implements MemoryManager {
    private static final Logger logger = LogManager.getLogger(MemoryManagerImpl.class);
    private static final int TOTAL_MEMORY = 1024; // 总内存大小（MB）
    private final Map<Process, MemoryBlock> allocatedMemoryForProcess; // 已分配内存的进程映射
    private final Map<File, MemoryBlock> allocatedMemoryForFile; // 已分配内存的文件映射
    private final List<MemoryBlock> freeBlocks; // 空闲内存块列表

    public MemoryManagerImpl() {
        this.allocatedMemoryForProcess = new HashMap<>();
        this.allocatedMemoryForFile = new HashMap<>();
        this.freeBlocks = new ArrayList<>();
        this.freeBlocks.add(new MemoryBlock(0, TOTAL_MEMORY)); // 初始化时，所有内存都是空闲的
    }

    @Override
    public boolean allocateMemoryForProcess(Process process, int size) {
        return allocateMemoryForProcessInternal(process, size, false);
    }

    @Override
    public void freeMemoryForProcess(Process process) {
        freeMemoryForProcessInternal(process, false);
    }

    @Override
    public boolean allocateMemoryForFile(File file, int size) {
        return allocateMemoryForProcessInternal(file, size, true);
    }

    @Override
    public void freeMemoryForFile(File file) {
        freeMemoryForProcessInternal(file, true);
    }

    @Override
    public int getFreeMemorySize() {
        int freeMemorySize = 0;
        for (MemoryBlock block : freeBlocks) {
            freeMemorySize += block.getSize();
        }
        return freeMemorySize;
    }

    private boolean allocateMemoryForProcessInternal(Object entity, int size, boolean isFileSystem) {
        // 查找合适的空闲块（首次适应算法）
        for (int i = 0; i < freeBlocks.size(); i++) {
            MemoryBlock block = freeBlocks.get(i);
            if (block.getSize() >= size) {
                // 分配内存
                MemoryBlock allocatedBlock = new MemoryBlock(block.getStart(), size);
                if (isFileSystem) {
                    allocatedMemoryForFile.put((File) entity, allocatedBlock);
                } else {
                    allocatedMemoryForProcess.put((Process) entity, allocatedBlock);
                }

                // 更新空闲块
                if (block.getSize() > size) {
                    freeBlocks.set(i, new MemoryBlock(block.getStart() + size, block.getSize() - size));
                } else {
                    freeBlocks.remove(i);
                }

                logger.info("Allocated memory for entity: {}, Size: {}", entity, size);
                return true;
            }
        }

        logger.error("Failed to allocate memory for entity: {}, Size: {}", entity, size);
        return false;
    }

    private void freeMemoryForProcessInternal(Object entity, boolean isFileSystem) {
        MemoryBlock block;
        if (isFileSystem) {
            block = allocatedMemoryForFile.remove(entity); // 使用 File 类型
        } else {
            block = allocatedMemoryForProcess.remove(entity); // 使用 Process 类型
        }

        if (block != null) {
            // 添加到空闲块列表
            freeBlocks.add(block);
            mergeFreeBlocks(); // 合并相邻的空闲块
            if (isFileSystem) {
                logger.info("Freed memory for file: {}, Size: {}", ((File) entity).getName(), block.getSize());
            } else {
                logger.info("Freed memory for process: {}, Size: {}", ((Process) entity).getName(), block.getSize());
            }
        }
    }

    private void mergeFreeBlocks() {
        // 按起始地址排序
        freeBlocks.sort(Comparator.comparingInt(MemoryBlock::getStart));

        // 合并相邻的空闲块
        for (int i = 0; i < freeBlocks.size() - 1; ) {
            MemoryBlock current = freeBlocks.get(i);
            MemoryBlock next = freeBlocks.get(i + 1);

            if (current.getStart() + current.getSize() == next.getStart()) {
                freeBlocks.set(i, new MemoryBlock(current.getStart(), current.getSize() + next.getSize()));
                freeBlocks.remove(i + 1);
            } else {
                i++;
            }
        }
    }

    @Override
    public List<MemoryBlock> getFreeBlocks() {
        return new ArrayList<>(freeBlocks);
    }

    @Override
    public Map<Process, MemoryBlock> getAllocatedMemory() {
        return new HashMap<>(allocatedMemoryForProcess);
    }

    @Override
    public Map<File, MemoryBlock> getAllocatedFileSystemMemory() {
        return new HashMap<>(allocatedMemoryForFile);
    }
}