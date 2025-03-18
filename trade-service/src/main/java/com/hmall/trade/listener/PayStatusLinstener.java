package com.hmall.trade.listener;

import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayStatusLinstener {
    private final IOrderService orderService;
    @RabbitListener(bindings = @QueueBinding(value = @Queue(name = "trade.pay.success.queue",durable = "true"),
    exchange = @Exchange("pay.direct"),
    key = "pay.success"))
    public void listenPaySuccess(Long orderId){
        // 查询订单为已支付才更新
        Order byId = orderService.getById(orderId);
        if (byId == null || byId.getStatus() != 1) {
            return;
        }
        orderService.markOrderPaySuccess(orderId);
    }
}
