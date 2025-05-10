package com.group.myos.filesystem;

import com.group.myos.memory.MemoryManager;
import com.group.myos.filesystem.model.Directory;
import com.group.myos.filesystem.model.File;
import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FileSystem {
    private final Directory root;
    private Directory currentDirectory;
    private final MemoryManager memoryManager;

    public FileSystem(MemoryManager memoryManager) {
        this.root = new Directory("/", null);
        this.currentDirectory = root;
        this.memoryManager = memoryManager;
    }

    public boolean createFile(String name) {
        if (currentDirectory.getFiles().containsKey(name)) {
            return false;
        }
        File file = new File(name);
        currentDirectory.getFiles().put(name, file);
        return true;
    }

    public boolean createDirectory(String name) {
        if (currentDirectory.getSubdirectories().containsKey(name)) {
            return false;
        }
        Directory dir = new Directory(name, currentDirectory);
        currentDirectory.getSubdirectories().put(name, dir);
        return true;
    }

    public boolean deleteFile(String name) {
        File file = currentDirectory.getFiles().remove(name);
        if (file != null) {
            memoryManager.freeMemoryForFile(file);
            return true;
        }
        return false;
    }

    public boolean deleteDirectory(String name) {
        return currentDirectory.getSubdirectories().remove(name) != null;
    }

    public boolean changeDirectory(String path) {
        if (path.equals("/")) {
            currentDirectory = root;
            return true;
        }

        if (path.equals("..")) {
            if (currentDirectory.getParent() != null) {
                currentDirectory = currentDirectory.getParent();
                return true;
            }
            return false;
        }

        Directory dir = currentDirectory.getSubdirectories().get(path);
        if (dir != null) {
            currentDirectory = dir;
            return true;
        }

        return false;
    }

    public List<String> listDirectory() {
        List<String> contents = new ArrayList<>();
        contents.addAll(currentDirectory.getFiles().keySet());
        contents.addAll(currentDirectory.getSubdirectories().keySet());
        return contents;
    }

    public List<File> getFiles() {
        return new ArrayList<>(currentDirectory.getFiles().values());
    }

    public String readFileContent(String name) {
        File file = currentDirectory.getFiles().get(name);
        if (file != null && file.getContent() != null) {
            return new String(file.getContent());
        }
        return null; // 如果文件不存在或内容为空，返回 null
    }

    public boolean writeFileContent(String name, String content) {
        File file = currentDirectory.getFiles().get(name);
        if (file != null) {
            // 先释放原有内容占用的内存
            memoryManager.freeMemoryForFile(file);
            // 再尝试为新内容分配内存
            int newSize = content.getBytes().length;
            if (!memoryManager.allocateMemoryForFile(file, newSize)) {
                return false;
            }
            file.setContent(content.toCharArray()); // 确保内容被正确赋值
            file.setSize(file.calculateByteCount(content));
            return true;
        }
        return false;
    }

    public Map<String, Object> getDirectoryContent() {
        Map<String, Object> result = new HashMap<>();
        result.put("files", new ArrayList<>(currentDirectory.getFiles().values()));
        result.put("directories", new ArrayList<>(currentDirectory.getSubdirectories().keySet()));
        return result;
    }
}