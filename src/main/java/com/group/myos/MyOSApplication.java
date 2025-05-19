package com.group.myos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 操作系统模拟应用
 * 启用定时任务功能，用于处理中断等需要定时执行的任务
 */
@SpringBootApplication
@EnableScheduling  // 启用定时任务
public class MyOSApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyOSApplication.class, args);
    }

}
