package com.hmall.item;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
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

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemApplicationTest {

    private RestHighLevelClient restHighLevelClient;;
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
    public void contextLoads() {
        System.out.println(restHighLevelClient);
    }
    @Test
    public void testCreateIndex() throws IOException {
        // 创建索引
        CreateIndexRequest request = new CreateIndexRequest("hmall_item");
        request.source(TEMPLATE, XContentType.JSON);
        restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
    }
    @Test
    public void tesGetIndex() throws IOException {
        // 创建索引
        GetIndexRequest GetIndexRequest = new GetIndexRequest("hmall_item");
        System.out.println("索引是否存在：" + restHighLevelClient.indices().exists(GetIndexRequest, RequestOptions.DEFAULT));
    }
    @Test
    public void tesDeleteIndex() throws IOException {
        // 创建索引
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("hmall_item");
        restHighLevelClient.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);

    }
    @Test
    public void testGetMapping() throws IOException {
        // 创建索引
        IndexRequest indexRequest = new IndexRequest("hmall_item").id("1");

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