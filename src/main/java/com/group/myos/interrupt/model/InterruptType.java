package com.group.myos.interrupt.model;

/**
 * 中断类型枚举，定义系统中支持的中断类型。
 */
public enum InterruptType {
    TIMER,      // 时钟中断
    IO,         // I/O中断
    ERROR       // 错误中断
}