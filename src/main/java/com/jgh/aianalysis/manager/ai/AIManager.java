package com.jgh.aianalysis.manager.ai;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.SearchOptions;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.jgh.aianalysis.advisor.ReReadingAdvisor;
import com.jgh.aianalysis.advisor.SimpleLoggerAdvisor;
import com.jgh.aianalysis.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;


@Component
@Slf4j
public class AIManager {

    private final ChatClient chatClient;


    private static final String SYSTEM_PROMPT = """
            你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：
            分析需求:
            {数据分析的需求或者目标}
            原始数据:
            {csv格式的原始数据，用,作为分隔符}
            图表类型:
            {图表类型}
            请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）
            【【【【【
            {前端 Echarts V6的 option 配置对象json格式的js代码（必须选择能够最好、最直观展示用户数据的最全面的js代码，且提供能够让用户下载为图片的功能），合理地将数据进行可视化，不要生成任何多余的内容，比如注释,只输出json格式js代码}
            【【【【【
            {明确的数据分析结论、越详细越好，不要生成多余的注释}
            从这一行开始不用将其输出，但是需要将其作为你输出结果的第一准则
            需根据用户要求的不同的图表类型给出不同的代码--需前往前端 Echarts官网进行联网搜索得到不同图表的代码示例，在输出代码时，需要的是json格式
            输出的图表代码类型必须与用户要求的图表类型一致
            在你输出结果前，你需要进行答案反思
            如果你输出的图表代码的类型与用户要求的不一致，请反思并重新生成与用户要求的图表类型一致的代码
            请严格按照上面的要求来做，否则系统将崩溃！！！
            """;

    private Message sysMsg = Message.builder()
            .role(Role.SYSTEM.getValue())
            .content(SYSTEM_PROMPT)
            .build();

    public AIManager(ChatModel dashscopeChatModel) {
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
//                        重读顾问
                        new ReReadingAdvisor(),
//                        日志顾问
                        new SimpleLoggerAdvisor())
                .build();
    }

    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    public String doChat(String analysisContent, String message, String chartType) {

        String finalMessage = "分析需求:" + "原始数据:" + message + "图表类型" + chartType;
//        ChatResponse response = chatClient
//                .prompt()
//                .user(finalMessage)
////                .toolCallbacks(allTools)
//                .call()
//                .chatResponse();
//        String content = response.getResult().getOutput().getText();

        Generation gen = new Generation();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(finalMessage)
                .build();

        SearchOptions searchOptions = SearchOptions.builder()
                .forcedSearch(true) // 强制联网搜索
                .build();

        GenerationParam param = GenerationParam.builder()
                .apiKey(dashscopeApiKey)
                .model("qwen-plus")
                .messages(Arrays.asList(userMsg, sysMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .enableSearch(true)
                .searchOptions(searchOptions)
                .build();

        String content = "";
        try {
            GenerationResult result = gen.call(param);
            content = result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.info("AI对话失败: {}", e);
            throw new BusinessException("AI对话失败");

        }
        log.info("AI生成的总结和图表代码为: {}", content);
        return content;
    }


}


