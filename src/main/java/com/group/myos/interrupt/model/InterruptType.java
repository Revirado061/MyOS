package com.group.myos.interrupt.model;

/**
 * 中断类型枚举，定义系统中支持的中断类型。
 * 优先级从高到低：ERROR > DEVICE > IO > PROCESS > CLOCK > OTHER
 */
public enum InterruptType {
    ERROR(1),    // 错误中断，最高优先级
    DEVICE(2),   // 设备中断
    IO(3),       // I/O中断
    PROCESS(4),  // 进程中断
    CLOCK(5),    // 时钟中断
    OTHER(6);    // 其他中断，最低优先级

    private final int priority;

    InterruptType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}