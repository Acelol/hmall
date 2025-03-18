package com.hmall.trade.constatnts;

import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

public class MQConstants {
    public static final String DELAY_EXCHANGE = "trade.delay.direct";
    public static final String DELAY_ORDER_QUEUE = "trade.delay.order.direct";
    public static final String DELAY_ORDER_QUEUE_KEY = "trade.delay.order.query";
    public static final Integer DELAY_ORDER_TIME_OUT = 60*30*1000;
    @NotNull
    public static MessagePostProcessor getMessagePostProcessor() {
        return new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setDelay(DELAY_ORDER_TIME_OUT);
                return message;
            }
        };
    }
}
