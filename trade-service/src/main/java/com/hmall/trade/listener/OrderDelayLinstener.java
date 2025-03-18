package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.trade.constatnts.MQConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderDelayLinstener {
    private final IOrderService orderService;
    private final PayClient payClient;

    @RabbitListener(bindings =
    @QueueBinding(
            value = @Queue(name = MQConstants.DELAY_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(name = MQConstants.DELAY_EXCHANGE, delayed = "true"),
            key = MQConstants.DELAY_ORDER_QUEUE_KEY))
    public void listenOrderDelay(Long orderId) {
        //check order status
        Order byId = orderService.getById(orderId);
        if (byId == null || byId.getStatus() != 1){
            return;
        }
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);
        if (payOrderDTO != null && payOrderDTO.getStatus() == 3) {
            orderService.markOrderPaySuccess(orderId);
        }else{
            orderService.cancelOrder(orderId);
        }
        //未支付 取消订单 恢复库存
        // 已支付 更新支付状态
    }
}
