package com.jgh.aianalysis.manager.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "dubbo.protocol.port=-1" // 使用随机端口
})
class AIManagerTest {

    @Resource
    private AIManager aiManager;

    @Test
    void doChatWithGuangWu() {
        String conversationId = "1234";
        String s2 = aiManager.doChatWithGuangWuBy("你还记得我叫什么吗", conversationId);
        assertNotNull( s2);
    }
}