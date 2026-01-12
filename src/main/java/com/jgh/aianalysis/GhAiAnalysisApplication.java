package com.jgh.aianalysis;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.jgh.aianalysis.mapper")
@EnableAspectJAutoProxy
@EnableDubbo
public class GhAiAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(GhAiAnalysisApplication.class, args);
    }

}
