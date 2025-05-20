package com.group.myos.memory.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageEntry {
    private int physicalPageNumber; // 物理页号
    private boolean isWrite;        // 写权限
    private boolean isRead;         // 读权限
    private boolean valid;          // 有效位
    private int lastVisitTime;      // 最后访问时间
    private boolean dirty;          // 脏位

    public PageEntry() {
        this.physicalPageNumber = -1;
        this.isWrite = false;
        this.isRead = false;
        this.valid = false;
        this.lastVisitTime = 0;
        this.dirty = false;
    }

    public PageEntry(int physicalPageNumber) {
        this.physicalPageNumber = physicalPageNumber;
        this.isWrite = true;
        this.isRead = true;
        this.valid = true;
        this.lastVisitTime = 0;
        this.dirty = false;
    }
} 