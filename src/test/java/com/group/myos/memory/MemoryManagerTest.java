package com.group.myos.memory;

import com.group.myos.memory.impl.MemoryManagerImpl;
import com.group.myos.memory.model.MemoryBlock;
import com.group.myos.process.model.Process;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

public class MemoryManagerTest {
    private MemoryManager memoryManager;
    private Process process1;
    private Process process2;

    @BeforeEach
    void setUp() {
        memoryManager = new MemoryManagerImpl();
        process1 = new Process();
        process1.setId(1L);
        process1.setName("Process1");
        process1.setMemorySize(16); // 16MB

        process2 = new Process();
        process2.setId(2L);
        process2.setName("Process2");
        process2.setMemorySize(32); // 32MB
    }

    @Test
    void testInitialMemoryState() {
        // 测试初始内存状态
        List<MemoryBlock> freeBlocks = memoryManager.getFreeBlocks();
        assertEquals(1, freeBlocks.size());
        assertEquals(1024, freeBlocks.get(0).getSize()); // 总内存1024MB
        assertEquals(0, freeBlocks.get(0).getStart());
        assertEquals(0.0, memoryManager.getMemoryUsage());
    }

    @Test
    void testMemoryAllocation() {
        // 测试内存分配
        assertTrue(memoryManager.allocateMemoryForProcess(process1, 16));
        assertEquals(1008, memoryManager.getFreeMemorySize()); // 1024 - 16
        assertTrue(memoryManager.getMemoryUsage() > 0);
    }

    @Test
    void testMemoryAllocationAndFree() {
        // 测试内存分配和释放
        assertTrue(memoryManager.allocateMemoryForProcess(process1, 16));
        int freeMemoryAfterAllocation = memoryManager.getFreeMemorySize();
        assertEquals(1008, freeMemoryAfterAllocation);

        memoryManager.freeMemoryForProcess(process1);
        int freeMemoryAfterFree = memoryManager.getFreeMemorySize();

        assertEquals(1024, freeMemoryAfterFree); // 释放后应该恢复全部内存
        assertEquals(0.0, memoryManager.getMemoryUsage());
    }

    @Test
    void testMultipleProcessesMemoryManagement() {
        // 测试多个进程的内存管理
        assertTrue(memoryManager.allocateMemoryForProcess(process1, 16));
        assertTrue(memoryManager.allocateMemoryForProcess(process2, 32));

        int allocatedMemory = 1024 - memoryManager.getFreeMemorySize();
        assertEquals(48, allocatedMemory);

        memoryManager.freeMemoryForProcess(process1);
        allocatedMemory = 1024 - memoryManager.getFreeMemorySize();
        assertEquals(32, allocatedMemory);

        memoryManager.freeMemoryForProcess(process2);
        allocatedMemory = 1024 - memoryManager.getFreeMemorySize();
        assertEquals(0, allocatedMemory);
        assertEquals(1024, memoryManager.getFreeMemorySize());
    }

    @Test
    void testMemoryFragmentation() {
        // 测试内存碎片化情况
        assertTrue(memoryManager.allocateMemoryForProcess(process1, 16));
        assertTrue(memoryManager.allocateMemoryForProcess(process2, 32));

        memoryManager.freeMemoryForProcess(process1);

        List<MemoryBlock> freeBlocks = memoryManager.getFreeBlocks();
        assertEquals(2, freeBlocks.size()); // 应该有两个空闲块

        // 验证空闲块的大小和位置
        boolean foundFirstBlock = false;
        boolean foundSecondBlock = false;

        for (MemoryBlock block : freeBlocks) {
            if (block.getStart() == 0 && block.getSize() == 16) {
                foundFirstBlock = true;
            } else if (block.getStart() == 48 && block.getSize() == 976) {
                foundSecondBlock = true;
            }
        }

        assertTrue(foundFirstBlock && foundSecondBlock);
    }

    @Test
    void testMemoryAllocationFailure() {
        // 测试内存分配失败的情况
        Process largeProcess = new Process();
        largeProcess.setId(3L);
        largeProcess.setName("LargeProcess");
        largeProcess.setMemorySize(2048); // 尝试分配超过总内存的大小

        assertFalse(memoryManager.allocateMemoryForProcess(largeProcess, 2048));
        assertEquals(1024, memoryManager.getFreeMemorySize());
    }

    @Test
    void testMemoryStatus() {
        // 测试内存状态数组
        assertTrue(memoryManager.allocateMemoryForProcess(process1, 16));
        int[] status = memoryManager.getMemoryStatus();

        // 验证前4个页（16MB）被分配
        for (int i = 0; i < 4; i++) {
            assertEquals(1, status[i]); // 进程1的ID为1
        }

        // 验证其余页未被分配
        for (int i = 4; i < status.length; i++) {
            assertEquals(0, status[i]);
        }
    }
}