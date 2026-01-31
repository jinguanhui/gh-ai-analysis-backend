package com.jgh.aianalysis.manager.ai;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class MessageAndChatMessageConverter {

    @Resource
    private ToolResponseMessage toolResponseMessage;


    /**
     * 将ChatMessage转换为Message
     * @param chatMessage
     * @return
     */
    public static Message convertToMessage(ChatMessage chatMessage) {
        MessageType messageType = MessageType.valueOf(chatMessage.getMessageType());
        String content = chatMessage.getContent();
        return switch (messageType) {
            case USER -> new UserMessage(content);
            case ASSISTANT -> new AssistantMessage(content);
            case SYSTEM -> new SystemMessage(content);
            case TOOL -> null;
        };
    }

    public static ChatMessage convertToChatMessage(Message message, String conversationId) {
        return ChatMessage
                .builder()
                .conversationId(conversationId)
                .messageType(String.valueOf(message.getMessageType()))
                .content(message.getText())
                .metadata(message.getMetadata().toString())
                .createTime(new Date())
                .updateTime(new Date())
                .build();
    }
}
