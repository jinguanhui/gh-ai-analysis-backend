package com.jgh.aianalysis.manager.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jgh.aianalysis.service.ChatMessageService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class MysqlChatMemoryRepository implements ChatMemoryRepository {
    private ChatMessageService chatMessageService;


    public MysqlChatMemoryRepository(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @Override
    public List<String> findConversationIds() {
        List<String> list = chatMessageService.list().stream().map(ChatMessage::getConversationId).toList();
        return list.isEmpty() ? List.of() : list;
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getCreateTime);

        List<ChatMessage> chatMessages = chatMessageService.list(queryWrapper);

        // 按照时间顺序返回
        if (!chatMessages.isEmpty()) {
            Collections.reverse(chatMessages);
        }

        return chatMessages.stream()
                .map(chatMessage -> MessageAndChatMessageConverter.convertToMessage(chatMessage))
                .toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        List<ChatMessage> list = messages.stream()
                .map(message -> MessageAndChatMessageConverter.convertToChatMessage(message, conversationId))
                .toList();

        chatMessageService.saveBatch(list,list.size());
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getConversationId, conversationId);
        chatMessageService.remove(queryWrapper);
    }
}
