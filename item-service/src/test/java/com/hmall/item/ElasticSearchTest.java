package com.hmall.item;

import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

//@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticSearchTest {
    @Autowired
    private IItemService iItemService;
    private RestHighLevelClient restHighLevelClient;
    @Test
    public void test(){
        SearchRequest searchRequest = new SearchRequest("hmall_item");
        searchRequest.source().query(QueryBuilders.matchAllQuery());
        try {
            SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            parseResult(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseResult(SearchResponse search) {
        SearchHits hits = search.getHits();
        long value = hits.getTotalHits().value;
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1) {
            System.out.println(hit.getSourceAsString());
            String name = hit.getHighlightFields().get("name").getFragments()[0].toString();
            System.out.println(name);
        }
        System.out.println(search);
    }

    @Test
    public void test2() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hmall_item");
        searchRequest.source().query(
                QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("name","脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand","德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lt(30000))
        );
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        parseResult(search);
    }
    @Test
    public void test3() throws IOException {
        int pageNo = 2, pageSize = 15;
        SearchRequest searchRequest = new SearchRequest("hmall_item");
        searchRequest.source().query(QueryBuilders.matchAllQuery());
        searchRequest.source().from((pageNo - 1) * pageSize).size(pageSize);
        searchRequest.source().sort("sold", SortOrder.DESC);
        searchRequest.source().sort("price", SortOrder.ASC);
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        parseResult(search);
    }
    @Test
    public void test4() throws IOException {
        int pageNo = 1, pageSize = 15;
        SearchRequest searchRequest = new SearchRequest("hmall_item");
        searchRequest.source().query(
                QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("name","脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand","德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lt(30000))
        );
        searchRequest.source().from((pageNo - 1) * pageSize).size(pageSize);
        searchRequest.source().sort("sold", SortOrder.DESC);
        searchRequest.source().sort("price", SortOrder.ASC);
        searchRequest.source().highlighter(SearchSourceBuilder.highlight().field("name"));
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = search.getHits();
        long value = hits.getTotalHits().value;
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1) {
            System.out.println(hit.getSourceAsString());
            String name = hit.getHighlightFields().get("name").getFragments()[0].toString();
            System.out.println(name);
        }
        System.out.println(search);

    }
    @Test
    public void test5() throws IOException {
        SearchRequest searchRequest = new SearchRequest("hmall_item");
        searchRequest.source().size(0);
        String aggname = "AggrName";
        searchRequest.source().aggregation(AggregationBuilders.terms(aggname).field("brand").size(20));
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        Aggregations aggregations = search.getAggregations();
        Terms aggregation = aggregations.get(aggname);
        aggregation.getBuckets().forEach(bucket -> {
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getDocCount());
        });
        System.out.println(search);
    }
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
}
