package com.group.myos.process.controller;

import com.group.myos.process.model.Process;
import com.group.myos.process.ProcessScheduler;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/process")
public class ProcessController {
    @Resource
    private ProcessScheduler processScheduler;

    // 进程管理API
    @PostMapping("")
    public Process createProcess(@RequestBody Process process) {
        processScheduler.addProcess(process);
        return process;
    }

    @GetMapping("")
    public List<Process> getAllProcesses() {
        return processScheduler.getReadyProcesses();
    }

    @GetMapping("current")
    public Process getCurrentProcess() {
        return processScheduler.getCurrentProcess();
    }
}
