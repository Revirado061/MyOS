package com.group.myos.interrupt.event;

import com.group.myos.interrupt.model.Interrupt;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 中断处理完成事件
 * 当中断处理程序完成对中断的处理后，会发布此事件
 * 用于通知系统其他组件中断已被处理完成
 */
@Getter
public class InterruptHandledEvent extends ApplicationEvent {
    /**
     * 被处理的中断对象
     * 包含中断的详细信息，如类型、时间戳、数据等
     */
    private final Interrupt interrupt;

    /**
     * 中断处理的结果
     * 描述中断处理的结果或状态
     */
    private final String result;

    /**
     * 中断处理的时间
     * 描述中断处理所花费的时间
     */
    private final long processingTime;

    /**
     * 构造函数
     * @param interrupt 被处理的中断对象
     * @param result 中断处理的结果
     * @param processingTime 中断处理的时间
     */
    public InterruptHandledEvent(Interrupt interrupt, String result, long processingTime) {
        super(interrupt);
        this.interrupt = interrupt;
        this.result = result;
        this.processingTime = processingTime;
    }
} 