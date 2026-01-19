package com.jgh.aianalysis.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AnalysisAiTest {

    @Resource
    private com.jgh.aianalysis.manager.ai.AIManager AIManager;

    @Test
    void doChat() {
        String answer = AIManager.doChat("帮我分析","分析需求：\n" +
                "分析网站用户的增长情况\n" +
                "原始数据：\n" +
                "日期,用户数\n" +
                "1号,10\n" +
                "2号,20\n" +
                "3号,30\n", "line");
        System.out.println(answer);
    }
}