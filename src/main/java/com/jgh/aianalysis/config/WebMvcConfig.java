package com.jgh.aianalysis.config;

import com.jgh.aianalysis.handler.MyInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private MyInterceptor myInterceptor;
//    @Override
//    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
//        // 配置内容协商，支持SSE流
//        configurer
//                .favorParameter(false)
//                .ignoreAcceptHeader(false)
//                .defaultContentType(MediaType.APPLICATION_JSON)
//                .mediaType("json", MediaType.APPLICATION_JSON)
//                .mediaType("xml", MediaType.APPLICATION_XML)
//                .mediaType("html", MediaType.TEXT_HTML)
//                .mediaType("txt", MediaType.TEXT_PLAIN)
//                .mediaType("event-stream", MediaType.TEXT_EVENT_STREAM)
//                ; // 支持SSE
//    }
//
//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
//        jsonConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
//        converters.add(0, jsonConverter);
//    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册自定义拦截器
        registry.addInterceptor(myInterceptor)
                .addPathPatterns("/**"); // 拦截所有请求
//                .excludePathPatterns(); // 排除不需要拦截的路径
    }
}
