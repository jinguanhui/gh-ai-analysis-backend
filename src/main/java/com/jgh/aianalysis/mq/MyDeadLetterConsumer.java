package com.jgh.aianalysis.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jgh.aianalysis.constant.PayStatusEnum;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.manager.SseEmitterManager;
import com.jgh.aianalysis.manager.ai.AIManager;
import com.jgh.aianalysis.modal.entity.Order;
import com.jgh.aianalysis.service.ChartService;
import com.jgh.aianalysis.service.GhFileService;
import com.jgh.aianalysis.service.OrderService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.ExcelUtils;
import com.jgh.aianalysis.utils.aliyun.AliyunOSSUtil;
import com.jgh.aianalysis.utils.aliyun.FileGreenUtil;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.common.ChartStatusEnum;
import com.jgh.ghcommon.model.entity.Chart;
import com.jgh.ghcommon.model.entity.GhFile;
import com.jgh.ghcommon.model.entity.User;
import com.jgh.ghcommon.model.vo.BiResponse;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;


// 使用@Component注解标记该类为一个组件，让Spring框架能够扫描并将其纳入管理
@Component
// 使用@Slf4j注解生成日志记录器
@Slf4j
public class MyDeadLetterConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private GhFileService ghFileService;

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
    @RabbitListener(queues = {"guangwu_dead_queue"}, ackMode = "MANUAL")
    // @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag是一个方法参数注解,用于从消息头中获取投递标签(deliveryTag),
    // 在RabbitMQ中,每条消息都会被分配一个唯一的投递标签，用于标识该消息在通道中的投递状态和顺序。通过使用@Header(AmqpHeaders.DELIVERY_TAG)注解,可以从消息头中提取出该投递标签,并将其赋值给long deliveryTag参数。
    public void receiveDeadMessage(Map message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("AI分析死信队列开始工作，死信消息 = {}", message.toString());
        Long chartResultId = Long.valueOf(message.get("chartResultId").toString());

        UpdateWrapper<Chart> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", chartResultId);
        wrapper.set("status", ChartStatusEnum.FAILED.getStatus());
        wrapper.set("execMessage", ChartStatusEnum.FAILED.getExecMessage() + "当前系统繁忙，请稍后再试");

        boolean update = chartService.update(wrapper);
        if (!update) {
            log.error("死信队列执行时，遇到图表不存在问题");
            handleMessageReject(channel, deliveryTag);
            throw new BusinessException("图表不存在");
        }

        byte[] fileBytes = (byte[]) message.get("fileBytes");
        String originalFilename = message.get("originalFilename").toString();

        GhFile ghFile = new GhFile();
        ghFile.setFileName(originalFilename);
        ghFile.setFileExcel(fileBytes);
        boolean save = ghFileService.save(ghFile);
        if (!save) {
            log.error("死信队列执行时，保存重试文件失败");
            handleMessageReject(channel, deliveryTag);
            throw new BusinessException("保存重试文件失败");
        }

        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("MQ任务发送失败！！！", e);
            handleMessageReject(channel, deliveryTag);
            throw new BusinessException("MQ任务发送失败！！！");
        }
    }

    /**
     * 订单延时队列
     *
     * @param message
     * @param channel
     * @param deliveryTag
     */
    @RabbitListener(queues = {"order_dead_queue"}, ackMode = "MANUAL")
    public void receiveOrderMessage(Map message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("接收到订单消息...:{}" , JSONUtil.toJsonStr(message));

        Object origianMessage = message.get("orderId");
        Object userOrigian = message.get("userId");

        if (ObjectUtils.isEmpty(userOrigian)) {
            log.error("用户不存在");
            handleMessageReject(channel, deliveryTag);
            throw new BusinessException("用户不存在");
        }

        if (ObjectUtils.isEmpty(origianMessage)) {
            log.error("订单不存在");
            handleMessageReject(channel, deliveryTag);
            throw new BusinessException("订单不存在");
        }

        Long orderId = Long.parseLong(origianMessage.toString());
        Long userId = Long.parseLong(userOrigian.toString());

        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("id", orderId);
        wrapper.eq("userId", userId);
        Order order = orderService.getOne(wrapper);

        if (order == null) {
            log.error("订单不存在");
            handleMessageReject(channel, deliveryTag);
            throw new BusinessException("订单不存在");
        }

        //  如果订单的状态为待支付状态，取消订单
        if (PayStatusEnum.AWAIT_PAY.getStatus().equals(order.getStatus())) {
            order.setStatus(PayStatusEnum.CANCEL.getStatus());
            order.setDescription("用户超时未支付，已取消");
            boolean update = orderService.updateById(order);
            if (!update) {
                log.error("数据库更新错误！");
                handleMessageReject(channel, deliveryTag);
                throw new BusinessException("数据库更新错误！");
            }
        }


        try {
            channel.basicAck(deliveryTag, false);
            log.info("任务完成...已成功取消订单");
        } catch (IOException e) {
            log.error("MQ任务发送失败！！！", e);
            handleMessageReject(channel, deliveryTag);
            throw new BusinessException("MQ任务发送失败！！！");
        }
    }

    private static void handleMessageReject(Channel channel, long deliveryTag) {
        log.error("拒接AI分析消息");
        try {
            channel.basicReject(deliveryTag, false);
        } catch (IOException e) {
            log.error("消息手动拒接失败！");
            throw new BusinessException("消息手动拒接失败！");
        }
    }
}
