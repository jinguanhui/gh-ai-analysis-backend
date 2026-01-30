package com.jgh.aianalysis.mq;

import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.manager.SseEmitterManager;
import com.jgh.aianalysis.manager.ai.AIManager;
import com.jgh.aianalysis.service.ChartService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.ExcelUtils;
import com.jgh.aianalysis.utils.aliyun.AliyunOSSUtil;
import com.jgh.aianalysis.utils.aliyun.FileGreenUtil;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.common.ChartStatusEnum;
import com.jgh.ghcommon.model.entity.Chart;
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

    }
}
