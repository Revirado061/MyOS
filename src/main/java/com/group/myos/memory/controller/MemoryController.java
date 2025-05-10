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
    @GetMapping("free")
    public List<MemoryBlock> getFreeMemory() {
        return memoryManager.getFreeBlocks();
    }
}
