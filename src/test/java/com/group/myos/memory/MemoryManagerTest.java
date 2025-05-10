package com.group.myos.memory;

import com.group.myos.memory.model.MemoryBlock;
import com.group.myos.process.model.Process;
import com.group.myos.memory.impl.MemoryManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryManagerTest {
    private MemoryManager memoryManager;

    @BeforeEach
    public void setUp() {
        memoryManager = new MemoryManagerImpl(); // 初始化内存管理器
    }

    @Test
    public void testAllocateAndFreeMemoryForProcess() {
        // 创建进程对象
        Process process1 = new Process(null, "Process1", 1, Process.ProcessState.NEW, 256);
        Process process2 = new Process(null, "Process2", 2, Process.ProcessState.NEW, 128);

        // 测试为进程分配内存
        assertTrue(memoryManager.allocateMemoryForProcess(process1, 256));
        assertEquals(768, memoryManager.getFreeMemorySize()); // 总内存 1024 - 256 = 768

        // 测试为第二个进程分配内存
        assertTrue(memoryManager.allocateMemoryForProcess(process2, 128));
        assertEquals(640, memoryManager.getFreeMemorySize()); // 总内存 1024 - 256 - 128 = 640

        // 测试释放进程占用的内存
        memoryManager.freeMemoryForProcess(process1);
        assertEquals(896, memoryManager.getFreeMemorySize()); // 总内存 1024 - 128 = 896

        // 测试释放第二个进程占用的内存
        memoryManager.freeMemoryForProcess(process2);
        assertEquals(1024, memoryManager.getFreeMemorySize()); // 总内存 1024
    }

    @Test
    public void testGetFreeBlocks() {
        // 创建进程对象
        Process process = new Process(null, "Process", 1, Process.ProcessState.NEW, 256);

        // 分配内存
        assertTrue(memoryManager.allocateMemoryForProcess(process, 256));

        // 获取空闲内存块列表
        List<MemoryBlock> freeBlocks = memoryManager.getFreeBlocks();
        assertEquals(1, freeBlocks.size());
        assertEquals(768, freeBlocks.get(0).getSize()); // 总内存 1024 - 256 = 768
        assertEquals(256, freeBlocks.get(0).getStart()); // 起始地址为 256
    }

    @Test
    public void testGetAllocatedMemory() {
        // 创建进程对象
        Process process = new Process(null, "Process", 1, Process.ProcessState.NEW, 256);

        // 分配内存
        assertTrue(memoryManager.allocateMemoryForProcess(process, 256));

        // 获取已分配内存的进程映射
        Map<Process, MemoryBlock> allocatedMemory = memoryManager.getAllocatedMemory();
        assertEquals(1, allocatedMemory.size());
        assertEquals(256, allocatedMemory.get(process).getSize());
        assertEquals(0, allocatedMemory.get(process).getStart());
    }

    @Test
    public void testMemoryAllocationFailure() {
        // 创建进程对象
        Process process = new Process(null, "Process", 1, Process.ProcessState.NEW, 1025);

        // 分配超出总内存大小的内存
        assertFalse(memoryManager.allocateMemoryForProcess(process, 1025)); // 总内存为 1024
        assertEquals(1024, memoryManager.getFreeMemorySize());
    }

    @Test
    public void testMemoryMerge() {
        // 创建进程对象
        Process process1 = new Process(null, "Process1", 1, Process.ProcessState.NEW, 256);
        Process process2 = new Process(null, "Process2", 2, Process.ProcessState.NEW, 256);

        // 分配内存
        assertTrue(memoryManager.allocateMemoryForProcess(process1, 256));
        assertTrue(memoryManager.allocateMemoryForProcess(process2, 256));

        // 释放内存并合并空闲块
        memoryManager.freeMemoryForProcess(process1);
        memoryManager.freeMemoryForProcess(process2);

        // 验证空闲块是否合并
        List<MemoryBlock> freeBlocks = memoryManager.getFreeBlocks();
        assertEquals(1, freeBlocks.size());
        assertEquals(1024, freeBlocks.get(0).getSize());
    }
}