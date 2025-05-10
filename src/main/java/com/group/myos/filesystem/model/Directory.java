package com.group.myos.filesystem.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Directory {
    private String name;
    private Directory parent;
    private Map<String, File> files;
    private Map<String, Directory> subdirectories;

    public Directory(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
        this.files = new HashMap<>();
        this.subdirectories = new HashMap<>();
    }
}