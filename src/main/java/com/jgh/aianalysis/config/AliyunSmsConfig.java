package com.jgh.aianalysis.config;

import com.aliyun.teaopenapi.models.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliyunSmsConfig {

    @Value("${aliyun.dypnsapi.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.dypnsapi.access-key-secret}")
    private String accessKeySecret;

    @Bean
    public com.aliyun.dypnsapi20170525.Client dypnsClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        config.endpoint = "dypnsapi.aliyuncs.com";
        return new com.aliyun.dypnsapi20170525.Client(config);
    }

}
