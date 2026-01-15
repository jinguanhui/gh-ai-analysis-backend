package com.jgh.aianalysis.service.impl;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jgh.aianalysis.ai.AnalysisAi;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.manager.SseEmitterManager;
import com.jgh.aianalysis.mapper.ChartMapper;
import com.jgh.aianalysis.service.ChartService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.ExcelUtils;
import com.jgh.ghcommon.common.AccessKeyEnum;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.dto.chart.GenChartByAiRequest;
import com.jgh.ghcommon.model.entity.Chart;
import com.jgh.ghcommon.model.entity.User;
import com.jgh.ghcommon.model.vo.BiResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author jgh
 */
@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Resource
    private UserService userService;

    @Resource
    private AnalysisAi analysisAi;

    @Resource
    private SseEmitterManager sseEmitterManager;


    /**
     * 生成图表
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @Override
    public BaseResponse<BiResponse> genChartByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        // 创建SseEmitter，设置较长的超时时间
        BiResponse biResponse = new BiResponse();
        BaseResponse<BiResponse> baseResponse = new BaseResponse<>();
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 1. 生成唯一taskId
        String taskId = UUID.randomUUID().toString().substring(0, 8);

        sseEmitterManager.createEmitter(taskId);
        log.info(request.getHeader("userId"));
        Long userId = Long.valueOf(request.getHeader("userId"));

        // 启动异步任务进行图表生成
        CompletableFuture.runAsync(() -> {
            try {
                // 立即复制文件到安全位置
                byte[] fileBytes = multipartFile.getBytes();
                Long userId2 = userId;
                // 1. 验证参数（10%）
                log.info("开始处理参数...");
                biResponse.setTaskInfo("正在处理参数...");
                biResponse.setTaskProcess(10);
                biResponse.setTaskId(taskId);
                baseResponse.setData(biResponse);
                baseResponse.setCode(200);
                sseEmitterManager.sendProgress(baseResponse);
                if (StringUtils.isBlank(goal)) {
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("请输入分析目标");
                }
                if (StringUtils.isBlank(name)) {
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("请输入分析表的名称");
                }
                if (StringUtils.isNotBlank(name) && name.length() > 100) {
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("名称过长");
                }

                // 2. 文件处理（30%）
                log.info("开始处理Excel文件...");
                biResponse.setTaskInfo("正在处理Excel文件...");
                biResponse.setTaskProcess(30);
                baseResponse.setData(biResponse);
                baseResponse.setCode(200);
                sseEmitterManager.sendProgress(baseResponse);

                String CsvData = ExcelUtils.excelToCsvFromBytes(fileBytes);

                // 3. AI分析（60%）
                log.info("开始进行AI分析...");
                biResponse.setTaskInfo("正在进行AI分析...");
                biResponse.setTaskProcess(60);
                baseResponse.setData(biResponse);
                baseResponse.setCode(200);

                sseEmitterManager.sendProgress(baseResponse);

                String result = analysisAi.doChat(CsvData, chartType);
                String[] splits = result.split("【【【【【");
                if (splits.length < 3) {
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("AI生成错误！");
                }



                // 4. 保存数据（80%）
                log.info("正在保存数据...");
                biResponse.setTaskInfo("正在保存数据...");
                biResponse.setTaskProcess(80);
                baseResponse.setCode(200);
                baseResponse.setData(biResponse);
                sseEmitterManager.sendProgress(baseResponse);
                String genChart = splits[1].trim();
                String genResult = splits[2].trim();

                Chart chart = new Chart();
                chart.setName(name);
                chart.setGoal(goal);
                chart.setChartData(CsvData);
                chart.setChartType(chartType);
                chart.setGenChart(genChart);
                chart.setGenResult(genResult);
                chart.setUserId(userId);


                boolean saveResult = this.save(chart);
                if (!saveResult) {
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("数据库插入错误！");
                }
                log.info("数据保存成功...");


                // 5. 完成任务（100%）
                log.info("任务完成...");
                biResponse.setTaskInfo("完成任务...");
                biResponse.setTaskProcess(100);
                biResponse.setChartId(chart.getId());
                biResponse.setGenChart(genChart);
                biResponse.setGenResult(genResult);
                baseResponse.setData(biResponse);
                baseResponse.setCode(200);
                sseEmitterManager.sendProgress(baseResponse);

                User user = userService.getById(userId2);
                if (user == null) {
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("用户不存在！");
                }
                user.setInvokeCount(user.getInvokeCount() - 1);
                boolean b = userService.updateById(user);
                if (!b) {
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("数据库更新错误！");
                }

            } catch (Exception e) {
                // 发送错误信息
                sseEmitterManager.removeEmitter(taskId);
                throw new BusinessException("任务执行错误！");
            } finally {
                // 确保连接关闭
                sseEmitterManager.removeEmitter(taskId);
            }
        });

        biResponse.setTaskId(taskId);

        baseResponse.setData(biResponse);
        baseResponse.setCode(200);

        return baseResponse;
    }
}




