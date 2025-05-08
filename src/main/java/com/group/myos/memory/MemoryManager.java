package com.group.myos.memory;

import com.group.myos.process.model.Process;
import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class MemoryManager {
    private static final int TOTAL_MEMORY = 1024; // 总内存大小（MB）
    private final Map<Process, MemoryBlock> allocatedMemory;
    private final List<MemoryBlock> freeBlocks;
    
    public MemoryManager() {
        this.allocatedMemory = new HashMap<>();
        this.freeBlocks = new ArrayList<>();
        this.freeBlocks.add(new MemoryBlock(0, TOTAL_MEMORY));
    }
    
    public boolean allocateMemory(Process process, int size) {
        // 查找合适的空闲块
        MemoryBlock bestFit = null;
        int bestFitIndex = -1;
        
        for (int i = 0; i < freeBlocks.size(); i++) {
            MemoryBlock block = freeBlocks.get(i);
            if (block.size >= size) {
                if (bestFit == null || block.size < bestFit.size) {
                    bestFit = block;
                    bestFitIndex = i;
                }
            }
        }
        
        if (bestFit != null) {
            // 分配内存
            MemoryBlock allocatedBlock = new MemoryBlock(bestFit.start, size);
            allocatedMemory.put(process, allocatedBlock);
            
            // 更新空闲块
            if (bestFit.size > size) {
                freeBlocks.set(bestFitIndex, 
                    new MemoryBlock(bestFit.start + size, bestFit.size - size));
            } else {
                freeBlocks.remove(bestFitIndex);
            }
            
            return true;
        }
        
        return false;
    }
    
    public void freeMemory(Process process) {
        MemoryBlock block = allocatedMemory.remove(process);
        if (block != null) {
            // 合并相邻的空闲块
            freeBlocks.add(block);
            Collections.sort(freeBlocks, Comparator.comparingInt(b -> b.start));
            
            for (int i = 0; i < freeBlocks.size() - 1; i++) {
                MemoryBlock current = freeBlocks.get(i);
                MemoryBlock next = freeBlocks.get(i + 1);
                
                if (current.start + current.size == next.start) {
                    freeBlocks.set(i, new MemoryBlock(current.start, 
                        current.size + next.size));
                    freeBlocks.remove(i + 1);
                    i--;
                }
            }
        }
    }
    
    public List<MemoryBlock> getFreeBlocks() {
        return new ArrayList<>(freeBlocks);
    }
    
    public Map<Process, MemoryBlock> getAllocatedMemory() {
        return new HashMap<>(allocatedMemory);
    }
    
    @Data
    public static class MemoryBlock {
        private final int start;
        private final int size;
    }
} 