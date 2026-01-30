package com.jgh.aianalysis.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 配置ObjectMapper支持字节数组序列化
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL);
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
