package com.hmall.item.mq;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.item.constant.MQConstant;
import com.hmall.item.domain.dto.OrderDetailDTO;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ItemListener {
    private final RestHighLevelClient restHighLevelClientl;
    private final IItemService iItemService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstant.ITEM_STOCK_QUEUE_KEY,durable ="true"),
            exchange = @Exchange(name = MQConstant.ITEM_STOCK_EXCHANGE,type = "topic"),
            key = {MQConstant.ITEM_STOCK_DECREASE_KEY,MQConstant.ITEM_STOCK_INCREASE_KEY})
    )
    public void onItemStockChange(List<OrderDetailDTO>  list) throws IOException {
        for (OrderDetailDTO orderDetailDTO : list) {
            rebuildEsIndex(orderDetailDTO);
        }
    }

    private void rebuildEsIndex(OrderDetailDTO orderDetailDTO) throws IOException {
        GetRequest getRequest = new GetRequest(MQConstant.INDEX, orderDetailDTO.getItemId() + "");
        if (restHighLevelClientl.exists(getRequest, RequestOptions.DEFAULT)) {
            // 先删除索引信息
            DeleteRequest deleteRequest = new DeleteRequest(MQConstant.INDEX, orderDetailDTO.getItemId() + "");
            restHighLevelClientl.delete(deleteRequest, RequestOptions.DEFAULT);
        }

        // 后重新查询商品信息再插入
        Item one = iItemService.lambdaQuery().eq(Item::getId, orderDetailDTO.getItemId()).one();
        if (one == null){
            log.info("商品不存在,{}", orderDetailDTO.getItemId());
            return;
        }
        IndexRequest request = new IndexRequest(MQConstant.INDEX).id(one.getId() + "" )
                .source(JSONUtil.toJsonStr(one), XContentType.JSON);
        int status = restHighLevelClientl.index(request, RequestOptions.DEFAULT).status().getStatus();
        log.info("商品信息更新成功" + status);
    }
}
