package com.group.myos.process.controller;

import com.group.myos.process.model.Process;
import com.group.myos.process.ProcessScheduler;
import com.group.myos.process.ProcessSwapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/process")
public class ProcessController {
    
    private final ProcessScheduler processScheduler;
    private final ProcessSwapper processSwapper;
    
    public ProcessController(ProcessScheduler processScheduler, ProcessSwapper processSwapper) {
        this.processScheduler = processScheduler;
        this.processSwapper = processSwapper;
    }

    // 进程管理API
    @PostMapping("")
    public Process createProcess(@RequestBody Process process) {
        return processScheduler.addProcess(process);
    }

    @GetMapping("")
    public List<Process> getAllProcesses() {
        return processScheduler.getAllProcesses();
    }
    
    @GetMapping("ready")
    public List<Process> getReadyProcesses() {
        return processScheduler.getReadyProcesses();
    }
    
    @GetMapping("waiting")
    public List<Process> getWaitingProcesses() {
        return processScheduler.getWaitingProcesses();
    }
    
    @GetMapping("terminated")
    public List<Process> getTerminatedProcesses() {
        return processScheduler.getTerminatedProcesses();
    }
    
    @GetMapping("swapped")
    public List<Process> getSwappedProcesses() {
        return processSwapper.getSwappedProcesses();
    }

    @GetMapping("current")
    public Process getCurrentProcess() {
        return processScheduler.getCurrentProcess();
    }
    
    @DeleteMapping("{id}")
    public ResponseEntity<Map<String, Object>> deleteProcess(@PathVariable Long id) {
        processScheduler.deleteProcess(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "进程已删除");
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("{id}/state")
    public ResponseEntity<Map<String, Object>> updateProcessState(
            @PathVariable Long id, 
            @RequestParam String state) {
        try {
            Process.ProcessState processState = Process.ProcessState.valueOf(state.toUpperCase());
            processScheduler.updateProcessState(id, processState);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "进程状态已更新");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "无效的进程状态: " + state);
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("schedule")
    public Process scheduleNextProcess() {
        return processScheduler.schedule();
    }
    
    @PostMapping("{id}/block")
    public ResponseEntity<Map<String, Object>> blockProcess(@PathVariable Long id) {
        Process process = processScheduler.getAllProcesses().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
                
        if (process == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "未找到进程: " + id);
            return ResponseEntity.badRequest().body(response);
        }
        
        processScheduler.blockProcess(process);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "进程已阻塞");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("{id}/wakeup")
    public ResponseEntity<Map<String, Object>> wakeUpProcess(@PathVariable Long id) {
        Process process = processScheduler.getAllProcesses().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
                
        if (process == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "未找到进程: " + id);
            return ResponseEntity.badRequest().body(response);
        }
        
        processScheduler.wakeUpProcess(process);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "进程已唤醒");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("{id}/terminate")
    public ResponseEntity<Map<String, Object>> terminateProcess(@PathVariable Long id) {
        Process process = processScheduler.getAllProcesses().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
                
        if (process == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "未找到进程: " + id);
            return ResponseEntity.badRequest().body(response);
        }
        
        processScheduler.terminateProcess(process);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "进程已终止");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("{id}/swapin")
    public ResponseEntity<Map<String, Object>> swapInProcess(@PathVariable Long id) {
        Process process = processScheduler.getAllProcesses().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
                
        if (process == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "未找到进程: " + id);
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean success = processSwapper.swapIn(process);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "进程已加载到内存" : "加载进程到内存失败");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("{id}/swapout")
    public ResponseEntity<Map<String, Object>> swapOutProcess(@PathVariable Long id) {
        Process process = processScheduler.getAllProcesses().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
                
        if (process == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "未找到进程: " + id);
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean success = processSwapper.swapOut(process);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "进程已交换到磁盘" : "交换进程到磁盘失败");
        return ResponseEntity.ok(response);
    }
}
