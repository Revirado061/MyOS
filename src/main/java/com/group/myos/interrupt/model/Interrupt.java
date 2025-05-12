package com.group.myos.interrupt.model;

import lombok.Data;
import java.util.Map;

@Data
public class Interrupt {
    private Long id;                    // 中断ID
    private InterruptType type;         // 中断类型
    private Long timestamp;             // 触发时间
    private Map<String, Object> data;   // 附加数据
    private String message;             // 日志消息

    public Interrupt(Long id, InterruptType type, Long timestamp, Map<String, Object> data, String message) {
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
        this.data = data;
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }
} 