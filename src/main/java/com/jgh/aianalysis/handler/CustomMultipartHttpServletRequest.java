package com.jgh.aianalysis.handler;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jgh.aianalysis.service.AccessKeyService;
import com.jgh.aianalysis.utils.EncryptionUtils;
import com.jgh.ghcommon.model.entity.AccessKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.*;

@Slf4j
public class CustomMultipartHttpServletRequest extends HttpServletRequestWrapper implements MultipartHttpServletRequest {

    private final AccessKeyService accessKeyService;
    private Map<String, MultipartFile> modifiedFileMap;
    private Map<String, String[]> modifiedParameterMap;

    public CustomMultipartHttpServletRequest(HttpServletRequest request, AccessKeyService accessKeyService) {
        super(request);
        this.accessKeyService = accessKeyService;
        this.modifiedParameterMap = new HashMap<>();

        // 复制原有的参数
        for (Map.Entry<String, String[]> entry : super.getParameterMap().entrySet()) {
            modifiedParameterMap.put(entry.getKey(), entry.getValue());
        }

        // 执行解密并更新参数
        decryptAndModifyParameters(request);

        // 保留原有的文件部分
        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            this.modifiedFileMap = new HashMap<>(multipartRequest.getFileMap());
        } else {
            this.modifiedFileMap = new HashMap<>();
        }
    }


    private void decryptAndModifyParameters(HttpServletRequest request) {
        try {
            // 获取encryptedKey和encryptedData参数
            String encryptedKey = getParameter("encryptedKey");
            String encryptedData = getParameter("encryptedData");

            if (encryptedKey != null && encryptedData != null) {
                // 获取用户ID
                Long userId = Long.valueOf(request.getHeader("userId"));

                // 查询访问密钥
                QueryWrapper<AccessKey> wrapper = new QueryWrapper<>();
                wrapper.eq("userId", userId);
                AccessKey accessKey = accessKeyService.getOne(wrapper);

                if (accessKey != null) {
                    String rsaPrivateKeyStr = accessKey.getPrivateKey();

                    // 获取RSA私钥
                    PrivateKey rsaPrivateKey = EncryptionUtils.stringToRSAPrivateKey(rsaPrivateKeyStr);

                    // 解密AES密钥
                    String aesKeyStr = EncryptionUtils.decryptWithRSA(encryptedKey, rsaPrivateKey);
                    SecretKey aesKey = EncryptionUtils.stringToAESKey(aesKeyStr);

                    // 解密实际数据
                    String decryptedData = EncryptionUtils.decryptWithAES(encryptedData, aesKey);

                    log.info("解密后的数据: {}", decryptedData);

                    // 解析解密后的JSON数据
                    Map<String, Object> decryptedParams = JSON.parseObject(decryptedData, Map.class);

                    // 将解密后的参数合并到参数映射中，但跳过文件参数
                    for (Map.Entry<String, Object> entry : decryptedParams.entrySet()) {
                        String key = entry.getKey();

                        // 跳过文件相关的参数，保留原始文件
                        if ("file".equals(key)) {
                            continue;
                        }

                        Object value = entry.getValue();

                        if (value instanceof String) {
                            modifiedParameterMap.put(key, new String[]{(String) value});
                        } else if (value instanceof List) {
                            List<?> list = (List<?>) value;
                            String[] stringArray = list.stream()
                                    .map(Object::toString)
                                    .toArray(String[]::new);
                            modifiedParameterMap.put(key, stringArray);
                        } else {
                            modifiedParameterMap.put(key, new String[]{value.toString()});
                        }
                    }

                    // 移除已解密的加密参数
                    modifiedParameterMap.remove("encryptedKey");
                    modifiedParameterMap.remove("encryptedData");

                } else {
                    throw new RuntimeException("未找到用户的访问密钥");
                }
            }

        } catch (Exception e) {
            log.error("解密过程出错", e);
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getParameter(String name) {
        String[] values = modifiedParameterMap.get(name);
        return (values != null && values.length > 0) ? values[0] : super.getParameter(name);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = modifiedParameterMap.get(name);
        return (values != null) ? values : super.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        // 合并修改过的参数和原有的参数
        Map<String, String[]> combinedMap = new HashMap<>(modifiedParameterMap);

        // 添加原始参数（如果不在修改后的参数中）
        for (Map.Entry<String, String[]> entry : super.getParameterMap().entrySet()) {
            if (!combinedMap.containsKey(entry.getKey())) {
                combinedMap.put(entry.getKey(), entry.getValue());
            }
        }

        return Collections.unmodifiableMap(combinedMap);
    }

    @Override
    public Iterator<String> getFileNames() {
        return null;
    }

    @Override
    public MultipartFile getFile(String name) {
        return modifiedFileMap.get(name);
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        if (super.getRequest() instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest) super.getRequest()).getPart(name);
        }
        return null;
    }

    @Override
    public java.util.Collection<Part> getParts() throws IOException, ServletException {
        if (super.getRequest() instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest) super.getRequest()).getParts();
        }
        return new ArrayList<>();
    }

    @Override
    public Map<String, MultipartFile> getFileMap() {
        return Collections.unmodifiableMap(modifiedFileMap);
    }

    @Override
    public MultiValueMap<String, MultipartFile> getMultiFileMap() {
        return null;
    }

    @Override
    public List<MultipartFile> getFiles(String name) {
        List<MultipartFile> files = new ArrayList<>();
        for (Map.Entry<String, MultipartFile> entry : modifiedFileMap.entrySet()) {
            if (entry.getKey().equals(name)) {
                files.add(entry.getValue());
            }
        }
        return files;
    }

    @Override
    public String getMultipartContentType(String paramOrFileName) {
        if (super.getRequest() instanceof MultipartHttpServletRequest) {
            return ((MultipartHttpServletRequest) super.getRequest()).getMultipartContentType(paramOrFileName);
        }
        return null;
    }

    @Override
    public HttpMethod getRequestMethod() {
        return null;
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        return null;
    }

    @Override
    public HttpHeaders getMultipartHeaders(String paramOrFileName) {
        return null;
    }
}
