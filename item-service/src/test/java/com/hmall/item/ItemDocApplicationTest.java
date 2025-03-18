package com.hmall.item;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SpringBootTest(properties = "spring.profiles.active=local")
class ItemDocApplicationTest {
    @Autowired
    private  IItemService iItemService;
    private RestHighLevelClient restHighLevelClient;
    @BeforeEach
    void setUp(){
        restHighLevelClient = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.138.101:9200")));
    }
    @AfterEach
    void tearDown() throws IOException {
        if (restHighLevelClient != null){
            restHighLevelClient.close();
        }

    }

    @Test
    public void testCreateMapping() throws IOException {
        // 创建索引
        Item byId = iItemService.getById(317578L);
        ItemDoc itemDoc = BeanUtil.copyProperties(byId, ItemDoc.class);
        String str = JSONUtil.toJsonStr(itemDoc);
        IndexRequest indexRequest = new IndexRequest("hmall_item").id(itemDoc.getId());
        indexRequest.source(str, XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    }
    @Test
    public void testGetMapping() throws IOException {
        // 创建索引
        GetRequest getRequest = new GetRequest("hmall_item","317578");
        Map<String, Object> source = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT).getSource();
        System.out.println(source);

    }
    @Test
    public void testDeleteMapping() throws IOException {
        // 创建索引
        DeleteRequest deleteRequest = new DeleteRequest("hmall_item","317578");
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        // 创建索引
        GetRequest getRequest = new GetRequest("hmall_item","317578");
        boolean exists = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT).isExists();

        System.out.println(exists);

    }
    @Test
    public void testUpdateMapping() throws IOException {
        // 创建索引
        DeleteRequest deleteRequest = new DeleteRequest("hmall_item","317578");
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        // 创建索引
        Item byId = iItemService.getById(317578L);
        ItemDoc itemDoc = BeanUtil.copyProperties(byId, ItemDoc.class);
        itemDoc.setPrice(11111);
        String str = JSONUtil.toJsonStr(itemDoc);
        IndexRequest indexRequest = new IndexRequest("hmall_item").id(itemDoc.getId());
        indexRequest.source(str, XContentType.JSON);
        restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        GetRequest getRequest = new GetRequest("hmall_item","317578");
        Map<String, Object> source = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT).getSource();
        System.out.println(source);

    }
    @Test
    public void testBulkIndex() throws IOException {
        int pageNo = 1, pageSize = 500;
        while(true){
            Page<Item> page = iItemService.lambdaQuery().eq(Item::getStatus, 1).page(Page.of(pageNo, pageSize));
            List<Item> records = page.getRecords();
            if (CollectionUtil.isEmpty(records)){
                return;
            }
            BulkRequest request = new BulkRequest();
            for (Item record : records) {
                request.add(new IndexRequest("hmall_item")
                        .id(String.valueOf(record.getId()))
                        .source(JSONUtil.toJsonStr(record), XContentType.JSON));
            };
            pageNo++;
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        }
        // 创建索引

    }

}