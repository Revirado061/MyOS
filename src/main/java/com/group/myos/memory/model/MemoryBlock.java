package com.group.myos.memory.model;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MemoryBlock {
    private int start;
    private int size;

    public MemoryBlock(int start, int size) {
        this.start = start;
        this.size = size;
    }

}
