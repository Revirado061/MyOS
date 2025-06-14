package com.group.myos.process.controller;

import com.group.myos.process.ProcessSwapper;
import com.group.myos.process.model.Process;
import com.group.myos.process.ProcessScheduler;
import com.group.myos.process.model.ProcessTransition;
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

    // 添加新的端点：创建进程并立即启动（设置为READY状态）
    @PostMapping("/create-and-start")
    public Process createAndStartProcess(@RequestBody Process process) {
        // 直接创建进程，由ProcessScheduler决定是运行还是就绪
        Process newProcess = processScheduler.addProcess(process);
        
        // 如果进程状态为NEW，则根据ProcessScheduler的逻辑进行处理
        // 不再强制设为READY状态
        return newProcess;
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
    public ResponseEntity<?> getProcessInfo(@PathVariable Long id) {
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
        
        return ResponseEntity.ok(info);
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
        
        return ResponseEntity.ok(stats);
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
    public ResponseEntity<?> getProcessTransitions(@PathVariable Long id) {
        // 获取进程状态转换历史
        List<ProcessTransition> transitions = processScheduler.getProcessTransitionHistory(id);
        
        if (transitions.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "未找到进程状态转换历史");
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.ok(transitions);
    }
    
    /**
     * 控制自动调度功能
     */
    @PostMapping("/auto-schedule")
    public ResponseEntity<Map<String, Object>> setAutoSchedule(@RequestParam boolean enabled) {
        processScheduler.setAutoScheduleEnabled(enabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", enabled ? "自动调度已启用" : "自动调度已禁用");
        response.put("autoScheduleEnabled", processScheduler.isAutoScheduleEnabled());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取自动调度状态
     */
    @GetMapping("/auto-schedule")
    public ResponseEntity<Map<String, Object>> getAutoScheduleStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("autoScheduleEnabled", processScheduler.isAutoScheduleEnabled());
        return ResponseEntity.ok(response);
    }
}
