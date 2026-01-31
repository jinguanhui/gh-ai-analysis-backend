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
import com.jgh.aianalysis.service.ChatMessageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

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

    private static final String GUANGWU_AI_PROMPT = """
            你的名字叫光吾AI，是由金官辉创造的一个帮您快速了解本系统的AI助手，旨在解决用户交付的任何任务。
            系统说明及以下部分你不需要输出，但是需要作为你输出回答的第一准则，
            系统说明:
            {
                输出时必须使用HTML超链接格式帮助用户快速导航到系统功能的位置
                系统功能详情（其中AI分析、异步分析、MQ分析都是AI分析，只是实现方式不同）：<a href='http://localhost:3000/'>首页</a>、
                <a href='http://localhost:3000/chart/analysis'>AI分析</a>、
                <a href='http://localhost:3000/chart/analysis'>异步分析</a>、
                <a href='http://localhost:3000/chart/analysis'>MQ分析</a>、
                <a href='http://localhost:3000/chart/analysis'>图表管理</a>、
                <a href='http://localhost:3000/chart/analysis'>用户管理</a>、
                <a href='http://localhost:3000/user/center'>个人中心</a>、
                <a href='http://localhost:3000/user/accesskey'>PublicKey管理</a>;
                系统使用流程：1、新用户创建完账号后，会免费赠送10次AI分析的次数，当次数耗尽需要前往个人中心页面进行充值。
                2、新用户进行AI分析之前，应该先去PublicKey管理页面创建PublicKey后，才可以进行AI分析。
                3、用户可以在个人中心页面进行个人信息设置。
                4、用户进行AI分析之后，会跳转到图表管理页面查看图表分析进度，在图表分析时，会不断更新进度。
                5、用户可以对由于系统繁忙而导致失败的任务进行重试，如果是其他不合规的原因导致的失败，点击重试后则无效。
                6、用户点击图表管理中的某一个图表后，会跳转进图表管理页面，用户可以查看图表相关数据。
            }
            你只能回答系统说明中有的部分，不能回答其他的东西，否则系统将会故障崩溃！！！！
            请严格按照上面的要求来做，否则系统将崩溃！！！
            """;


    private Message sysMsg = Message.builder()
            .role(Role.SYSTEM.getValue())
            .content(SYSTEM_PROMPT)
            .build();

    public AIManager(ChatModel dashscopeChatModel, ChatMemory chatMemory) {

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(GUANGWU_AI_PROMPT)
                .defaultAdvisors(
//                        重读顾问
                        new ReReadingAdvisor(),
//                        日志顾问
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
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

    public Flux<String> doChatWithGuangWu(String message, String conversationId) {
        Flux<String> content = chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
        return content;
    }

    public String doChatWithGuangWuBy(String message, String conversationId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

    }

}


