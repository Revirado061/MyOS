package com.group.myos.interrupt.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InterruptLog {
    private Long id;                    // 日志ID
    private Long interruptId;           // 中断ID
    private InterruptType type;         // 中断类型
    private Long timestamp;             // 记录时间
    private String message;             // 日志消息
    private String result;              // 处理结果

    public InterruptLog(Long id, Long interruptId, InterruptType type, Long timestamp, String message, String result) {
        this.id = id;
        this.interruptId = interruptId;
        this.type = type;
        this.timestamp = timestamp;
        this.message = message;
        this.result = result;
    }

    public Long getInterruptId() {
        return interruptId;
    }
} 