package com.hmall.item.init;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.constant.MQConstant;
import com.hmall.item.domain.po.Item;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticSearchInitListener implements ApplicationListener<ApplicationReadyEvent> {
    private final RestHighLevelClient restHighLevelClient;
    private final IItemService iItemService;

    private static final ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(3, 4, 0, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 创建索引
        GetIndexRequest GetIndexRequest = new GetIndexRequest(MQConstant.INDEX);
        boolean getIndex = true;
        try {
            getIndex = restHighLevelClient.indices().exists(GetIndexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error("初始化查询es失败", e);
        }
        if (!getIndex) {
            // 创建索引
            CreateIndexRequest request = new CreateIndexRequest(MQConstant.INDEX);
            request.source(TEMPLATE, XContentType.JSON);
            try {
            restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
            int pageNo = 1, pageSize = 500;
                while(true){
                    final int excutPageNo = pageNo;
                    Future<Boolean> submit1 = threadPoolExecutor.submit(() -> {
                        Page<Item> page = iItemService.lambdaQuery().eq(Item::getStatus, 1).page(Page.of(excutPageNo, pageSize));
                        List<Item> records = page.getRecords();
                        if (CollectionUtil.isEmpty(records)) {
                            return false;
                        }
                        BulkRequest requestbulk = new BulkRequest();
                        for (Item record : records) {
                            requestbulk.add(new IndexRequest(MQConstant.INDEX)
                                    .id(String.valueOf(record.getId()))
                                    .source(JSONUtil.toJsonStr(record), XContentType.JSON));
                        }
                        try {
                            restHighLevelClient.bulk(requestbulk, RequestOptions.DEFAULT);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    });
                    if (!submit1.get()){
                        return;
                    }
                    pageNo++;

            }
            } catch (IOException e) {
                log.error("初始化es失败", e);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private static final String TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\":{\n" +
            "        \"type\":\"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_smart\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\":\"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"index\": false,\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\":\"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"index\": false,\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"isAd\":{\n" +
            "        \"type\":\"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\":\"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
