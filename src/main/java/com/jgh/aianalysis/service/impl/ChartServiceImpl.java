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
        String taskId = UUID.randomUUID().toString();

        sseEmitterManager.createEmitter(taskId);
        log.info(request.getHeader("userId"));
        Long userId = Long.valueOf(request.getHeader("userId"));

        validInspection(request, userId);

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
                    throw new BusinessException("请输入分析目标");
                }
                if (StringUtils.isBlank(name)) {
                    throw new BusinessException("请输入分析表的名称");
                }
                if (StringUtils.isNotBlank(name) && name.length() > 100) {
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
                    throw new BusinessException("用户不存在！");
                }
                user.setInvokeCount(user.getInvokeCount() - 1);
                boolean b = userService.updateById(user);
                if (!b) {
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

        return baseResponse;
    }

    private void validInspection(HttpServletRequest request, Long userId) {
        User currentUser = userService.getById(userId);
        if (currentUser.getInvokeCount() < 1) {
            throw new BusinessException("调用次数已用完！请前往充值");
        }
        String url = request.getServletPath();
        log.info("用户请求的接口是:{},需要进行校验", url);

        // 获取签名
        String signature = request.getHeader("signature");
        log.info("前端生成的签名 = {}", signature);
        if (StringUtils.isEmpty(signature)) {
            throw new BusinessException("签名不能为空");
        }
        //  获取时间戳
        String timestamp = request.getHeader("stamp");
        log.info("前端生成的时间戳 = {}", timestamp);
        if (StringUtils.isEmpty(timestamp)) {
            throw new BusinessException("无时间信息");
        }

        //  获取secret
        String secret = AccessKeyEnum.SIGN.getDescription();

        //  获取请求路径
        String servletPath = request.getServletPath();


        //        合成加密前字符串
        String jointStr = "&secret=" + secret + "&stamp=" + timestamp + "&url=" + servletPath;
        log.info("jointStr = {}", jointStr);

        //(hutool工具类)加密
        Digester digester = new Digester(DigestAlgorithm.MD5);
        String encryptStr = digester.digestHex(jointStr).toUpperCase();
        log.info("后端生成的签名 = {}", encryptStr);

        if (!encryptStr.equals(signature)) {
            throw new BusinessException("签名错误");
        }
    }
}




