package com.group.myos.interrupt.handler;

import com.group.myos.interrupt.model.InterruptType;

/**
 * 中断处理接口，定义中断处理程序的契约。
 */
public interface InterruptHandler {
    /**
     * 处理中断。
     * @param type 中断类型
     * @param processId 相关进程ID
     * @param data 额外数据
     */
    void handleInterrupt(InterruptType type, Long processId, Object data);
}