package com.group.myos.filesystem;

import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FileSystem {
    private final Directory root;
    private Directory currentDirectory;
    
    public FileSystem() {
        this.root = new Directory("/", null);
        this.currentDirectory = root;
    }
    
    public boolean createFile(String name, int size) {
        if (currentDirectory.files.containsKey(name)) {
            return false;
        }
        
        File file = new File(name, size);
        currentDirectory.files.put(name, file);
        return true;
    }
    
    public boolean createDirectory(String name) {
        if (currentDirectory.subdirectories.containsKey(name)) {
            return false;
        }
        
        Directory dir = new Directory(name, currentDirectory);
        currentDirectory.subdirectories.put(name, dir);
        return true;
    }
    
    public boolean deleteFile(String name) {
        return currentDirectory.files.remove(name) != null;
    }
    
    public boolean deleteDirectory(String name) {
        return currentDirectory.subdirectories.remove(name) != null;
    }
    
    public boolean changeDirectory(String path) {
        if (path.equals("/")) {
            currentDirectory = root;
            return true;
        }
        
        if (path.equals("..")) {
            if (currentDirectory.parent != null) {
                currentDirectory = currentDirectory.parent;
                return true;
            }
            return false;
        }
        
        Directory dir = currentDirectory.subdirectories.get(path);
        if (dir != null) {
            currentDirectory = dir;
            return true;
        }
        
        return false;
    }
    
    public List<String> listDirectory() {
        List<String> contents = new ArrayList<>();
        contents.addAll(currentDirectory.files.keySet());
        contents.addAll(currentDirectory.subdirectories.keySet());
        return contents;
    }
    
    @Data
    public static class File {
        private final String name;
        private final int size;
        private byte[] content;
        
        public File(String name, int size) {
            this.name = name;
            this.size = size;
            this.content = new byte[size];
        }
    }
    
    @Data
    public static class Directory {
        private final String name;
        private final Directory parent;
        private final Map<String, File> files;
        private final Map<String, Directory> subdirectories;
        
        public Directory(String name, Directory parent) {
            this.name = name;
            this.parent = parent;
            this.files = new HashMap<>();
            this.subdirectories = new HashMap<>();
        }
    }
} 