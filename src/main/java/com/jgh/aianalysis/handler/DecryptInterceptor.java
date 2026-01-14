//package com.jgh.aianalysis.handler;
//
//import com.alibaba.fastjson.JSON;
//import com.jgh.aianalysis.utils.EncryptionUtils;
//import com.jgh.ghcommon.model.dto.Access.EncryptedRequest;
//import jakarta.servlet.ReadListener;
//import jakarta.servlet.ServletInputStream;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletRequestWrapper;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.method.HandlerMethod;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//import javax.crypto.SecretKey;
//import java.io.BufferedReader;
//import java.io.ByteArrayInputStream;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.security.PrivateKey;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Component
//public class DecryptInterceptor implements HandlerInterceptor {
//
//    // 从配置文件注入RSA私钥，用于解密AES密钥
//    @Value("${security.rsa.private-key}")
//    private String rsaPrivateKeyStr;
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        // 只处理带有@Decrypt注解的控制器方法
//        if (handler instanceof HandlerMethod) {
//            HandlerMethod handlerMethod = (HandlerMethod) handler;
//
//            // 读取请求体内容
//            String requestBody = request.getReader().lines().collect(Collectors.joining());
//
//            // 解析加密的请求对象
//            EncryptedRequest encryptedRequest = JSON.parseObject(requestBody, EncryptedRequest.class);
//
//            // 获取RSA私钥
//            PrivateKey rsaPrivateKey = EncryptionUtils.stringToRSAPrivateKey(rsaPrivateKeyStr);
//
//            // 解密AES密钥
//            String aesKeyStr = EncryptionUtils.decryptWithRSA(encryptedRequest.getEncryptedKey(), rsaPrivateKey);
//            SecretKey aesKey = EncryptionUtils.stringToAESKey(aesKeyStr);
//
//            // 获取初始化向量
//
//            // 使用AES密钥解密实际数据
//            String decryptedData = EncryptionUtils.decryptWithAES(encryptedRequest.getEncryptedData(), aesKey);
//
//            // 防重放攻击检查（可选）
//
//            log.debug("解密后的数据: {}", decryptedData);
//
//            // 创建一个包含解密数据的新BufferedReader，替换原有的请求Reader
//            request.setAttribute("DECRYPTED_DATA", decryptedData);
//
//            // 包装请求，使控制器能够读取解密后的数据
//            return wrapRequest(request, decryptedData);
//        }
//        return true;
//    }
//
//    /**
//     * 包装HttpServletRequest，替换请求体内容为解密后的数据
//     *
//     * @param request       原始请求
//     * @param decryptedData 解密后的数据
//     * @return 是否成功包装请求
//     */
//    private boolean wrapRequest(HttpServletRequest request, String decryptedData) {
//        try {
//            // 创建包装后的请求对象
//            DecryptedRequestWrapper wrapper = new DecryptedRequestWrapper(request, decryptedData);
//            // 替换当前请求
//            request.setAttribute("org.springframework.web.util.WebUtils.ERROR_EXCEPTION_ATTRIBUTE", wrapper);
//            return true;
//        } catch (Exception e) {
//            log.error("包装请求失败", e);
//            return false;
//        }
//    }
//
//    /**
//     * 自定义请求包装类，用于替换请求体内容
//     */
//    private static class DecryptedRequestWrapper extends HttpServletRequestWrapper {
//        private final String decryptedData;
//
//        public DecryptedRequestWrapper(HttpServletRequest request, String decryptedData) {
//            super(request);
//            this.decryptedData = decryptedData;
//        }
//
//        @Override
//        public BufferedReader getReader() {
//            return new BufferedReader(new InputStreamReader(
//                    new ByteArrayInputStream(decryptedData.getBytes(StandardCharsets.UTF_8))));
//        }
//
//        @Override
//        public ServletInputStream getInputStream() {
//            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
//                    decryptedData.getBytes(StandardCharsets.UTF_8));
//
//            return new ServletInputStream() {
//                @Override
//                public boolean isFinished() {
//                    return byteArrayInputStream.available() == 0;
//                }
//
//                @Override
//                public boolean isReady() {
//                    return true;
//                }
//
//                @Override
//                public void setReadListener(ReadListener readListener) {
//                    throw new UnsupportedOperationException();
//                }
//
//                @Override
//                public int read() {
//                    return byteArrayInputStream.read();
//                }
//            };
//        }
//    }
//}
