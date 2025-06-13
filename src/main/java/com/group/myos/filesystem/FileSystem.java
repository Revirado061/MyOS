package com.group.myos.filesystem;

import com.group.myos.memory.MemoryManager;
import com.group.myos.filesystem.model.Directory;
import com.group.myos.filesystem.model.File;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FileSystem {
    private final Directory root;
    private Directory currentDirectory;
    private final MemoryManager memoryManager;
    private static final int BLOCK_SIZE = 4096; // 4KB per block
    private final Map<Integer, byte[]> diskBlocks; // 模拟磁盘块
    private int nextBlockNumber;

    public FileSystem(MemoryManager memoryManager) {
        this.root = new Directory("/", null);
        this.currentDirectory = root;
        this.memoryManager = memoryManager;
        this.diskBlocks = new HashMap<>();
        this.nextBlockNumber = 0;
    }

    public boolean createFile(String name) {
        if (currentDirectory.getFiles().containsKey(name)) {
            return false;
        }
        String path = getCurrentPath() + "/" + name;
        File file = new File(name, 0, path);
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
            // 释放文件占用的磁盘块
            if (file.getBlockNumbers() != null) {
                for (int blockNumber : file.getBlockNumbers()) {
                    diskBlocks.remove(blockNumber);
                }
            }
            // 释放文件占用的内存
            if (file.isAllocated()) {
                memoryManager.freeMemoryForProcess(null); // 这里需要传入正确的Process对象
            }
            return true;
        }
        return false;
    }

    public boolean deleteDirectory(String name) {
        Directory dir = currentDirectory.getSubdirectories().get(name);
        if (dir != null) {
            // 递归删除目录下的所有文件和子目录
            deleteDirectoryRecursively(dir);
            currentDirectory.getSubdirectories().remove(name);
            return true;
        }
        return false;
    }

    private void deleteDirectoryRecursively(Directory dir) {
        // 删除所有文件
        for (File file : dir.getFiles().values()) {
            if (file.getBlockNumbers() != null) {
                for (int blockNumber : file.getBlockNumbers()) {
                    diskBlocks.remove(blockNumber);
                }
            }
            if (file.isAllocated()) {
                memoryManager.freeMemoryForProcess(null);
            }
        }
        dir.getFiles().clear();

        // 递归删除子目录
        for (Directory subDir : dir.getSubdirectories().values()) {
            deleteDirectoryRecursively(subDir);
        }
        dir.getSubdirectories().clear();
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

    public String readFileContent(String name) {
        File file = currentDirectory.getFiles().get(name);
        if (file == null || !file.isOpen()) {
            return null;
        }

        StringBuilder content = new StringBuilder();
        if (file.getBlockNumbers() != null) {
            for (int blockNumber : file.getBlockNumbers()) {
                byte[] block = diskBlocks.get(blockNumber);
                if (block != null) {
                    content.append(new String(block));
                }
            }
        }
        return content.toString();
    }

    public boolean writeFileContent(String name, String content) {
        File file = currentDirectory.getFiles().get(name);
        if (file == null || !file.isOpen()) {
            return false;
        }

        // 计算需要的块数
        int contentSize = content.getBytes().length;
        int requiredBlocks = (contentSize + BLOCK_SIZE - 1) / BLOCK_SIZE;

        // 释放原有块
        if (file.getBlockNumbers() != null) {
            for (int blockNumber : file.getBlockNumbers()) {
                diskBlocks.remove(blockNumber);
            }
        }

        // 分配新块
        int[] newBlockNumbers = new int[requiredBlocks];
        for (int i = 0; i < requiredBlocks; i++) {
            newBlockNumbers[i] = nextBlockNumber++;
        }
        file.setBlockNumbers(newBlockNumbers);

        // 写入内容
        byte[] contentBytes = content.getBytes();
        for (int i = 0; i < requiredBlocks; i++) {
            int start = i * BLOCK_SIZE;
            int end = Math.min(start + BLOCK_SIZE, contentBytes.length);
            byte[] blockContent = new byte[end - start];
            System.arraycopy(contentBytes, start, blockContent, 0, end - start);
            diskBlocks.put(newBlockNumbers[i], blockContent);
        }

        file.setSize(contentSize);
        file.setAllocated(true);
        return true;
    }

    public boolean openFile(String name) {
        File file = currentDirectory.getFiles().get(name);
        if (file != null && !file.isOpen()) {
            file.open();
            return true;
        }
        return false;
    }

    public boolean closeFile(String name) {
        File file = currentDirectory.getFiles().get(name);
        if (file != null && file.isOpen()) {
            file.close();
            return true;
        }
        return false;
    }

    public String getCurrentPath() {
        StringBuilder path = new StringBuilder();
        Directory dir = currentDirectory;
        while (dir != null && dir != root) {
            path.insert(0, "/" + dir.getName());
            dir = dir.getParent();
        }
        return path.length() == 0 ? "/" : path.toString();
    }

    public Map<String, Object> getDirectoryContent() {
        Map<String, Object> result = new HashMap<>();
        result.put("files", new ArrayList<>(currentDirectory.getFiles().values()));
        result.put("directories", new ArrayList<>(currentDirectory.getSubdirectories().keySet()));
        return result;
    }

    // 获取完整的目录树
    public Map<String, Object> getDirectoryTree() {
        Map<String, Object> tree = new HashMap<>();
        tree.put("name", root.getName());
        tree.put("type", "directory");
        tree.put("path", "/");
        tree.put("children", getDirectoryChildren(root));
        return tree;
    }

    private List<Map<String, Object>> getDirectoryChildren(Directory dir) {
        List<Map<String, Object>> children = new ArrayList<>();
        
        // 添加文件
        for (File file : dir.getFiles().values()) {
            Map<String, Object> fileNode = new HashMap<>();
            fileNode.put("name", file.getName());
            fileNode.put("type", "file");
            fileNode.put("path", file.getPath());
            fileNode.put("size", file.getSize());
            fileNode.put("isOpen", file.isOpen());
            fileNode.put("isAllocated", file.isAllocated());
            children.add(fileNode);
        }
        
        // 添加子目录
        for (Directory subDir : dir.getSubdirectories().values()) {
            Map<String, Object> dirNode = new HashMap<>();
            dirNode.put("name", subDir.getName());
            dirNode.put("type", "directory");
            dirNode.put("path", getDirectoryPath(subDir));
            dirNode.put("children", getDirectoryChildren(subDir));
            children.add(dirNode);
        }
        
        return children;
    }

    private String getDirectoryPath(Directory dir) {
        StringBuilder path = new StringBuilder();
        Directory current = dir;
        while (current != null && current != root) {
            path.insert(0, "/" + current.getName());
            current = current.getParent();
        }
        return path.length() == 0 ? "/" : path.toString();
    }
}