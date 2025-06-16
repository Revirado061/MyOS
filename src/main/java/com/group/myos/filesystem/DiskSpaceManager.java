package com.group.myos.filesystem;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class DiskSpaceManager {
    private static final long TOTAL_DISK_SIZE = 8L * 1024 * 1024 * 1024; // 8GB
    private static final int BLOCK_SIZE = 32 * 1024 * 1024; // 32MB per block
    private static final long TOTAL_BLOCKS = TOTAL_DISK_SIZE / BLOCK_SIZE;

    private final BitSet blockBitmap; // 位图，标记块的使用情况
    private final Map<Integer, byte[]> diskBlocks; // 模拟磁盘块存储
    private final Map<Integer, Set<Integer>> fileBlocks; // 文件占用的块号映射

    public DiskSpaceManager() {
        this.blockBitmap = new BitSet((int) TOTAL_BLOCKS);
        this.diskBlocks = new HashMap<>();
        this.fileBlocks = new HashMap<>();
    }

    // 分配指定数量的块
    public int[] allocateBlocks(int fileId, int numBlocks) {
        if (numBlocks <= 0) {
            return new int[0];
        }

        // 查找连续的空闲块
        int[] allocatedBlocks = new int[numBlocks];
        int allocatedCount = 0;
        int startBlock = 0;

        while (allocatedCount < numBlocks && startBlock < TOTAL_BLOCKS) {
            // 查找下一个空闲块
            startBlock = blockBitmap.nextClearBit(startBlock);
            if (startBlock >= TOTAL_BLOCKS) {
                break;
            }

            // 检查是否有足够的连续空闲块
            boolean hasEnoughBlocks = true;
            for (int i = 0; i < numBlocks; i++) {
                if (startBlock + i >= TOTAL_BLOCKS || blockBitmap.get(startBlock + i)) {
                    hasEnoughBlocks = false;
                    startBlock += i + 1;
                    break;
                }
            }

            if (hasEnoughBlocks) {
                // 分配块
                for (int i = 0; i < numBlocks; i++) {
                    int blockNumber = startBlock + i;
                    blockBitmap.set(blockNumber);
                    allocatedBlocks[allocatedCount++] = blockNumber;
                    diskBlocks.put(blockNumber, new byte[BLOCK_SIZE]);
                }
                break;
            }
        }

        if (allocatedCount == numBlocks) {
            // 记录文件占用的块
            Set<Integer> blocks = new HashSet<>();
            for (int block : allocatedBlocks) {
                blocks.add(block);
            }
            fileBlocks.put(fileId, blocks);
            return allocatedBlocks;
        } else {
            // 分配失败，释放已分配的块
            for (int i = 0; i < allocatedCount; i++) {
                blockBitmap.clear(allocatedBlocks[i]);
                diskBlocks.remove(allocatedBlocks[i]);
            }
            return new int[0];
        }
    }

    // 释放文件占用的块
    public void freeBlocks(int fileId) {
        Set<Integer> blocks = fileBlocks.remove(fileId);
        if (blocks != null) {
            for (int block : blocks) {
                blockBitmap.clear(block);
                diskBlocks.remove(block);
            }
        }
    }

    // 写入块数据
    public boolean writeBlock(int blockNumber, byte[] data) {
        if (blockNumber < 0 || blockNumber >= TOTAL_BLOCKS || !blockBitmap.get(blockNumber)) {
            return false;
        }
        if (data.length > BLOCK_SIZE) {
            return false;
        }
        byte[] blockData = new byte[BLOCK_SIZE];
        System.arraycopy(data, 0, blockData, 0, data.length);
        diskBlocks.put(blockNumber, blockData);
        return true;
    }

    // 读取块数据
    public byte[] readBlock(int blockNumber) {
        if (blockNumber < 0 || blockNumber >= TOTAL_BLOCKS || !blockBitmap.get(blockNumber)) {
            return null;
        }
        return diskBlocks.get(blockNumber);
    }

    // 获取磁盘使用情况
    public Map<String, Object> getDiskStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalSize", TOTAL_DISK_SIZE);
        status.put("blockSize", BLOCK_SIZE);
        status.put("totalBlocks", TOTAL_BLOCKS);
        status.put("usedBlocks", blockBitmap.cardinality());
        status.put("freeBlocks", TOTAL_BLOCKS - blockBitmap.cardinality());
        status.put("usagePercentage", (double) blockBitmap.cardinality() / TOTAL_BLOCKS * 100);
        return status;
    }

    // 获取块大小
    public int getBlockSize() {
        return BLOCK_SIZE;
    }

    // 获取文件占用的块
    public Set<Integer> getFileBlocks(int fileId) {
        return fileBlocks.getOrDefault(fileId, new HashSet<>());
    }

    // 获取总块数
    public long getTotalBlocks() {
        return TOTAL_BLOCKS;
    }

    // 获取已使用块数
    public long getUsedBlocks() {
        return blockBitmap.cardinality();
    }

    // 获取空闲块数
    public long getFreeBlocks() {
        return TOTAL_BLOCKS - getUsedBlocks();
    }

    // 获取磁盘使用百分比
    public double getUsagePercentage() {
        return (double) getUsedBlocks() / TOTAL_BLOCKS;
    }
} 