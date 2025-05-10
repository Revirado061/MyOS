package com.group.myos.interrupt.manager;

import com.group.myos.interrupt.handler.InterruptHandler;
import com.group.myos.interrupt.model.InterruptType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中断管理器，负责中断的注册、触发和处理。
 */
@Component
public class InterruptManager {
    // 中断向量表，存储中断号与处理程序的映射
    private final Map<Integer, InterruptHandler> interruptVector = new ConcurrentHashMap<>();
    // 中断优先级表，存储中断号与优先级的映射
    private final Map<Integer, Integer> interruptPriorities = new ConcurrentHashMap<>();
    // 中断启用状态
    private volatile boolean interruptEnabled = true;

    /**
     * 注册中断处理程序。
     * @param vector 中断号
     * @param handler 中断处理程序
     * @param priority 中断优先级（0最高）
     */
    public void registerInterrupt(int vector, InterruptHandler handler, int priority) {
        interruptVector.put(vector, handler);
        interruptPriorities.put(vector, priority);
        System.out.println("注册中断 " + vector + "，优先级 " + priority);
    }

    /**
     * 触发中断。
     * @param vector 中断号
     * @param type 中断类型
     * @param processId 相关进程ID
     * @param data 额外数据
     */
    public void triggerInterrupt(int vector, InterruptType type, Long processId, Object data) {
        if (!interruptEnabled) {
            System.out.println("中断已禁用，忽略中断 " + vector);
            return;
        }

        InterruptHandler handler = interruptVector.get(vector);
        if (handler != null) {
            // 模拟保存上下文
            saveContext();

            try {
                // 执行中断处理程序
                handler.handleInterrupt(type, processId, data);
                System.out.println("处理中断 " + vector + "，类型 " + type);
            } finally {
                // 模拟恢复上下文
                restoreContext();
            }
        } else {
            System.out.println("未找到中断 " + vector + " 的处理程序");
        }
    }

    /**
     * 启用或禁用中断。
     * @param enabled 是否启用
     */
    public void setInterruptEnabled(boolean enabled) {
        this.interruptEnabled = enabled;
        System.out.println("中断状态设置为 " + (enabled ? "启用" : "禁用"));
    }

    /**
     * 模拟保存上下文。
     */
    private void saveContext() {
        System.out.println("保存当前上下文");
    }

    /**
     * 模拟恢复上下文。
     */
    private void restoreContext() {
        System.out.println("恢复上下文");
    }
}