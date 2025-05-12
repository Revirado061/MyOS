package com.group.myos.interrupt.model;

/**
 * 中断类型枚举，定义系统中支持的中断类型。
 */
public enum InterruptType {
    CLOCK,   // 时钟中断
    DEVICE,  // 设备中断
    IO,      // I/O中断
    PROCESS, // 进程中断
    ERROR,   // 错误中断
    OTHER    // 其他中断
}