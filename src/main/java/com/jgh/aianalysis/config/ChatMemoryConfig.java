package com.jgh.aianalysis.config;

import com.jgh.aianalysis.manager.ai.MysqlChatMemory;
import com.jgh.aianalysis.manager.ai.MysqlChatMemoryRepository;
import com.jgh.aianalysis.service.ChatMessageService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Autowired
    private ChatMessageService chatMessageService;

    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new MysqlChatMemoryRepository(chatMessageService);
    }

    @Bean
    public ChatMemory chatMemory(MysqlChatMemoryRepository chatMemoryRepository) {
        return new MysqlChatMemory(chatMemoryRepository, 20);
    }
}
