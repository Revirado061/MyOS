package com.group.myos.filesystem.controller;

import com.group.myos.filesystem.FileSystem;
import com.group.myos.filesystem.model.File;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/filesystem")
public class FileSystemController {
    @Resource
    private FileSystem fileSystem;

    // 获取当前目录路径
    @GetMapping("/current-path")
    public ResponseEntity<?> getCurrentPath() {
        return ResponseEntity.ok(fileSystem.getCurrentPath());
    }

    // 创建文件
    @PostMapping("/file")
    public ResponseEntity<?> createFile(@RequestParam String name) {
        boolean result = fileSystem.createFile(name);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "文件创建成功" : "文件已存在或创建失败");
        return ResponseEntity.ok(response);
    }

    // 创建目录
    @PostMapping("/directory")
    public ResponseEntity<?> createDirectory(@RequestParam String name) {
        boolean result = fileSystem.createDirectory(name);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "目录创建成功" : "目录已存在或创建失败");
        return ResponseEntity.ok(response);
    }

    // 删除文件
    @DeleteMapping("/file")
    public ResponseEntity<?> deleteFile(@RequestParam String name) {
        boolean result = fileSystem.deleteFile(name);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "文件删除成功" : "文件不存在或删除失败");
        return ResponseEntity.ok(response);
    }

    // 删除目录
    @DeleteMapping("/directory")
    public ResponseEntity<?> deleteDirectory(@RequestParam String name) {
        boolean result = fileSystem.deleteDirectory(name);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "目录删除成功" : "目录不存在或删除失败");
        return ResponseEntity.ok(response);
    }

    // 切换目录
    @PostMapping("/change-directory")
    public ResponseEntity<?> changeDirectory(@RequestParam String path) {
        boolean result = fileSystem.changeDirectory(path);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "目录切换成功" : "目录不存在或切换失败");
        return ResponseEntity.ok(response);
    }

    // 列出当前目录内容
    @GetMapping("/list")
    public ResponseEntity<?> listDirectory() {
        List<String> contents = fileSystem.listDirectory();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("contents", contents);
        return ResponseEntity.ok(response);
    }

    // 获取目录详细内容
    @GetMapping("/directory-content")
    public ResponseEntity<?> getDirectoryContent() {
        Map<String, Object> content = fileSystem.getDirectoryContent();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("content", content);
        return ResponseEntity.ok(response);
    }

    // 打开文件
    @PostMapping("/file/open")
    public ResponseEntity<?> openFile(@RequestParam String name) {
        boolean result = fileSystem.openFile(name);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "文件打开成功" : "文件不存在或打开失败");
        return ResponseEntity.ok(response);
    }

    // 关闭文件
    @PostMapping("/file/close")
    public ResponseEntity<?> closeFile(@RequestParam String name) {
        boolean result = fileSystem.closeFile(name);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "文件关闭成功" : "文件不存在或关闭失败");
        return ResponseEntity.ok(response);
    }

    // 读取文件内容
    @GetMapping("/file/content")
    public ResponseEntity<?> readFileContent(@RequestParam String name) {
        String content = fileSystem.readFileContent(name);
        Map<String, Object> response = new HashMap<>();
        if (content != null) {
            response.put("success", true);
            response.put("content", content);
        } else {
            response.put("success", false);
            response.put("message", "文件不存在或未打开");
        }
        return ResponseEntity.ok(response);
    }

    // 写入文件内容
    @PostMapping("/file/content")
    public ResponseEntity<?> writeFileContent(
            @RequestParam String name,
            @RequestBody(required = false) String content) {
        if (content == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "文件内容不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        boolean result = fileSystem.writeFileContent(name, content);
        Map<String, Object> response = new HashMap<>();
        response.put("success", result);
        response.put("message", result ? "文件写入成功" : "文件不存在或未打开");
        return ResponseEntity.ok(response);
    }
} 