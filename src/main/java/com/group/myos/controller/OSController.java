package com.group.myos.controller;

import com.group.myos.process.model.Process;
import com.group.myos.process.scheduler.ProcessScheduler;
import com.group.myos.memory.MemoryManager;
import com.group.myos.filesystem.FileSystem;
import com.group.myos.device.DeviceManager;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/os")
public class OSController {
    private final ProcessScheduler processScheduler;
    private final MemoryManager memoryManager;
    private final FileSystem fileSystem;
    private final DeviceManager deviceManager;
    
    public OSController(
        ProcessScheduler processScheduler,
        MemoryManager memoryManager,
        FileSystem fileSystem,
        DeviceManager deviceManager
    ) {
        this.processScheduler = processScheduler;
        this.memoryManager = memoryManager;
        this.fileSystem = fileSystem;
        this.deviceManager = deviceManager;
    }
    
    // 进程管理API
    @PostMapping("/process")
    public Process createProcess(@RequestBody Process process) {
        processScheduler.addProcess(process);
        return process;
    }
    
    @GetMapping("/process")
    public List<Process> getAllProcesses() {
        return processScheduler.getReadyProcesses();
    }
    
    @GetMapping("/process/current")
    public Process getCurrentProcess() {
        return processScheduler.getCurrentProcess();
    }
    
    // 内存管理API
    @PostMapping("/memory/allocate")
    public boolean allocateMemory(
        @RequestParam Long processId,
        @RequestParam Integer size
    ) {
        Process process = processScheduler.getCurrentProcess();
        return memoryManager.allocateMemory(process, size);
    }
    
    @GetMapping("/memory/free")
    public List<MemoryManager.MemoryBlock> getFreeMemory() {
        return memoryManager.getFreeBlocks();
    }
    
    // 文件系统API
    @PostMapping("/file")
    public boolean createFile(
        @RequestParam String name,
        @RequestParam Integer size
    ) {
        return fileSystem.createFile(name, size);
    }
    
    @GetMapping("/file")
    public List<String> listFiles() {
        return fileSystem.listDirectory();
    }
    
    // 设备管理API
    @PostMapping("/device/request")
    public boolean requestDevice(
        @RequestParam Long processId,
        @RequestParam String deviceName
    ) {
        Process process = processScheduler.getCurrentProcess();
        return deviceManager.requestDevice(process, deviceName);
    }
    
    @GetMapping("/device/available")
    public List<DeviceManager.Device> getAvailableDevices() {
        return deviceManager.getAvailableDevices();
    }
} 