package com.jgh.aianalysis.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jgh.aianalysis.manager.ai.AIManager;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.manager.SseEmitterManager;
import com.jgh.aianalysis.mapper.ChartMapper;
import com.jgh.aianalysis.service.ChartService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.ExcelUtils;
import com.jgh.aianalysis.utils.aliyun.AliyunOSSUtil;
import com.jgh.aianalysis.utils.aliyun.FileGreenUtil;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.dto.chart.GenChartByAiRequest;
import com.jgh.ghcommon.model.entity.Chart;
import com.jgh.ghcommon.model.entity.User;
import com.jgh.ghcommon.model.vo.BiResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    private AIManager aIManager;

    @Resource
    private SseEmitterManager sseEmitterManager;

    @Resource
    private AliyunOSSUtil aliyunOSSUtil;

    @Resource
    private FileGreenUtil fileGreenUtil;

    //  文件大小最多为1MB
    private final long MAX_FILE_SIZE = 1024 * 1024;

    //  合格的文件后缀
    private static final List<String> SUFFIX_ARRAY = List.of("xlsx");


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
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        //  校验文件大小、后缀、内容合规性（阿里云OSS对象存储的审核功能）
        String originalFilename = multipartFile.getOriginalFilename();
        long size = multipartFile.getSize();

        if (size > MAX_FILE_SIZE) {
            throw new BusinessException("文件超过1MB！");
        }

        String suffix = FileUtil.getSuffix(originalFilename);

        if (!SUFFIX_ARRAY.contains(suffix)) {
            throw new BusinessException("文件格式错误！");
        }




        // 创建SseEmitter，设置较长的超时时间
        BiResponse biResponse = new BiResponse();
        BaseResponse<BiResponse> baseResponse = new BaseResponse<>();


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
                    baseResponse.setMessage("请输入分析目标");
                    baseResponse.setCode(500);
                    sseEmitterManager.sendProgress(baseResponse);
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("请输入分析目标");
                }
                if (StringUtils.isBlank(name)) {
                    baseResponse.setCode(500);
                    baseResponse.setMessage("请输入分析表的名称");
                    sseEmitterManager.sendProgress(baseResponse);
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("请输入分析表的名称");
                }
                if (StringUtils.isNotBlank(name) && name.length() > 100) {
                    baseResponse.setCode(500);
                    baseResponse.setMessage("名称过长");
                    sseEmitterManager.sendProgress(baseResponse);
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("名称过长");
                }
                //  将文档上传到OSS
                String fileURL = null;
                try {
                    fileURL = aliyunOSSUtil.getFileURL(multipartFile, "file");
                } catch (IOException e) {
                    log.error("上传文件失败！", e);
                    baseResponse.setMessage("上传文件失败！！！");
                    baseResponse.setCode(500);
                    sseEmitterManager.sendProgress(baseResponse);
                    sseEmitterManager.removeEmitter(taskId);
                    e.printStackTrace();
                    throw new BusinessException("上传文件失败！！！");
                }

                if (fileURL == null) {
                    log.error("上传文件失败！");
                    baseResponse.setCode(500);
                    baseResponse.setMessage("上传文件失败！！！");
                    sseEmitterManager.sendProgress(baseResponse);
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("上传文件失败！！！");
                }

                //  对文档检测
                Map map = null;
                try {
                    map = fileGreenUtil.fileGreenCheck(fileURL, "file");
                } catch (Exception e) {
                    log.error("文件存在不合规内容！！！");
                    baseResponse.setCode(500);
                    baseResponse.setMessage("文件存在不合规内容！！！");
                    sseEmitterManager.sendProgress(baseResponse);
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("文件存在不合规内容！！！");
                }

                if (ObjectUtils.isEmpty(map) || !"pass".equals(map.get("suggestion"))) {
                    log.error("文件检测失败！！！内容不合规");
                    baseResponse.setCode(500);
                    baseResponse.setMessage("文件检测失败！！！内容不合规");
                    sseEmitterManager.sendProgress(baseResponse);
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("文件检测失败！！！内容不合规");
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

                String result = aIManager.doChat(goal,CsvData, chartType);
                String[] splits = result.split("【【【【【");
                if (splits.length < 3) {
                    baseResponse.setCode(500);
                    baseResponse.setMessage("AI生成错误！");
                    sseEmitterManager.sendProgress(baseResponse);
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
                    baseResponse.setCode(500);
                    baseResponse.setMessage("数据库插入错误！");
                    sseEmitterManager.sendProgress(baseResponse);
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
                    baseResponse.setCode(500);
                    baseResponse.setMessage("用户不存在！");
                    sseEmitterManager.sendProgress(baseResponse);
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("用户不存在！");
                }
                user.setInvokeCount(user.getInvokeCount() - 1);
                boolean b = userService.updateById(user);
                if (!b) {
                    baseResponse.setCode(500);
                    baseResponse.setMessage("数据库更新错误！");
                    sseEmitterManager.sendProgress(baseResponse);
                    sseEmitterManager.removeEmitter(taskId);
                    throw new BusinessException("数据库更新错误！");
                }

            } catch (Exception e) {
                // 发送错误信息
                baseResponse.setCode(500);
                baseResponse.setMessage("任务执行错误！");
                sseEmitterManager.sendProgress(baseResponse);
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




