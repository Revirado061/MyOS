package com.group.myos.timer.controller;

import com.group.myos.timer.TimerManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 时钟控制器
 * 提供时钟管理相关的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/timer")
public class TimerController {

    @Autowired
    private TimerManager timerManager;

    /**
     * 启动系统时钟
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startTimer() {
        timerManager.start();
        Map<String, String> response = new HashMap<>();
        response.put("message", "系统时钟已启动");
        return ResponseEntity.ok(response);
    }

    /**
     * 停止系统时钟
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopTimer() {
        timerManager.stop();
        Map<String, String> response = new HashMap<>();
        response.put("message", "系统时钟已停止");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前系统时间
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Long>> getCurrentTime() {
        long currentTime = timerManager.getCurrentTime();
        Map<String, Long> response = new HashMap<>();
        response.put("currentTime", currentTime);
        return ResponseEntity.ok(response);
    }
} 