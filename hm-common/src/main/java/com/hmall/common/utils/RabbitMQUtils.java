package com.hmall.common.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class RabbitMQUtils {
    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(String exchange, String routingKey, Object msg){
        log.debug("ready To Send RabbitMq Message exchange:{}, routingKey:{} msg:{}", exchange, routingKey, msg);
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
    }

    public void sendDelayMessage(String exchange, String routingKey, Object msg, int delay){
        log.debug("ready To Send RabbitMq delayMessage exchange:{}, routingKey:{} msg:{} delay:{}", exchange, routingKey, msg, delay);
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setDelay(delay);
                return message;
            }
        });
    }

    public void sendMessageWithConfirm(String exchange, String routingKey, Object msg, int maxRetries){
        log.debug("ready To Send RabbitMq Message exchange:{}, routingKey:{} msg:{}", exchange, routingKey, msg);
        CorrelationData cd = new CorrelationData(UUID.randomUUID().toString());
        cd.getFuture().addCallback(new ListenableFutureCallback<CorrelationData.Confirm>() {
            int retyries = 0;
            @Override
            public void onFailure(Throwable ex) {
                log.error("send message failed, exchange:{}, routingKey:{}, msg:{}, cause:{}", exchange, routingKey, msg, ex.getMessage());
            }

            @Override
            public void onSuccess(CorrelationData.Confirm result) {
                if (result != null && !result.isAck()){
                    log.debug("send message failed, exchange:{}, routingKey:{}, msg:{}, cause:{}", exchange, routingKey, msg, result.toString());
                    if (retyries < maxRetries){
                        log.debug("retry send message, exchange:{}, routingKey:{}, msg:{}", exchange, routingKey, msg);
                        rabbitTemplate.convertAndSend(exchange, routingKey, msg, cd);
                        retyries++;
                    }else{
                        log.error("send message failed, exchange:{}, routingKey:{}, msg:{}, cause:{}", exchange, routingKey, msg, result.toString());
                        return;
                    }
                }
            }
        });
    }
}
