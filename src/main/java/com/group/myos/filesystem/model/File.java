package com.group.myos.filesystem.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class File {
    public char[] content;
    private String name; // 文件名
    private int size; // 文件大小（单位：MB）
    private String path; // 文件路径
    private boolean isAllocated; // 标记文件是否已分配内存

    public File(String name) {
        this.name = name;
    }

    public File(String name, int size, String path) {
        this.name = name;
        this.size = size;
        this.path = path;
        this.isAllocated = false; // 默认未分配内存
    }

    public int calculateByteCount(String content) {
        return content.getBytes().length;
    }

    @Override
    public String toString() {
        return "File{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", path='" + path + '\'' +
                ", isAllocated=" + isAllocated +
                '}';
    }
}