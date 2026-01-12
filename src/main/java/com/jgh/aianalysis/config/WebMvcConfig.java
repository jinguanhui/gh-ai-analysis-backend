package com.jgh.aianalysis.config;
import com.jgh.aianalysis.handler.MyInterceptor;
import com.jgh.aianalysis.service.AccessKeyService;
import com.jgh.aianalysis.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {


    @Resource
    private MyInterceptor myInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(myInterceptor)
                .addPathPatterns("/**") // 添加拦截路径
        .excludePathPatterns("/api/user/login", "/api/user/register");// 排除拦截路径
    }
}
