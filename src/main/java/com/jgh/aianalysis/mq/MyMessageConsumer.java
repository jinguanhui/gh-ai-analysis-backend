package com.jgh.aianalysis.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jgh.aianalysis.constant.PayStatusEnum;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.manager.SseEmitterManager;
import com.jgh.aianalysis.manager.ai.AIManager;
import com.jgh.aianalysis.modal.entity.Order;
import com.jgh.aianalysis.service.AccessKeyService;
import com.jgh.aianalysis.service.ChartService;
import com.jgh.aianalysis.service.OrderService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.ExcelUtils;
import com.jgh.aianalysis.utils.aliyun.AliyunOSSUtil;
import com.jgh.aianalysis.utils.aliyun.FileGreenUtil;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.common.ChartStatusEnum;
import com.jgh.ghcommon.model.entity.AccessKey;
import com.jgh.ghcommon.model.entity.Chart;
import com.jgh.ghcommon.model.entity.User;
import com.jgh.ghcommon.model.vo.BiResponse;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


// 使用@Component注解标记该类为一个组件，让Spring框架能够扫描并将其纳入管理
@Component
// 使用@Slf4j注解生成日志记录器
@Slf4j
public class MyMessageConsumer {

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
    private ChartService chartService;

    @Resource
    private AccessKeyService accessKeyService;

    @Resource
    private OrderService orderService;

    /**
     * 接收消息的方法
     *
     * @param message     接收到的消息内容，是一个字符串类型
     * @param channel     消息所在的通道，可以通过该通道与 RabbitMQ 进行交互，例如手动确认消息、拒绝消息等
     * @param deliveryTag 消息的投递标签，用于唯一标识一条消息
     */
    // 使用@SneakyThrows注解简化异常处理
    // 使用@RabbitListener注解指定要监听的队列名称为"code_queue"，并设置消息的确认机制为手动确认
    @RabbitListener(queues = {"guangwu_queue"}, ackMode = "MANUAL")
    // @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag是一个方法参数注解,用于从消息头中获取投递标签(deliveryTag),
    // 在RabbitMQ中,每条消息都会被分配一个唯一的投递标签，用于标识该消息在通道中的投递状态和顺序。通过使用@Header(AmqpHeaders.DELIVERY_TAG)注解,可以从消息头中提取出该投递标签,并将其赋值给long deliveryTag参数。
    public void receiveMessage(Map message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        // 使用日志记录器打印接收到的消息内容
        log.info("receiveMessage message = {}", message.toString());
        Long chartResultId = Long.valueOf(message.get("chartResultId").toString());
        String taskId = message.get("taskId").toString();
        byte[] fileBytes = (byte[]) message.get("fileBytes");
        String originalFilename = message.get("originalFilename").toString();
        BiResponse biResponse = new BiResponse();
        BaseResponse<BiResponse> baseResponse = new BaseResponse<>();

        Chart chartServiceById = chartService.getById(chartResultId);

        if (ChartStatusEnum.SUCCEED.getStatus().equals(chartServiceById.getStatus())) {
            log.error("图表已生成成功！不能重复生成");
            handleMessageReject(channel, deliveryTag);
            handleSseError(baseResponse, "图表已生成成功！不能重复生成", taskId);
            throw new BusinessException("图表已生成成功！不能重复生成");
        }
        String name = chartServiceById.getName();
        String goal = chartServiceById.getGoal();
        String chartType = chartServiceById.getChartType();
        Long userId = chartServiceById.getUserId();

        try {

            Chart updateChart = new Chart();
            updateChart.setId(chartResultId);
            updateChart.setStatus(ChartStatusEnum.RUNNING.getStatus());
            updateChart.setExecMessage(ChartStatusEnum.RUNNING.getExecMessage());
            boolean b1 = chartService.updateById(updateChart);
            if (!b1) {
                log.error("图表更新失败！");
                handleSseError(baseResponse, "图表更新失败！", taskId);
                handleMessageReject(channel, deliveryTag);

                throw new BusinessException("图表更新失败！");
            }

            Long userId2 = userId;
            // 1. 验证参数（10%）
            log.info("开始处理参数...");
            biResponse.setTaskId(taskId);
            handleSseSend(biResponse, "正在处理参数...", 10, baseResponse);
            if (StringUtils.isBlank(goal)) {
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "请输入分析目标", taskId);
                throw new BusinessException("请输入分析目标");
            }
            if (StringUtils.isBlank(name)) {
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "请输入分析表的名称", taskId);
                throw new BusinessException("请输入分析表的名称");
            }
            if (StringUtils.isNotBlank(name) && name.length() > 100) {
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "名称过长", taskId);
                throw new BusinessException("名称过长");
            }
            //  将文档上传到OSS
            String fileURL = null;
            try {
                // 使用预读取的文件字节数组创建输入流上传到OSS
                ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
                fileURL = aliyunOSSUtil.getFileURL(inputStream, originalFilename, "file");
            } catch (IOException e) {
                log.error("上传文件失败！", e);
                handleSseError(baseResponse, "上传文件失败！！！", taskId);
                handleMessageReject(channel, deliveryTag);
                e.printStackTrace();
                throw new BusinessException("上传文件失败！！！");
            }

            if (fileURL == null) {
                log.error("上传文件失败！");
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "上传文件失败！！！", taskId);
                throw new BusinessException("上传文件失败！！！");
            }

            //  对文档检测
            Map map = null;
            try {
                map = fileGreenUtil.fileGreenCheck(fileURL, "file");
            } catch (Exception e) {
                log.error("文件存在不合规内容！！！");
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "文件存在不合规内容！！！", taskId);
                throw new BusinessException("文件存在不合规内容！！！");
            }

            if (ObjectUtils.isEmpty(map) || !"pass".equals(map.get("suggestion"))) {
                log.error("文件检测失败！！！内容不合规");
                handleMessageReject(channel, deliveryTag);
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
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "AI生成错误！", taskId);
                throw new BusinessException("AI生成错误！");
            }


            // 4. 保存数据（80%）
            log.info("正在保存数据...");
            handleSseSend(biResponse, "正在保存数据...", 80, baseResponse);
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();

            Chart chart = new Chart();
            chart.setId(chartResultId);
            chart.setName(name);
            chart.setGoal(goal);
            chart.setChartData(CsvData);
            chart.setChartType(chartType);
            chart.setGenChart(genChart);
            chart.setGenResult(genResult);
            chart.setUserId(userId);


            boolean saveResult = chartService.updateById(chart);
            if (!saveResult) {
                log.error("数据库插入错误！");
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "数据库插入错误！", taskId);
                throw new BusinessException("数据库插入错误！");
            }
            log.info("数据保存成功...");

            User user = userService.getById(userId2);
            if (user == null) {
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "用户不存在！", taskId);
                throw new BusinessException("用户不存在！");
            }
            if (user.getInvokeCount() < 1) {
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "调用次数不足！", taskId);
                throw new BusinessException("调用次数不足！");
            }
            user.setInvokeCount(user.getInvokeCount() - 1);
            boolean b = userService.updateById(user);
            if (!b) {
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "数据库更新错误！", taskId);
                throw new BusinessException("数据库更新错误！");
            }

            //  修改图表状态
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartResultId);
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(ChartStatusEnum.SUCCEED.getStatus());
            updateChartResult.setExecMessage(ChartStatusEnum.SUCCEED.getExecMessage());
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                log.error("数据库更新错误！");
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "数据库更新错误！", taskId);
                throw new BusinessException("数据库更新错误！");
            }

            UpdateWrapper<AccessKey> wrapper = new UpdateWrapper<>();

            wrapper.eq("userId", userId);
            wrapper.set("lastUsedTime", new Date());

            boolean update = accessKeyService.update(wrapper);

            if (!update) {
                log.error("AccessKey数据库更新错误！");
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "AccessKey数据库更新错误！", taskId);
                throw new BusinessException("AccessKey数据库更新错误！");
            }

            // 5. 完成任务（100%）
            log.info("任务完成...");
            biResponse.setChartId(chartResultId);
            biResponse.setGenChart(genChart);
            biResponse.setGenResult(genResult);
            handleSseSend(biResponse, "任务完成...", 100, baseResponse);


        } catch (Exception e) {
            try {
                Chart chart = new Chart();
                handleDatabase(e, chart, chartServiceById, baseResponse, taskId);
            } catch (BusinessException ex) {
                log.error("数据库更新错误！");
                ex.printStackTrace();
                handleMessageReject(channel, deliveryTag);
                handleSseError(baseResponse, "数据库更新错误！", taskId);
                throw new BusinessException("数据库更新错误");
            }
            // 发送错误信息
            e.printStackTrace();
            handleMessageReject(channel, deliveryTag);
            handleSseError(baseResponse, "任务执行错误！", taskId);
            throw new BusinessException("任务执行错误！");
        } finally {
            // 确保连接关闭
            sseEmitterManager.removeEmitter(taskId);
        }
        // 投递标签是一个数字标识,它在消息消费者接收到消息后用于向RabbitMQ确认消息的处理状态。通过将投递标签传递给channel.basicAck(deliveryTag, false)方法,可以告知RabbitMQ该消息已经成功处理,可以进行确认和从队列中删除。
        // 手动确认消息的接收，向RabbitMQ发送确认消息
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            handleMessageReject(channel, deliveryTag);
            log.error("MQ任务发送失败！！！", e);
            throw new BusinessException("MQ任务发送失败！！！");
        }
    }


    private void handleMessageReject(Channel channel, long deliveryTag) {
        log.error("拒接AI分析消息");
        try {
            channel.basicReject(deliveryTag, false);
        } catch (IOException e) {
            log.error("消息手动拒接失败！");
            throw new BusinessException("消息手动拒接失败！");
        }
    }

    private void handleDatabase(Exception e, Chart chart, Chart chartResult, BaseResponse<BiResponse> baseResponse, String taskId) {
        chart.setId(chartResult.getId());
        chart.setStatus(ChartStatusEnum.FAILED.getStatus());
        if (e == null) {
            chart.setStatus(ChartStatusEnum.FAILED.getStatus());
        } else {
            chart.setExecMessage(ChartStatusEnum.FAILED.getExecMessage() + ":" + e.getMessage());
        }
        boolean updateResult = chartService.updateById(chart);
        if (!updateResult) {
            log.error("数据库更新错误！");
            handleSseError(baseResponse, "数据库更新错误！", taskId);
            throw new BusinessException("数据库更新错误！");
        }
    }


    private void handleSseSend(BiResponse biResponse, String taskInfo, int taskProcess, BaseResponse<BiResponse> baseResponse) {
        biResponse.setTaskInfo(taskInfo);
        biResponse.setTaskProcess(taskProcess);
        baseResponse.setData(biResponse);
        baseResponse.setCode(200);
        sseEmitterManager.sendProgress(baseResponse);
    }

    private void handleSseError(BaseResponse<BiResponse> baseResponse, String goal, String taskId) {
        log.error("sse执行错误！！！:" + goal);
        baseResponse.setMessage(goal);
        baseResponse.setCode(500);
        sseEmitterManager.sendProgress(baseResponse);
    }
}
