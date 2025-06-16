package com.group.myos.process.controller;

import com.group.myos.process.ProcessScheduler;
import com.group.myos.process.ProcessSwapper;
import com.group.myos.process.model.Process;
import com.group.myos.device.manager.DeviceManager;
import com.group.myos.device.model.Device;
import com.group.myos.interrupt.model.InterruptType;
import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.timer.TimerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/process")
public class ProcessController {
    
    private final ProcessScheduler processScheduler;
    private final ProcessSwapper processSwapper;
    private final DeviceManager deviceManager;
    private final InterruptManager interruptManager;
    private final TimerManager timerManager;
    private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);
    
    @Autowired
    public ProcessController(
            ProcessScheduler processScheduler, 
            ProcessSwapper processSwapper,
            DeviceManager deviceManager,
            InterruptManager interruptManager,
            TimerManager timerManager) {
        this.processScheduler = processScheduler;
        this.processSwapper = processSwapper;
        this.deviceManager = deviceManager;
        this.interruptManager = interruptManager;
        this.timerManager = timerManager;
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
        
        // 直接调用进程调度器终止进程
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
        Process process = processScheduler.getProcessById(id);
        if (process == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "未找到进程: " + id);
            return ResponseEntity.badRequest().body(response);
        }
        
        // 使用ProcessScheduler的方法更新优先级
        processScheduler.updateProcessPriority(id, priority);
        
        // 获取更新后的进程信息
        process = processScheduler.getProcessById(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "进程优先级已更新");
        response.put("data", process);
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

    /**
     * 设置调度算法
     */
    @PutMapping("/scheduling-algorithm")
    public ResponseEntity<Map<String, Object>> setSchedulingAlgorithm(@RequestParam String algorithm) {
        try {
            ProcessScheduler.SchedulingAlgorithm schedulingAlgorithm = 
                ProcessScheduler.SchedulingAlgorithm.valueOf(algorithm.toUpperCase());
            
            processScheduler.setSchedulingAlgorithm(schedulingAlgorithm);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "调度算法已更新为: " + algorithm);
            response.put("data", Map.of(
                "algorithm", algorithm,
                "description", algorithm.equals("FCFS") ? "先到先服务" : "优先级调度"
            ));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "无效的调度算法: " + algorithm);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取当前调度算法
     */
    @GetMapping("/scheduling-algorithm")
    public ResponseEntity<Map<String, Object>> getSchedulingAlgorithm() {
        ProcessScheduler.SchedulingAlgorithm algorithm = processScheduler.getCurrentAlgorithm();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "操作成功");
        response.put("data", Map.of(
            "algorithm", algorithm.name(),
            "description", algorithm == ProcessScheduler.SchedulingAlgorithm.FCFS ? "先到先服务" : "优先级调度"
        ));
        return ResponseEntity.ok(response);
    }

    /**
     * 触发进程中断
     */
    @PostMapping("/{id}/interrupt")
    public ResponseEntity<Map<String, Object>> interruptProcess(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            Process process = processScheduler.getProcessById(id);
            if (process == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "进程不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查进程是否处于运行状态
            if (!process.equals(processScheduler.getCurrentProcess()) || process.getState() != Process.ProcessState.RUNNING) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "进程不在运行状态，无法触发中断");
                return ResponseEntity.badRequest().body(response);
            }
            
            processScheduler.interruptProcess(id, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "进程中断已触发");
            response.put("data", Map.of(
                "processId", id,
                "reason", reason,
                "description", reason.equals("IO") ? "IO中断：进程变为等待状态" : "进程中断"
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "触发中断失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 检查中断原因是否有效
     */
    private boolean isValidInterruptReason(String reason) {
        return getValidInterruptReasons().contains(reason);
    }
    
    /**
     * 获取有效的中断原因列表
     */
    private List<String> getValidInterruptReasons() {
        return Arrays.asList(
            InterruptType.CLOCK.name(),     // 时钟中断，时间片用完，进程变为就绪状态
            InterruptType.PROCESS.name(),   // 进程中断，高优先级进程到达，当前进程变为就绪状态
            InterruptType.IO.name(),        // IO中断，进程变为等待状态
            InterruptType.DEVICE.name(),    // 设备中断，进程变为等待状态
            InterruptType.ERROR.name()      // 错误中断，进程被终止
        );
    }
    
    /**
     * 获取中断原因的描述
     */
    private String getInterruptReasonDescription(String reason) {
        Map<String, String> descriptions = Map.of(
            InterruptType.CLOCK.name(), "时钟中断：时间片用完，进程变为就绪状态",
            InterruptType.PROCESS.name(), "进程中断：高优先级进程到达，当前进程变为就绪状态",
            InterruptType.IO.name(), "IO中断：进程变为等待状态",
            InterruptType.DEVICE.name(), "设备中断：进程变为等待状态",
            InterruptType.ERROR.name(), "错误中断：进程被终止"
        );
        return descriptions.getOrDefault(reason, "未知中断原因");
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
            processScheduler.updateProcessPriority(id, priority);
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

    /**
     * 请求设备
     * @param id 进程ID
     * @param deviceId 设备ID
     * @return 设备分配结果
     */
    @PostMapping("{id}/request-device")
    public ResponseEntity<Map<String, Object>> requestDevice(
            @PathVariable Long id,
            @RequestParam Long deviceId) {
        Process process = processScheduler.getProcessById(id);
        if (process == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "进程不存在"
            ));
        }

        // 检查进程是否处于运行状态
        if (process.getState() != Process.ProcessState.RUNNING) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "只有运行状态的进程才能请求设备"
            ));
        }

        // 启动系统时钟（如果还没有启动）
        timerManager.start();
        logger.info("系统时钟已启动，用于设备超时检查");

        // 调用设备管理器分配设备
        boolean allocated = deviceManager.allocateDevice(deviceId, id, 30); // 默认超时时间30秒
        Device device = deviceManager.getDevice(deviceId);
        if (allocated && device != null) {
            // 设备分配成功
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "设备分配成功",
                "data", Map.of(
                    "process", process,
                    "device", device
                )
            ));
        } else {
            // 设备分配失败，进程进入等待状态
            processScheduler.blockProcess(id, "等待设备: " + deviceId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "设备分配失败，进程进入等待状态",
                "data", Map.of(
                    "process", process,
                    "deviceId", deviceId
                )
            ));
        }
    }
    
    /**
     * 释放设备
     * @param id 进程ID
     * @param deviceId 设备ID
     * @return 设备释放结果
     */
    @PostMapping("{id}/release-device")
    public ResponseEntity<Map<String, Object>> releaseDevice(
            @PathVariable Long id,
            @RequestParam Long deviceId) {
        Process process = processScheduler.getProcessById(id);
        if (process == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "进程不存在"
            ));
        }

        // 调用设备管理器释放设备
        Device device = deviceManager.releaseDevice(deviceId);
        if (device != null) {
            // 如果进程正在等待该设备，则唤醒进程
            if (process.getState() == Process.ProcessState.WAITING && 
                process.getWaitingReason() != null && 
                process.getWaitingReason().startsWith("等待设备: " + deviceId)) {
                processScheduler.wakeupProcess(id);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "设备释放成功",
                "data", Map.of(
                    "process", process,
                    "device", device
                )
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "设备释放失败，可能进程未持有该设备"
            ));
        }
    }
}
