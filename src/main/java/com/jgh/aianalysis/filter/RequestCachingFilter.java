//package com.jgh.aianalysis.filter;
//
//import com.jgh.aianalysis.handler.CustomMultipartHttpServletRequest;
//import com.jgh.aianalysis.service.AccessKeyService;
//import jakarta.annotation.Resource;
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletRequestWrapper;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.io.BufferedReader;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.util.stream.Collectors;
//
//@Component
//@Order(1)
//public class RequestCachingFilter implements Filter {
//
//    @Resource
//    private AccessKeyService accessKeyService;
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//
//        // 对所有POST/PUT请求都进行处理，但multipart请求会特殊处理
//        if ("POST".equalsIgnoreCase(httpRequest.getMethod()) &&
//                httpRequest.getServletPath().equals("/chart/gen") &&
//                !(httpRequest.getContentType() != null &&
//                        httpRequest.getContentType().toLowerCase().startsWith("multipart/"))) {
//            CustomMultipartHttpServletRequest customMultipartHttpServletRequest = new
//                    CustomMultipartHttpServletRequest(httpRequest, accessKeyService);
//            chain.doFilter(customMultipartHttpServletRequest, response);
//        } else {
//            chain.doFilter(request, response);
//        }
//    }
//
//}
