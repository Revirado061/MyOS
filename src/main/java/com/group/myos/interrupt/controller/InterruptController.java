package com.group.myos.interrupt.controller;

import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.Interrupt;
import com.group.myos.interrupt.model.InterruptLog;
import com.group.myos.interrupt.model.InterruptType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequestMapping("/interrupts")
public class InterruptController {
    private final InterruptManager interruptManager;

    public InterruptController(InterruptManager interruptManager) {
        this.interruptManager = interruptManager;
    }

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerInterrupt(@RequestBody Map<String, Object> request) {
        try {
            String typeStr = (String) request.get("type");
            InterruptType type = InterruptType.valueOf(typeStr);
            String message = (String) request.get("message");
            Long deviceId = request.get("deviceId") != null ? 
                Long.valueOf(request.get("deviceId").toString()) : null;

            interruptManager.triggerInterrupt(1, type, deviceId, message);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "中断已触发",
                "data", Map.of(
                    "type", type,
                    "deviceId", deviceId,
                    "message", message
                )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "无效的中断类型"
            ));
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getInterruptLogs(
            @RequestParam(required = false, defaultValue = "10") int limit,
            @RequestParam(required = false) String type) {
        try {
            // 验证limit参数
            if (limit <= 0 || limit > 100) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "limit参数必须在1-100之间"
                ));
            }
            
            // 验证type参数
            InterruptType interruptType = null;
            if (type != null) {
                try {
                    interruptType = InterruptType.valueOf(type);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "无效的中断类型: " + type
                    ));
                }
            }
            
            // 获取日志
            List<InterruptLog> logs = interruptManager.getInterruptLogs(limit, interruptType);
            
            // 格式化返回数据
            List<Map<String, Object>> formattedLogs = logs.stream()
                .map(log -> {
                    Map<String, Object> logMap = new HashMap<>();
                    logMap.put("id", log.getId());
                    logMap.put("interruptId", log.getInterruptId());
                    logMap.put("type", log.getType());
                    logMap.put("timestamp", log.getTimestamp());
                    logMap.put("message", log.getMessage());
                    logMap.put("result", log.getResult());
                    return logMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "中断日志已获取",
                "data", Map.of(
                    "total", logs.size(),
                    "logs", formattedLogs
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "获取中断日志时发生错误: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/queue")
    public ResponseEntity<Map<String, Object>> getInterruptQueue() {
        List<Interrupt> queue = interruptManager.getInterruptQueue();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "中断队列已获取",
            "data", queue
        ));
    }
} 