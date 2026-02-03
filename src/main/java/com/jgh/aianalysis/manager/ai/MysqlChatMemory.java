package com.jgh.aianalysis.manager.ai;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Stream;

public class MysqlChatMemory implements ChatMemory {

    private static final int DEFAULT_MAX_MESSAGES = 20;
    private final MysqlChatMemoryRepository mysqlChatMemoryRepository;
    private final int maxMessages;

    public MysqlChatMemory(MysqlChatMemoryRepository mysqlChatMemoryRepository, int maxMessages) {
        Assert.notNull(mysqlChatMemoryRepository, "chatMemoryRepository cannot be null");
        Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
        this.mysqlChatMemoryRepository = mysqlChatMemoryRepository;
        this.maxMessages = maxMessages;
    }
    @Override
    public void add(String conversationId, Message message) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(message, "message cannot be null");
        this.add(conversationId, List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> memoryMessages = this.mysqlChatMemoryRepository.findByConversationId(conversationId);
        List<Message> processedMessages = this.process(memoryMessages, messages);
        this.mysqlChatMemoryRepository.saveAll(conversationId, processedMessages);
    }

    @Override
    public List<Message> get(String conversationId) {
        return this.mysqlChatMemoryRepository.findByConversationId(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        this.mysqlChatMemoryRepository.deleteByConversationId(conversationId);
    }

    private List<Message> process(List<Message> memoryMessages, List<Message> newMessages) {
        List<Message> processedMessages = new ArrayList();
        Set<Message> memoryMessagesSet = new HashSet(memoryMessages);
        Stream var10000 = newMessages.stream();
        Objects.requireNonNull(SystemMessage.class);
        boolean hasNewSystemMessage = var10000.filter(SystemMessage.class::isInstance).anyMatch((messagex) -> !memoryMessagesSet.contains(messagex));
        var10000 = memoryMessages.stream().filter((messagex) -> !hasNewSystemMessage || !(messagex instanceof SystemMessage));
        Objects.requireNonNull(processedMessages);
        var10000.forEach(messagex -> {
            processedMessages.add((Message) messagex);
        });
        processedMessages.addAll(newMessages);
        if (processedMessages.size() <= this.maxMessages) {
            return processedMessages;
        } else {
            int messagesToRemove = processedMessages.size() - this.maxMessages;
            List<Message> trimmedMessages = new ArrayList();
            int removed = 0;

            for(Message message : processedMessages) {
                if (!(message instanceof SystemMessage) && removed < messagesToRemove) {
                    ++removed;
                } else {
                    trimmedMessages.add(message);
                }
            }

            return trimmedMessages;
        }
    }
}
