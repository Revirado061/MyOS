package com.group.myos.filesystem.controller;

import com.group.myos.filesystem.FileSystem;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/file")
public class FileContoller {
    private static final Logger logger = LogManager.getLogger(FileContoller.class);

    @Resource
    private FileSystem fileSystem;
    // 文件系统API
    @PostMapping("")
    public boolean createFile(@RequestParam String name) {
        logger.info("Creating file: {}", name);
        boolean result = fileSystem.createFile(name);
        logger.info("File creation result: {}", result);
        return result;
    }

//    @DeleteMapping("{name}")
//    public boolean deleteFile(@PathVariable String name) {
//        logger.info("Deleting file: {}", name);
//        boolean result = fileSystem.deleteFile(name);
//        logger.info("File deletion result: {}", result);
//        return result;
//    }

    @GetMapping("")
    public Map<String, Object> listFiles() {
        logger.debug("Listing files in current directory");
        return fileSystem.getDirectoryContent();
    }

    @GetMapping("{name}/content")
    public String readFileContent(@PathVariable String name) {
        logger.info("Reading content of file: {}", name);
        return fileSystem.readFileContent(name);
    }

//    @PostMapping("{name}/content")
//    public boolean writeFileContent(@PathVariable String name, @RequestBody String content) {
//        logger.info("Writing content to file: {}", name);
//        boolean result = fileSystem.writeFileContent(name, content);
//        logger.info("File write result: {}", result);
//        return result;
//    }

    @PostMapping("directory")
    public boolean createDirectory(@RequestParam String name) {
        logger.info("Creating directory: {}", name);
        boolean result = fileSystem.createDirectory(name);
        logger.info("Directory creation result: {}", result);
        return result;
    }

    @PostMapping("change-directory")
    public boolean changeDirectory(@RequestParam String name) {
        logger.info("Changing directory to: {}", name);
        boolean result = fileSystem.changeDirectory(name);
        logger.info("Directory change result: {}", result);
        return result;
    }

    @DeleteMapping("/directory/{name}")
    public boolean deleteDirectory(@PathVariable String name) {
        logger.info("Deleting directory: {}", name);
        boolean result = fileSystem.deleteDirectory(name);
        logger.info("Directory deletion result: {}", result);
        return result;
    }
}
