package com.group.myos.memory.controller;

import com.group.myos.memory.MemoryManager;
import com.group.myos.memory.model.MemoryBlock;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/memory")
public class MemoryController {
    @Resource
    private MemoryManager memoryManager;

    // 获取空闲内存大小
    @GetMapping("free-size")
    public int getFreeMemorySize() {
        return memoryManager.getFreeMemorySize();
    }

    // 获取每一个内存块的状态
    @GetMapping("status")
    public int[] getMemoryStatus() {
        return memoryManager.getMemoryStatus();
    }

    // 获取空闲内存块列表
    @GetMapping("free-blocks")
    public List<MemoryBlock> getFreeMemoryBlocks() {
        return memoryManager.getFreeBlocks();
    }


    // 获取内存使用率
    @GetMapping("/usage")
    public double getMemoryUsage() {
        return memoryManager.getMemoryUsage();
    }
}
