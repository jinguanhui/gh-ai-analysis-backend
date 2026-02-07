package com.jgh.aianalysis.mq;

/**
 * 通用常量
 *
 * @author 光吾
 */
public interface MQConstant {

    String EXCHANGE_NAME = "guangwu_exchange";
    String QUEUE_NAME = "guangwu_queue";

    String ROUTING_KEY = "my_routingKey";

    String DEAD_LETTER_EXCHANGE_NAME = "guangwu_dead_exchange";
    String DEAD_LETTER_QUEUE_NAME = "guangwu_dead_queue";
    String DEAD_LETTER_ROUTING_KEY = "my_dead_routingKey";

    String ORDER_EXCHANGE_NAME = "order_exchange";
    String ORDER_QUEUE_NAME = "order_queue";
    String ORDER_ROUTING_KEY = "order_routingKey";
    String ORDER_DEAD_LETTER_EXCHANGE_NAME = "order_dead_exchange";
    String ORDER_DEAD_LETTER_QUEUE_NAME = "order_dead_queue";
    String ORDER_DEAD_LETTER_ROUTING_KEY = "order_dead_routingKey";


}
