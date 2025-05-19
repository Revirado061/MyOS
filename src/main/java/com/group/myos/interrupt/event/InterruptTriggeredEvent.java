package com.group.myos.interrupt.event;

import com.group.myos.interrupt.model.Interrupt;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 中断触发事件
 * 当系统中发生新的中断时，会发布此事件
 * 用于通知系统其他组件有新的中断需要处理
 */
@Getter
public class InterruptTriggeredEvent extends ApplicationEvent {
    /**
     * 触发的中断对象
     * 包含中断的详细信息，如类型、时间戳、数据等
     */
    private final Interrupt interrupt;

    /**
     * 构造函数
     * @param interrupt 触发的中断对象
     */
    public InterruptTriggeredEvent(Interrupt interrupt) {
        super(interrupt);
        this.interrupt = interrupt;
    }
} 