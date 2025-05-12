package com.group.myos.interrupt.controller;

import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.Interrupt;
import com.group.myos.interrupt.model.InterruptLog;
import com.group.myos.interrupt.model.InterruptType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interrupts")
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
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            String message = (String) request.get("message");

            interruptManager.triggerInterrupt(1, type, null, message);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "中断已触发",
                "data", Map.of(
                    "type", type,
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
        
        InterruptType interruptType = type != null ? InterruptType.valueOf(type) : null;
        List<InterruptLog> logs = interruptManager.getInterruptLogs(limit, interruptType);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "中断日志已获取",
            "data", logs
        ));
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