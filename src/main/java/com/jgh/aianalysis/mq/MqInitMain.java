package com.jgh.aianalysis.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;

import static com.jgh.ghcommon.constant.CommonConstant.*;

/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 */
public class MqInitMain {

    public static void main(String[] args) {
        try {
            // 创建连接工厂
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("119.91.68.39");
            factory.setPort(5672); // RabbitMQ默认端口（显式指定，避免默认值问题）
            factory.setUsername("admin"); // 替换为你创建的RabbitMQ用户名（不要用guest）
            factory.setPassword("jxlSKVsQsbPab6N3"); // 替换为你的密码
            // 创建连接
            Connection connection = factory.newConnection();
            // 创建通道
            Channel channel = connection.createChannel();
            // 定义交换机的名称为"code_exchange"
            // 声明交换机，指定交换机类型为 direct
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            channel.exchangeDeclare(DEAD_LETTER_EXCHANGE_NAME, "direct");

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("x-max-length", 1);
            map.put("x-message-ttl", 30000);
            map.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE_NAME);
            map.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);

            Map<String, Object> map1 = new HashMap<String, Object>();
            map1.put("x-max-length", 10);
            map1.put("x-message-ttl", 30000);

            // 创建队列，随机分配一个队列名称
            // 声明队列，设置队列持久化、非独占、非自动删除，并传入额外的参数为 null
            channel.queueDeclare(QUEUE_NAME, true, false, false, map);
            channel.queueDeclare(DEAD_LETTER_QUEUE_NAME, true, false, false, map1);
            // 将队列绑定到交换机，指定路由键为 "my_routingKey"
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
            channel.queueBind(DEAD_LETTER_QUEUE_NAME, DEAD_LETTER_EXCHANGE_NAME, DEAD_LETTER_ROUTING_KEY);
        } catch (Exception e) {
            e.printStackTrace();
            // 异常处理
        }
    }
}
