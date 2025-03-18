package com.hmall.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.hmall.common.utils.RabbitMQUtils;
import com.hmall.common.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;

import java.lang.reflect.Type;

@Configuration
@ConditionalOnClass(RabbitTemplate.class)
@Slf4j
public class MqMsgConvertConfig {
    @Bean
    public MessageConverter messageConverter(){
        return new AuthMessageConverter(new Jackson2JsonMessageConverter());
    }
    static class AuthMessageConverter implements MessageConverter {
        private final MessageConverter messageConverter;

        public AuthMessageConverter(MessageConverter messageConverter) {
            this.messageConverter = messageConverter;
        }

        @Override
        public Message toMessage(Object o, MessageProperties messageProperties) throws MessageConversionException {
            Long user = UserContext.getUser();
            if (user != null){
                messageProperties.setHeader("userInfo",user);
            }
            log.info("message :{},userInfo:{}",messageProperties.getHeader("userInfo"), user);
            return messageConverter.toMessage(o,messageProperties);
        }
        @Override
        public Object fromMessage(Message message) throws MessageConversionException {
            try {
                Long userInfo = message.getMessageProperties().getHeader("userInfo");
                log.info("message :{},userInfo:{}",message.getMessageProperties().getHeader("userInfo"), UserContext.getUser());
                if (ObjectUtil.isNotNull(userInfo)) {
                    UserContext.setUser(userInfo);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return messageConverter.fromMessage(message);
        }
    }
    @Bean
    public RabbitMQUtils rabbitMQUtilsHelper(RabbitTemplate rabbitTemplate){
        return new RabbitMQUtils(rabbitTemplate);
    }
}
