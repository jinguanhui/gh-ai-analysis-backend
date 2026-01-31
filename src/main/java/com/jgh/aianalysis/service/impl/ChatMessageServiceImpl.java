package com.jgh.aianalysis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jgh.aianalysis.manager.ai.ChatMessage;
import com.jgh.aianalysis.service.ChatMessageService;
import com.jgh.aianalysis.mapper.ChatMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author 15180
* @description 针对表【chat_message(聊天消息表)】的数据库操作Service实现
* @createDate 2026-01-31 20:26:44
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService{

}




