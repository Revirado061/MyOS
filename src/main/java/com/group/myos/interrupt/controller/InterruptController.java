package com.group.myos.interrupt.controller;

import com.group.myos.interrupt.manager.InterruptManager;
import com.group.myos.interrupt.model.InterruptType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interrupt")
public class InterruptController {
    private final InterruptManager interruptManager;

    public InterruptController(InterruptManager interruptManager) {
        this.interruptManager = interruptManager;
    }

    @PostMapping("/trigger")
    public void triggerInterrupt(@RequestParam int vector,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) Long processId,
                                 @RequestParam(required = false) String data) {
        InterruptType interruptType = null;
        if (type != null) {
            try {
                interruptType = InterruptType.valueOf(type);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("无效的中断类型: " + type);
            }
        }
        interruptManager.triggerInterrupt(vector, interruptType, processId, data);
    }
} 