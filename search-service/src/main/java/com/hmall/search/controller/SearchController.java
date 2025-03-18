package com.hmall.search.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.search.domain.ItemPageQuery;
import com.hmall.common.domain.PageDTO;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final RestHighLevelClient restHighLevelClient;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        // 分页查询基于Elas查询
        SearchRequest searchRequest = new SearchRequest("hmall_item");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.matchQuery("status",1));
        QueryBuilders.matchQuery("status", 1).operator(Operator.AND);
        if (StrUtil.isNotBlank(query.getKey())){
            boolQueryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()).operator(Operator.AND));
        }
        if (StrUtil.isNotBlank(query.getBrand())){
            boolQueryBuilder.filter(QueryBuilders.matchQuery("brand", query.getBrand()));
        }
        if (StrUtil.isNotBlank(query.getCategory())){
            boolQueryBuilder.filter(QueryBuilders.matchQuery("category", query.getCategory()));
        }
        if (query.getMaxPrice() != null){
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()).gte(query.getMinPrice()));
        }

        int toSize = query.getPageSize();
        int fromSize = (query.getPageNo() - 1) * query.getPageSize();
        SearchSourceBuilder elquery = searchRequest.source().query(boolQueryBuilder);
        SortOrder sortOrder = query.getIsAsc() ? SortOrder.ASC : SortOrder.DESC;
        if (StrUtil.isNotEmpty(query.getSortBy())){
            elquery.sort(query.getSortBy(), sortOrder).from(fromSize).size(toSize);
        }else{
            elquery.sort("updateTime", sortOrder).from(fromSize).size(toSize);
        }
        SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        PageDTO<ItemDTO> res = new PageDTO<>();
        long totalSize = search.getHits().getTotalHits().value;
        res.setTotal(totalSize);
        SearchHit[] hits = search.getHits().getHits();
        List<ItemDTO> list = new ArrayList<>();
        for (SearchHit hit : hits) {

            ItemDTO itemDTO = JSONUtil.toBean(hit.getSourceAsString(), ItemDTO.class);
            list.add(itemDTO);
        }
        res.setPages(Long.valueOf(totalSize / query.getPageSize() + 1)) ;
        res.setList(list);
        return res;
    }
}
