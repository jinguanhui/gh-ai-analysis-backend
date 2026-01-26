package com.jgh.aianalysis.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
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
import com.jgh.ghcommon.common.ChartStatusEnum;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

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

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

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

        int size1 = threadPoolExecutor.getQueue().size();
//        if (size1 == 1) {
//            log.error("当前系统繁忙，请稍后再试！");
//            throw new BusinessException("当前系统繁忙，请稍后再试！");
//        }

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

        Chart chartResult = new Chart();
        chartResult.setName(name);
        chartResult.setGoal(goal);
        chartResult.setChartType(chartType);
        chartResult.setUserId(userId);
        chartResult.setStatus(ChartStatusEnum.WAIT.getStatus());
        chartResult.setExecMessage(ChartStatusEnum.WAIT.getExecMessage());

        boolean save = save(chartResult);
        if (!save) {
            log.error("图表保存失败！");
            throw new BusinessException("图表保存失败！");
        }

        biResponse.setChartId(chartResult.getId());
        // 启动异步任务进行图表生成
        try {
            // 立即复制文件到安全位置
            byte[] fileBytes = multipartFile.getBytes();
            String originalFilename1 = multipartFile.getOriginalFilename();
            if (originalFilename1 == null) {
                log.error("没有文件名！");
                handleSseError(baseResponse, "没有文件名！", taskId);
                throw new BusinessException("没有文件名！");
            }
            CompletableFuture.runAsync(() -> {
                try {

                    // 创建一个HashMap存储线程池的状态信息
                    Map<String, Object> threadMap = new HashMap<>();
                    // 获取线程池的队列长度
                    int sizeQueue = threadPoolExecutor.getQueue().size();
                    // 将队列长度放入map中
                    threadMap.put("队列长度", sizeQueue);
                    // 获取线程池已接收的任务总数
                    long taskCount = threadPoolExecutor.getTaskCount();
                    // 将任务总数放入map中
                    threadMap.put("任务总数", taskCount);
                    // 获取线程池已完成的任务数
                    long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
                    // 将已完成的任务数放入map中
                    threadMap.put("已完成任务数", completedTaskCount);
                    // 获取线程池中正在执行任务的线程数
                    int activeCount = threadPoolExecutor.getActiveCount();
                    // 将正在工作的线程数放入map中
                    threadMap.put("正在工作的线程数", activeCount);
                    // 将map转换为JSON字符串并返回
                    log.info("线程池状态" + JSONUtil.toJsonStr(threadMap));
                    Chart updateChart = new Chart();
                    updateChart.setId(chartResult.getId());
                    updateChart.setStatus(ChartStatusEnum.RUNNING.getStatus());
                    updateChart.setExecMessage(ChartStatusEnum.RUNNING.getExecMessage());
                    boolean b1 = updateById(updateChart);
                    if (!b1) {
                        log.error("图表更新失败！");
                        handleSseError(baseResponse, "图表更新失败！", taskId);
                        throw new BusinessException("图表更新失败！");
                    }

                    Long userId2 = userId;
                    // 1. 验证参数（10%）
                    log.info("开始处理参数...");
                    biResponse.setTaskId(taskId);
                    handleSseSend(biResponse, "正在处理参数...", 10, baseResponse);
                    if (StringUtils.isBlank(goal)) {
                        handleSseError(baseResponse, "请输入分析目标", taskId);
                        throw new BusinessException("请输入分析目标");
                    }
                    if (StringUtils.isBlank(name)) {
                        handleSseError(baseResponse, "请输入分析表的名称", taskId);
                        throw new BusinessException("请输入分析表的名称");
                    }
                    if (StringUtils.isNotBlank(name) && name.length() > 100) {
                        handleSseError(baseResponse, "名称过长", taskId);
                        throw new BusinessException("名称过长");
                    }
                    //  将文档上传到OSS
                    String fileURL = null;
                    try {
                        // 使用预读取的文件字节数组创建输入流上传到OSS
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
                        fileURL = aliyunOSSUtil.getFileURL(inputStream, originalFilename1, "file");
                    } catch (IOException e) {
                        log.error("上传文件失败！", e);
                        handleSseError(baseResponse, "上传文件失败！！！", taskId);
                        e.printStackTrace();
                        throw new BusinessException("上传文件失败！！！");
                    }

                    if (fileURL == null) {
                        log.error("上传文件失败！");
                        handleSseError(baseResponse, "上传文件失败！！！", taskId);
                        throw new BusinessException("上传文件失败！！！");
                    }

                    //  对文档检测
                    Map map = null;
                    try {
                        map = fileGreenUtil.fileGreenCheck(fileURL, "file");
                    } catch (Exception e) {
                        log.error("文件存在不合规内容！！！");
                        handleSseError(baseResponse, "文件存在不合规内容！！！", taskId);
                        throw new BusinessException("文件存在不合规内容！！！");
                    }

                    if (ObjectUtils.isEmpty(map) || !"pass".equals(map.get("suggestion"))) {
                        log.error("文件检测失败！！！内容不合规");
                        handleSseError(baseResponse, "文件检测失败！！！内容不合规", taskId);
                        throw new BusinessException("文件检测失败！！！内容不合规");
                    }

                    // 2. 文件处理（30%）
                    log.info("开始处理Excel文件...");
                    handleSseSend(biResponse, "正在处理Excel文件...", 30, baseResponse);

                    String CsvData = ExcelUtils.excelToCsvFromBytes(fileBytes);

                    // 3. AI分析（60%）
                    log.info("开始进行AI分析...");
                    handleSseSend(biResponse, "正在进行AI分析...", 60, baseResponse);

                    String result = aIManager.doChat(goal, CsvData, chartType);
                    String[] splits = result.split("【【【【【");
                    if (splits.length < 3) {
                        handleSseError(baseResponse, "AI生成错误！", taskId);
                        throw new BusinessException("AI生成错误！");
                    }


                    // 4. 保存数据（80%）
                    log.info("正在保存数据...");
                    handleSseSend(biResponse, "正在保存数据...", 80, baseResponse);
                    String genChart = splits[1].trim();
                    String genResult = splits[2].trim();

                    Chart chart = new Chart();
                    chart.setId(chartResult.getId());
                    chart.setName(name);
                    chart.setGoal(goal);
                    chart.setChartData(CsvData);
                    chart.setChartType(chartType);
                    chart.setGenChart(genChart);
                    chart.setGenResult(genResult);
                    chart.setUserId(userId);


                    boolean saveResult = this.updateById(chart);
                    if (!saveResult) {
                        log.error("数据库插入错误！");
                        handleSseError(baseResponse, "数据库插入错误！", taskId);
                        throw new BusinessException("数据库插入错误！");
                    }
                    log.info("数据保存成功...");

                    User user = userService.getById(userId2);
                    if (user == null) {
                        handleSseError(baseResponse, "用户不存在！", taskId);
                        throw new BusinessException("用户不存在！");
                    }
                    if (user.getInvokeCount() < 1) {
                        handleSseError(baseResponse, "调用次数不足！", taskId);
                        throw new BusinessException("调用次数不足！");
                    }
                    user.setInvokeCount(user.getInvokeCount() - 1);
                    boolean b = userService.updateById(user);
                    if (!b) {
                        handleSseError(baseResponse, "数据库更新错误！", taskId);
                        throw new BusinessException("数据库更新错误！");
                    }

                    //  修改图表状态
                    Chart updateChartResult = new Chart();
                    updateChartResult.setId(chartResult.getId());
                    updateChartResult.setGenChart(genChart);
                    updateChartResult.setGenResult(genResult);
                    updateChartResult.setStatus(ChartStatusEnum.SUCCEED.getStatus());
                    updateChartResult.setExecMessage(ChartStatusEnum.SUCCEED.getExecMessage());
                    boolean updateResult = this.updateById(updateChartResult);
                    if (!updateResult) {
                        log.error("数据库更新错误！");
                        handleSseError(baseResponse, "数据库更新错误！", taskId);
                        throw new BusinessException("数据库更新错误！");
                    }
                    // 5. 完成任务（100%）
                    log.info("任务完成...");
                    biResponse.setChartId(chartResult.getId());
                    biResponse.setGenChart(genChart);
                    biResponse.setGenResult(genResult);
                    handleSseSend(biResponse, "任务完成...", 100, baseResponse);


                } catch (Exception e) {
                    try {
                        Chart chart = new Chart();
                        chart.setId(chartResult.getId());
                        chart.setStatus(ChartStatusEnum.FAILED.getStatus());
                        chart.setExecMessage(ChartStatusEnum.FAILED.getExecMessage() + ":" +e.getMessage());
                        boolean updateResult = this.updateById(chart);
                        if (!updateResult) {
                            log.error("数据库更新错误！");
                            handleSseError(baseResponse, "数据库更新错误！", taskId);
                            throw new BusinessException("数据库更新错误！");
                        }
                    } catch (BusinessException ex) {
                        log.error("数据库更新错误！");
                        ex.printStackTrace();
                        handleSseError(baseResponse, "数据库更新错误！", taskId);
                        throw new BusinessException("数据库更新错误");
                    }
                    // 发送错误信息
                    e.printStackTrace();
                    handleSseError(baseResponse, "任务执行错误！", taskId);
                    throw new BusinessException("任务执行错误！");
                } finally {
                    // 确保连接关闭
                    sseEmitterManager.removeEmitter(taskId);
                }
            }, threadPoolExecutor);
        } catch (Exception e) {
            log.error("异步任务执行错误！", e);
            throw new BusinessException("任务执行错误!!!");
        }

        biResponse.setTaskId(taskId);
        biResponse.setChartId(chartResult.getId());

        baseResponse.setData(biResponse);
        baseResponse.setCode(200);
        baseResponse.setMessage("任务提交成功！");

        return baseResponse;
    }

    @Override
    public BaseResponse<BiResponse> genChartByAiSync(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
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

        Chart chartResult = new Chart();
        chartResult.setName(name);
        chartResult.setGoal(goal);
        chartResult.setChartType(chartType);
        chartResult.setUserId(userId);
        chartResult.setStatus(ChartStatusEnum.WAIT.getStatus());
        chartResult.setExecMessage(ChartStatusEnum.WAIT.getExecMessage());

        boolean save = save(chartResult);
        if (!save) {
            log.error("图表保存失败！");
            throw new BusinessException("图表保存失败！");
        }


        // 启动异步任务进行图表生成
        try {
            // 立即复制文件到安全位置
            byte[] fileBytes = multipartFile.getBytes();
            InputStream inputStream = multipartFile.getInputStream();
            String originalFilename1 = multipartFile.getOriginalFilename();
            if (originalFilename1 == null) {
                log.error("没有文件名！");
                handleSseError(baseResponse, "没有文件名！", taskId);
                throw new BusinessException("没有文件名！");
            }
            // 创建一个HashMap存储线程池的状态信息
            Map<String, Object> threadMap = new HashMap<>();
            // 获取线程池的队列长度
            int sizeQueue = threadPoolExecutor.getQueue().size();
            // 将队列长度放入map中
            threadMap.put("队列长度", sizeQueue);
            // 获取线程池已接收的任务总数
            long taskCount = threadPoolExecutor.getTaskCount();
            // 将任务总数放入map中
            threadMap.put("任务总数", taskCount);
            // 获取线程池已完成的任务数
            long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
            // 将已完成的任务数放入map中
            threadMap.put("已完成任务数", completedTaskCount);
            // 获取线程池中正在执行任务的线程数
            int activeCount = threadPoolExecutor.getActiveCount();
            // 将正在工作的线程数放入map中
            threadMap.put("正在工作的线程数", activeCount);
            // 将map转换为JSON字符串并返回
            log.info("线程池状态" + JSONUtil.toJsonStr(threadMap));
            Chart updateChart = new Chart();
            updateChart.setId(chartResult.getId());
            updateChart.setStatus(ChartStatusEnum.RUNNING.getStatus());
            updateChart.setExecMessage(ChartStatusEnum.RUNNING.getExecMessage());
            boolean b1 = updateById(updateChart);
            if (!b1) {
                log.error("图表更新失败！");
                handleSseError(baseResponse, "图表更新失败！", taskId);
                throw new BusinessException("图表更新失败！");
            }

            Long userId2 = userId;
            // 1. 验证参数（10%）
            log.info("开始处理参数...");
            biResponse.setTaskId(taskId);
            handleSseSend(biResponse, "正在处理参数...", 10, baseResponse);
            if (StringUtils.isBlank(goal)) {
                handleSseError(baseResponse, "请输入分析目标", taskId);
                throw new BusinessException("请输入分析目标");
            }
            if (StringUtils.isBlank(name)) {
                handleSseError(baseResponse, "请输入分析表的名称", taskId);
                throw new BusinessException("请输入分析表的名称");
            }
            if (StringUtils.isNotBlank(name) && name.length() > 100) {
                handleSseError(baseResponse, "名称过长", taskId);
                throw new BusinessException("名称过长");
            }
            //  将文档上传到OSS
            String fileURL = null;
            try {
                fileURL = aliyunOSSUtil.getFileURL(inputStream, originalFilename1, "file");
            } catch (IOException e) {
                log.error("上传文件失败！", e);
                handleSseError(baseResponse, "上传文件失败！！！", taskId);
                e.printStackTrace();
                throw new BusinessException("上传文件失败！！！");
            }

            if (fileURL == null) {
                log.error("上传文件失败！");
                handleSseError(baseResponse, "上传文件失败！！！", taskId);
                throw new BusinessException("上传文件失败！！！");
            }

            //  对文档检测
            Map map = null;
            try {
                map = fileGreenUtil.fileGreenCheck(fileURL, "file");
            } catch (Exception e) {
                log.error("文件存在不合规内容！！！");
                handleSseError(baseResponse, "文件存在不合规内容！！！", taskId);
                throw new BusinessException("文件存在不合规内容！！！");
            }

            if (ObjectUtils.isEmpty(map) || !"pass".equals(map.get("suggestion"))) {
                log.error("文件检测失败！！！内容不合规");
                handleSseError(baseResponse, "文件检测失败！！！内容不合规", taskId);
                throw new BusinessException("文件检测失败！！！内容不合规");
            }

            // 2. 文件处理（30%）
            log.info("开始处理Excel文件...");
            handleSseSend(biResponse, "正在处理Excel文件...", 30, baseResponse);

            String CsvData = ExcelUtils.excelToCsvFromBytes(fileBytes);

            // 3. AI分析（60%）
            log.info("开始进行AI分析...");
            handleSseSend(biResponse, "正在进行AI分析...", 60, baseResponse);

            String result = aIManager.doChat(goal, CsvData, chartType);
            String[] splits = result.split("【【【【【");
            if (splits.length < 3) {
                handleSseError(baseResponse, "AI生成错误！", taskId);
                throw new BusinessException("AI生成错误！");
            }


            // 4. 保存数据（80%）
            log.info("正在保存数据...");
            handleSseSend(biResponse, "正在保存数据...", 80, baseResponse);
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();

            Chart chart = new Chart();
            chart.setId(chartResult.getId());
            chart.setName(name);
            chart.setGoal(goal);
            chart.setChartData(CsvData);
            chart.setChartType(chartType);
            chart.setGenChart(genChart);
            chart.setGenResult(genResult);
            chart.setUserId(userId);


            boolean saveResult = this.updateById(chart);
            if (!saveResult) {
                log.error("数据库插入错误！");
                handleSseError(baseResponse, "数据库插入错误！", taskId);
                throw new BusinessException("数据库插入错误！");
            }
            log.info("数据保存成功...");

            User user = userService.getById(userId2);
            if (user == null) {
                handleSseError(baseResponse, "用户不存在！", taskId);
                throw new BusinessException("用户不存在！");
            }
            user.setInvokeCount(user.getInvokeCount() - 1);
            boolean b = userService.updateById(user);
            if (!b) {
                handleSseError(baseResponse, "数据库更新错误！", taskId);
                throw new BusinessException("数据库更新错误！");
            }

            // 5. 完成任务（100%）
            log.info("任务完成...");
            biResponse.setChartId(chartResult.getId());
            biResponse.setGenChart(genChart);
            biResponse.setGenResult(genResult);
            handleSseSend(biResponse, "任务完成...", 100, baseResponse);

            //  修改图表状态
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartResult.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(ChartStatusEnum.SUCCEED.getStatus());
            updateChartResult.setExecMessage(ChartStatusEnum.SUCCEED.getExecMessage());
            boolean updateResult = this.updateById(updateChartResult);
            if (!updateResult) {
                log.error("数据库更新错误！");
                handleSseError(baseResponse, "数据库更新错误！", taskId);
                throw new BusinessException("数据库更新错误！");
            }

        } catch (Exception e) {
            try {
                Chart chart = new Chart();
                chart.setId(chartResult.getId());
                chart.setStatus(ChartStatusEnum.FAILED.getStatus());
                chart.setExecMessage(e.getMessage());
                boolean updateResult = this.updateById(chart);
                if (!updateResult) {
                    log.error("数据库更新错误！");
                    handleSseError(baseResponse, "数据库更新错误！", taskId);
                    throw new BusinessException("数据库更新错误！");
                }
            } catch (BusinessException ex) {
                log.error("数据库更新错误！");
                ex.printStackTrace();
                handleSseError(baseResponse, "数据库更新错误！", taskId);
                throw new BusinessException("数据库更新错误");
            }
            // 发送错误信息
            e.printStackTrace();
            handleSseError(baseResponse, "任务执行错误！", taskId);
            throw new BusinessException("任务执行错误！");
        } finally {
            // 确保连接关闭
            sseEmitterManager.removeEmitter(taskId);
        }

        biResponse.setTaskId(taskId);
        biResponse.setChartId(chartResult.getId());

        baseResponse.setData(biResponse);
        baseResponse.setCode(200);
        baseResponse.setMessage("任务提交成功！");

        return baseResponse;
    }

    private void handleSseSend(BiResponse biResponse, String taskInfo, int taskProcess, BaseResponse<BiResponse> baseResponse) {
        biResponse.setTaskInfo(taskInfo);
        biResponse.setTaskProcess(taskProcess);
        baseResponse.setData(biResponse);
        baseResponse.setCode(200);
        sseEmitterManager.sendProgress(baseResponse);
    }

    private void handleSseError(BaseResponse<BiResponse> baseResponse, String goal, String taskId) {
        log.error("sse执行错误！！！");
        baseResponse.setMessage(goal);
        baseResponse.setCode(500);
        sseEmitterManager.sendProgress(baseResponse);
    }
}




