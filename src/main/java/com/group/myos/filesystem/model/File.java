package com.group.myos.filesystem.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class File {
    private String name;           // 文件名
    private int size;             // 文件大小（字节）
    private String path;          // 文件路径
    private boolean isOpen;       // 文件是否打开
    private int[] blockNumbers;   // 文件占用的块号（索引分配方式）
    private int currentPosition;  // 当前读写位置
    private char[] content;       // 文件内容
    private boolean isAllocated;  // 是否已分配内存

    public File(String name) {
        this.name = name;
        this.size = 0;
        this.isOpen = false;
        this.currentPosition = 0;
        this.blockNumbers = new int[0];
        this.isAllocated = false;
    }

    public File(String name, int size, String path) {
        this.name = name;
        this.size = size;
        this.path = path;
        this.isOpen = false;
        this.currentPosition = 0;
        this.blockNumbers = new int[0];
        this.isAllocated = false;
    }

    public int calculateByteCount(String content) {
        return content.getBytes().length;
    }

    public void open() {
        this.isOpen = true;
        this.currentPosition = 0;
    }

    public void close() {
        this.isOpen = false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setBlockNumbers(int[] blockNumbers) {
        this.blockNumbers = blockNumbers;
    }

    public int[] getBlockNumbers() {
        return blockNumbers;
    }

    public void setCurrentPosition(int position) {
        if (position >= 0 && position <= size) {
            this.currentPosition = position;
        }
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public String toString() {
        return "File{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", path='" + path + '\'' +
                ", isOpen=" + isOpen +
                ", isAllocated=" + isAllocated +
                '}';
    }
}