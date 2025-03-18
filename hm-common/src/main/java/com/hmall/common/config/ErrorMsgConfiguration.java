package com.hmall.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(RabbitTemplate.class)
public class ErrorMsgConfiguration {

    private final RabbitTemplate rabbitTemplate;
    @Value("${spring.application.name}")
    private String serviceName;
    @Bean
    public DirectExchange errExchange(){
        return new DirectExchange("hmall.error.direct");
    }
    @Bean
    public Queue errorQueue(){
        return new Queue(serviceName + ".error.queue");
    }
    @Bean
    public Binding errorBinding(DirectExchange errExchange, Queue errorQueue){
       return  BindingBuilder.bind(errorQueue).to(errExchange).with(serviceName);
    }
    @Bean
    public MessageRecoverer mssageeRecoverer(){
        return new RepublishMessageRecoverer(rabbitTemplate, "error.direct", serviceName);
    }
}
