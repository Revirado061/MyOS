package com.group.myos.process.controller;

import com.group.myos.process.ProcessSwapper;
import com.group.myos.process.model.Process;
import com.group.myos.process.ProcessScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    public ResponseEntity<Map<String, Object>> createProcess(@RequestBody Process process) {
        // 确保基本字段不为空
        if (process.getName() == null || process.getName().trim().isEmpty()) {
            process.setName("Process-" + System.currentTimeMillis());
        }
        
        if (process.getPriority() == null) {
            process.setPriority(0);
        }
        
        if (process.getMemorySize() == null) {
            process.setMemorySize(10); // 默认内存大小
        }
        
        Process newProcess = processScheduler.addProcess(process);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", newProcess);
        return ResponseEntity.ok(response);
    }

    // 添加新的端点：创建进程并立即启动（设置为READY状态）
    @PostMapping("/create-and-start")
    public ResponseEntity<Map<String, Object>> createAndStartProcess(@RequestBody Process process) {
        // 确保基本字段不为空
        if (process.getName() == null || process.getName().trim().isEmpty()) {
            process.setName("Process-" + System.currentTimeMillis());
        }
        
        if (process.getPriority() == null) {
            process.setPriority(0);
        }
        
        if (process.getMemorySize() == null) {
            process.setMemorySize(10); // 默认内存大小
        }
        
        // 创建进程
        Process newProcess = processScheduler.addProcess(process);
        
        // 如果进程状态为NEW，则立即设置为READY
        if (newProcess.getState() == Process.ProcessState.NEW) {
            processScheduler.updateProcessState(newProcess.getId(), Process.ProcessState.READY);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", newProcess);
        return ResponseEntity.ok(response);
    }

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getAllProcesses() {
        List<Process> processes = processScheduler.getAllProcesses();
        processes = processes != null ? processes : new ArrayList<>();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", processes);
        return ResponseEntity.ok(response);
    }
    
    // 修改为使用路径参数获取特定状态的进程
    @GetMapping("{state}")
    public ResponseEntity<Map<String, Object>> getProcessesByState(@PathVariable String state) {
        List<Process> processes = new ArrayList<>();
        
        switch(state.toLowerCase()) {
            case "ready":
                processes = processScheduler.getReadyProcesses();
                break;
            case "waiting":
                processes = processScheduler.getWaitingProcesses();
                break;
            case "terminated":
                processes = processScheduler.getTerminatedProcesses();
                break;
            case "swapped":
                processes = processSwapper.getSwappedProcesses();
                break;
            default:
                // 返回空列表，或者可以抛出异常
                break;
        }
        
        processes = processes != null ? processes : new ArrayList<>();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", processes);
        return ResponseEntity.ok(response);
    }

    @GetMapping("current")
    public ResponseEntity<Map<String, Object>> getCurrentProcess() {
        Process currentProcess = processScheduler.getCurrentProcess();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", currentProcess);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> scheduleNextProcess() {
        Process scheduledProcess = processScheduler.schedule();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", scheduledProcess);
        return ResponseEntity.ok(response);
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
    
    @PostMapping("{id}/start")
    public ResponseEntity<Map<String, Object>> startProcess(@PathVariable Long id) {
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
        
        if (process.getState() != Process.ProcessState.NEW) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "只有NEW状态的进程可以启动");
            return ResponseEntity.badRequest().body(response);
        }
        
        processScheduler.updateProcessState(id, Process.ProcessState.READY);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "进程已启动");
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
    
    @PutMapping("{id}/priority")
    public ResponseEntity<Map<String, Object>> updateProcessPriority(
            @PathVariable Long id, 
            @RequestParam Integer priority) {
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
        
        Process.ProcessState oldState = process.getState();
        
        if (oldState == Process.ProcessState.READY) {
            processScheduler.updateProcessState(id, Process.ProcessState.WAITING);
        }
        
        process.setPriority(priority);
        process.setLastUpdateTime(java.time.LocalDateTime.now());
        
        if (oldState == Process.ProcessState.READY) {
            processScheduler.updateProcessState(id, Process.ProcessState.READY);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "进程优先级已更新");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("{id}/info")
    public ResponseEntity<Map<String, Object>> getProcessInfo(@PathVariable Long id) {
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
        
        Map<String, Object> info = new HashMap<>();
        info.put("id", process.getId());
        info.put("name", process.getName());
        info.put("state", process.getState().name());
        info.put("priority", process.getPriority());
        info.put("memorySize", process.getMemorySize());
        info.put("createTime", process.getCreateTime());
        info.put("lastUpdateTime", process.getLastUpdateTime());
        info.put("inMemory", process.isInMemory());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", info);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("stats")
    public ResponseEntity<Map<String, Object>> getProcessStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Process> allProcesses = processScheduler.getAllProcesses();
        stats.put("totalProcesses", allProcesses.size());
        stats.put("newProcesses", allProcesses.stream()
                .filter(p -> p.getState() == Process.ProcessState.NEW)
                .count());
        stats.put("readyProcesses", processScheduler.getReadyProcesses().size());
        stats.put("runningProcesses", processScheduler.getCurrentProcess() != null ? 1 : 0);
        stats.put("waitingProcesses", processScheduler.getWaitingProcesses().size());
        stats.put("terminatedProcesses", processScheduler.getTerminatedProcesses().size());
        stats.put("swappedProcesses", processSwapper.getSwappedProcesses().size());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", stats);
        return ResponseEntity.ok(response);
    }

    // 恢复之前的交换相关端点
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

    // 添加新的端点：批量更新进程优先级
    @PutMapping("/batch-update-priorities")
    public ResponseEntity<Map<String, Object>> batchUpdatePriorities(@RequestBody Map<Long, Integer> priorityUpdates) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> results = new HashMap<>();
        
        priorityUpdates.forEach((id, priority) -> {
            Process process = processScheduler.getAllProcesses().stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .orElse(null);
                    
            if (process == null) {
                results.put(id.toString(), "未找到进程");
                return;
            }
            
            // 保存旧状态
            Process.ProcessState oldState = process.getState();
            
            // 如果进程在就绪队列中，需要先移除再重新加入
            if (oldState == Process.ProcessState.READY) {
                processScheduler.updateProcessState(id, Process.ProcessState.WAITING);
            }
            
            // 更新优先级
            process.setPriority(priority);
            process.setLastUpdateTime(java.time.LocalDateTime.now());
            
            // 重新设置状态，以便重新按优先级插入队列
            if (oldState == Process.ProcessState.READY) {
                processScheduler.updateProcessState(id, Process.ProcessState.READY);
            }
            
            results.put(id.toString(), "优先级已更新为 " + priority);
        });
        
        response.put("success", true);
        response.put("results", results);
        return ResponseEntity.ok(response);
    }

    // 添加新的端点：获取进程状态转换历史
    @GetMapping("{id}/transitions")
    public ResponseEntity<Map<String, Object>> getProcessTransitions(@PathVariable Long id) {
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
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", processScheduler.getProcessTransitionHistory(id));
        return ResponseEntity.ok(response);
    }

    // 添加新的端点：运行中的进程调用设备
    @PostMapping("{id}/request-device")
    public ResponseEntity<Map<String, Object>> requestDevice(
            @PathVariable Long id, 
            @RequestParam String deviceType,
            @RequestParam(required = false) Integer deviceId) {
        
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
        
        // 检查进程是否处于运行状态
        if (process.getState() != Process.ProcessState.RUNNING) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "只有运行中的进程可以请求设备");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 模拟进程请求设备，将进程状态从RUNNING转为WAITING
        processScheduler.blockProcess(process);
        
        // 记录设备请求信息（可以根据实际需求扩展）
        process.setLastUpdateTime(java.time.LocalDateTime.now());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "进程已请求设备并转入等待状态");
        
        Map<String, Object> data = new HashMap<>();
        data.put("processId", process.getId());
        data.put("processName", process.getName());
        data.put("deviceType", deviceType);
        if (deviceId != null) {
            data.put("deviceId", deviceId);
        }
        data.put("requestTime", process.getLastUpdateTime());
        data.put("newState", process.getState().name());
        
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
    
    // 添加新的端点：设备操作完成，唤醒进程
    @PostMapping("{id}/device-complete")
    public ResponseEntity<Map<String, Object>> deviceOperationComplete(@PathVariable Long id) {
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
        
        // 检查进程是否处于等待状态
        if (process.getState() != Process.ProcessState.WAITING) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "只有等待状态的进程可以被唤醒");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 唤醒进程，将状态从WAITING转为READY
        processScheduler.wakeUpProcess(process);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "设备操作完成，进程已唤醒");
        
        Map<String, Object> data = new HashMap<>();
        data.put("processId", process.getId());
        data.put("processName", process.getName());
        data.put("newState", process.getState().name());
        data.put("wakeupTime", process.getLastUpdateTime());
        
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
