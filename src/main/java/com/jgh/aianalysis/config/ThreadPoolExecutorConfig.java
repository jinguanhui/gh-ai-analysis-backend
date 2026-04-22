package com.jgh.aianalysis.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableAsync
public class ThreadPoolExecutorConfig implements AsyncConfigurer {
    
    @Bean("asyncTaskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(10);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量
        executor.setQueueCapacity(200);
        // 线程存活时间
        executor.setKeepAliveSeconds(60);
        // 线程名前缀
        executor.setThreadNamePrefix("AsyncTask-");
        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化
        executor.initialize();
        return executor;
    }
    
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        // 创建一个线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            // 初始化线程数为 1
            private final AtomicInteger count = new AtomicInteger(1);

            @Override
            // 每当线程池需要创建新线程时，就会调用newThread方法
            // @NotNull Runnable r 表示方法参数 r 应该永远不为null，
            // 如果这个方法被调用的时候传递了一个null参数，就会报错
            public Thread newThread(@NotNull Runnable r) {
                // 创建一个新的线程
                Thread thread = new Thread(r);
                // 给新线程设置一个名称，名称中包含线程数的当前值
                thread.setName("线程" + count.getAndIncrement());
                // 返回新创建的线程
                return thread;
            }
        };

        return new ThreadPoolExecutor(
                2, // 核心线程数
                2, // 最大线程数
                60L, // 空闲线程存活时间
                java.util.concurrent.TimeUnit.SECONDS, // 时间单位
                new java.util.concurrent.LinkedBlockingQueue<>(2), // 工作队列
                threadFactory, // 线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
    }
}
