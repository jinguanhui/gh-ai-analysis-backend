package com.jgh.aianalysis.ai.transform;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.stereotype.Component;

@Component
public class MyQueryTranslateTransform {

    private final QueryTransformer queryTransformer;

    public MyQueryTranslateTransform(ChatModel dashscopeChatModel) {

        ChatClient.Builder builder = ChatClient.builder(dashscopeChatModel);

        queryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(builder)
                .targetLanguage("chinese")
                .build();
    }

    public String translate(String Prompt) {
        Query query = queryTransformer.transform(new Query(Prompt));
        return query.text();
    }
}
