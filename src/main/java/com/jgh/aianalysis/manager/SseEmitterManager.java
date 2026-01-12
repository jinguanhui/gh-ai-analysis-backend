package com.jgh.aianalysis.manager;

import com.google.gson.Gson;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.vo.BiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SseEmitterManager {
    // 线程安全的Map：taskId -> SseEmitter
    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    Gson gson = new Gson();

    /**
     * 创建SSE连接并缓存
     */
    public SseEmitter createEmitter(String taskId) {
        // 设置超时时间（1分钟，可根据业务调整）
        SseEmitter emitter = new SseEmitter(6000000L);
        emitterMap.put(taskId, emitter);
        log.info("创建SSE连接：taskId = " + taskId);

        // 连接关闭/超时/异常时，自动移除缓存
        emitter.onCompletion(() -> {
            log.info("sse任务完成");
            emitterMap.remove(taskId);
        });
        emitter.onTimeout(() -> {
            log.error("sse任务超时");
            emitterMap.remove(taskId);
            throw new BusinessException("sse任务超时");
        });
        emitter.onError((e) -> {
            log.error("sse任务异常");
            emitterMap.remove(taskId);
            throw new BusinessException("sse任务异常");
        });

        return emitter;
    }

    /**
     * 创建SSE连接并缓存
     */
    public SseEmitter getEmitter(String taskId) {
        // 设置超时时间（1分钟，可根据业务调整）
        SseEmitter sseEmitter = emitterMap.get(taskId);
        log.info("获取到SSE连接：taskId = " + taskId);
        return sseEmitter;
    }

    /**
     * 推送进度消息
     */
    public void sendProgress(BaseResponse baseResponse) {
        BiResponse biResponse = (BiResponse)baseResponse.getData();
        String taskId = biResponse.getTaskId();
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) {
            throw new BusinessException("获取SSE连接失败");
        }

        try {
            // 构造返回格式（与前端解析逻辑一致）
            // 发送JSON格式消息
            emitter.send(SseEmitter.event().data(gson.toJson(baseResponse)));
        } catch (Exception e) {
            // 发送失败：移除失效连接
            removeEmitter(taskId);
            throw new BusinessException("推送进度消息失败");
        }
    }

    /**
     * 推送错误消息
     */
    public void sendError(BiResponse biResponse) {
        String taskId = biResponse.getTaskId();
        SseEmitter emitter = emitterMap.get(taskId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event().data(gson.toJson(biResponse)));
        } catch (Exception e) {
            removeEmitter(taskId);
            throw new BusinessException("推送进度消息失败");
        }
    }

    /**
     * 移除并关闭连接
     */
    public void removeEmitter(String taskId) {
        SseEmitter emitter = emitterMap.remove(taskId);
        if (emitter != null) emitter.complete();
    }
}