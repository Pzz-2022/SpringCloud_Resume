package com.resume.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.resume.base.model.PageBean;
import com.resume.base.model.TokenInfo;
import com.resume.dubbo.api.SearchService;
import com.resume.dubbo.domian.Position;
import com.resume.dubbo.domian.PositionDTO;
import com.resume.dubbo.domian.Resume;
import com.resume.dubbo.domian.SearchCondition;
import com.resume.search.mapstruct.PositionMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.resume.base.utils.Constant.*;
import static com.resume.dubbo.domian.Constant.*;
import static com.resume.search.utils.ElasticSearchConstants.POSITION_INDEX;
import static com.resume.search.utils.ElasticSearchConstants.RESUME_INDEX;

@DubboService
public class SearchServiceImpl implements SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /*
        职位
     */
    @Override
    public Boolean savePositionDTOs(List<PositionDTO> positionArr) {
        deleteIndex(POSITION_INDEX);

        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("10s");

        // 批量请求处理
        for (PositionDTO positionDTO : positionArr) {
            bulkRequest.add(
                    // 这里是数据信息
                    new IndexRequest(POSITION_INDEX)
                            .id("" + positionDTO.getPkPositionId()) // 没有设置id 会自定生成一个随机id
                            .source(JSON.toJSONString(positionDTO), XContentType.JSON)
            );
        }

        BulkResponse response = null;
        try {
            response = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
//            System.out.println("批量添加结果：" + response.status());// OK
            return true;
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    @Override
    public Boolean savePositionDTO(PositionDTO positionDTO) {
        // 创建请求
        IndexRequest request = new IndexRequest(POSITION_INDEX);
        // 制定规则 PUT /pp_index/_doc/1
        request.id("" + positionDTO.getPkPositionId());// 设置文档ID
        request.timeout(TimeValue.timeValueMillis(1000));// request.timeout("1s")
        // 将我们的数据放入请求中
        request.source(JSON.toJSONString(positionDTO), XContentType.JSON);
        // 客户端发送请求，获取响应的结果
        IndexResponse response = null;
        try {
            response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
            // 获取建立索引的状态信息 CREATED
            System.out.println(response.status());
            // 查看返回内容 IndexResponse[index=pp_index,type=_doc,id=1,version=1,result=created,seqNo=0,primaryTerm=1,shards={"total":2,"successful":1,"failed":0}]
            System.out.println(response);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Boolean deletePositionDTOById(Long id) {
        DeleteRequest request = new DeleteRequest(POSITION_INDEX, "" + id);
        request.timeout("1s");

        DeleteResponse response = null;
        try {
            response = restHighLevelClient.delete(request, RequestOptions.DEFAULT);
//            System.out.println("删除状态：" + response.status());// OK
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Boolean updatePositionDTOById(PositionDTO positionDTO) {
        UpdateRequest updateRequest = new UpdateRequest(POSITION_INDEX, "" + positionDTO.getPkPositionId());

        updateRequest.doc(JSON.toJSONString(positionDTO), XContentType.JSON);

        try {
            UpdateResponse response = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
//            System.out.println("更新状态：" + response.status());// OK
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Position getPositionById(Long id) {
        GetRequest request = new GetRequest(POSITION_INDEX, "" + id);

        try {
            GetResponse response = restHighLevelClient.get(request, RequestOptions.DEFAULT);

            String sourceAsString = response.getSourceAsString();
            PositionDTO positionDTO = JSON.parseObject(sourceAsString, PositionDTO.class);

            // 将 Dto 转换成 pojo
            return PositionMapper.INSTANCE.convertToPosition(positionDTO);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    // 查询
// SearchRequest 搜索请求
// SearchSourceBuilder 条件构造
// HighlightBuilder 高亮
// TermQueryBuilder 精确查询
// MatchAllQueryBuilder
// xxxQueryBuilder ...
    @Override
    public PageBean<Position> searchPosition(SearchCondition searchCondition, TokenInfo tokenInfo) {
        // 1.创建查询请求对象
        SearchRequest searchRequest = new SearchRequest(POSITION_INDEX);
        // 2.构建搜索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // (1)查询条件 使用QueryBuilders工具类创建
        // 多条件查询
        // 搜索限定 公司、设置关键词
        BoolQueryBuilder termQueryBuilder = QueryBuilders.boolQuery();
        if (searchCondition.getQuery() != null && !"".equals(searchCondition.getQuery())) {
            termQueryBuilder.should(QueryBuilders.matchQuery(POSITION_NAME, searchCondition.getQuery()))
                    .should(QueryBuilders.matchQuery(WORKING_CITY, searchCondition.getQuery()))
                    .should(QueryBuilders.matchQuery(DESCRIPTION, searchCondition.getQuery()));
        }

        // 限制除了 超级管理员 其他角色都只能看到本公司的
        if (!SUPER_ADMIN.equals(tokenInfo.getRole()))
            termQueryBuilder.must(QueryBuilders.termQuery(COMPANY_ID, tokenInfo.getCompanyId()));

        if (searchCondition.getState() != -1)
            termQueryBuilder.must(QueryBuilders.termQuery(STATE, searchCondition.getState()));

        System.out.println("查询人角色：" + tokenInfo.getRole());
        System.out.println("查询人公司ID：" + tokenInfo.getCompanyId());

        switch (tokenInfo.getRole()) {
            case HR:
            case INTERVIEWER:
                termQueryBuilder.must(QueryBuilders.termQuery(POSITION_TEAM_ID_LIST, tokenInfo.getPkUserId()));
                break;

            case COMPANY_ADMIN:
            case SUPER_ADMIN:
                break;
        }

        // (2)其他<可有可无>：（可以参考 SearchSourceBuilder 的字段部分）
        // 设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(POSITION_NAME);
        highlightBuilder.preTags("<span class='key-word' style='color: red'>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        // 分页
        if (searchCondition.getPageSize() != -1) {
            searchSourceBuilder.from((searchCondition.getPage() - 1) * searchCondition.getPageSize());
            searchSourceBuilder.size(searchCondition.getPageSize());
        }

        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        // (3)条件投入
        searchSourceBuilder.query(termQueryBuilder);

        // 3.添加条件到请求
        searchRequest.source(searchSourceBuilder);
        // 4.客户端查询请求
        SearchResponse search = null;
        try {
            search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 5.查看返回结果
        SearchHits hits = search.getHits();
        System.out.println(JSON.toJSONString(hits));
        System.out.println("=======================");

        List<Position> resultArr = new ArrayList<>();
        for (SearchHit documentFields : hits.getHits()) {
            // 使用新的字段值（高亮），覆盖旧的字段值
            Map<String, Object> sourceAsMap = documentFields.getSourceAsMap();
            // 高亮字段
            Map<String, HighlightField> highlightFields = documentFields.getHighlightFields();
            HighlightField name = highlightFields.get(POSITION_NAME);

            // 替换
            if (name != null) {
                Text[] fragments = name.fragments();
                StringBuilder new_name = new StringBuilder();
                for (Text text : fragments) {
                    new_name.append(text);
                }
                sourceAsMap.put(POSITION_NAME, new_name.toString());
            }

            // 使用工具快速将Map转化为Bean
            Position position = BeanUtil.fillBeanWithMap(sourceAsMap, new Position(), false);
            resultArr.add(position);
        }
        // 完整分页数据
        System.out.println(resultArr);

        // hits.getTotalHits().value  得到符合条件的总数
        int totalCount = Math.toIntExact(hits.getTotalHits().value);

        return new PageBean<>(searchCondition.getQuery(), totalCount, totalCount / searchCondition.getPageSize(),
                searchCondition.getPage(), resultArr);
    }


    /*
        简历
     */
// ==========================================================================================
    @Override
    public Boolean saveResumes(List<Resume> resumeList) {
        deleteIndex(RESUME_INDEX);

        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("10s");

        // 批量请求处理
        for (Resume resume : resumeList) {
            bulkRequest.add(
                    // 这里是数据信息
                    new IndexRequest(RESUME_INDEX)
                            .id("" + resume.getPkResumeId()) // 没有设置id 会自定生成一个随机id
                            .source(JSON.toJSONString(resume), XContentType.JSON)
            );
        }

        BulkResponse response = null;
        try {
            response = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
//            System.out.println("批量添加结果：" + response.status());// OK
            return true;
        } catch (IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    @Override
    public Boolean saveResume(Resume resume) {
        // 创建请求
        IndexRequest request = new IndexRequest(RESUME_INDEX);
        // 制定规则 PUT /pp_index/_doc/1
        request.id("" + resume.getPkResumeId());// 设置文档ID
        request.timeout(TimeValue.timeValueMillis(1000));// request.timeout("1s")
        // 将我们的数据放入请求中
        request.source(JSON.toJSONString(resume), XContentType.JSON);
        // 客户端发送请求，获取响应的结果
        IndexResponse response = null;
        try {
            response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
            // 获取建立索引的状态信息 CREATED
            System.out.println(response.status());
            // 查看返回内容 IndexResponse[index=pp_index,type=_doc,id=1,version=1,result=created,seqNo=0,primaryTerm=1,shards={"total":2,"successful":1,"failed":0}]
            System.out.println(response);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Boolean updateResumeById(Resume resume) {
        UpdateRequest updateRequest = new UpdateRequest(RESUME_INDEX, "" + resume.getPkResumeId());

        updateRequest.doc(JSON.toJSONString(resume), XContentType.JSON);

        try {
            UpdateResponse response = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
//            System.out.println("更新状态：" + response.status());// OK
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Resume getResumeById(Long id) {
        GetRequest request = new GetRequest(RESUME_INDEX, "" + id);

        try {
            GetResponse response = restHighLevelClient.get(request, RequestOptions.DEFAULT);
            String sourceAsString = response.getSourceAsString();

            return JSON.parseObject(sourceAsString, Resume.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static float multipleInt = 0;

    @Override
    public PageBean<Resume> searchResume(SearchCondition searchCondition, TokenInfo tokenInfo, Long positionId) {
        // 1.创建查询请求对象
        SearchRequest searchRequest = new SearchRequest(RESUME_INDEX);
        // 2.构建搜索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // (1)查询条件 使用QueryBuilders工具类创建
        // 多条件查询
        // 搜索限定 公司、设置关键词
        BoolQueryBuilder termQueryBuilder = QueryBuilders.boolQuery();
        if (searchCondition.getQuery() != null && !"".equals(searchCondition.getQuery())) {
            termQueryBuilder.should(QueryBuilders.matchQuery(POSITION_NAME, searchCondition.getQuery()))
                    .should(QueryBuilders.matchQuery(NAME, searchCondition.getQuery()))
                    .should(QueryBuilders.matchQuery(GRADUATE_INSTITUTION, searchCondition.getQuery()))
                    .should(QueryBuilders.matchQuery(RESUME_CONTENT, searchCondition.getQuery()));
        }

        // 限制除了 超级管理员 其他角色都只能看到本公司的
        if (!SUPER_ADMIN.equals(tokenInfo.getRole()))
            termQueryBuilder.must(QueryBuilders.termQuery(COMPANY_ID, tokenInfo.getCompanyId()));

        // 查询未删除的简历
        if (searchCondition.getState() == null || searchCondition.getState() != 1)
            termQueryBuilder.must(QueryBuilders.termQuery(IS_DELETE, 0));

        // 查询已经解析的数据
        termQueryBuilder.must(QueryBuilders.termQuery(IS_Parsed, 1));

        System.out.println("查询人角色：" + tokenInfo.getRole());
        System.out.println("查询人公司ID：" + tokenInfo.getCompanyId());


        //        // 匹配查询
        //        MatchAllQueryBuilder matchAllQueryBuilder = QueryBuilders.matchAllQuery();
        // (2)其他<可有可无>：（可以参考 SearchSourceBuilder 的字段部分）
        // 设置高亮
//        HighlightBuilder highlightBuilder = new HighlightBuilder();
//        highlightBuilder.field(POSITION_NAME);
//        highlightBuilder.preTags("<span class='key-word'>");
//        highlightBuilder.postTags("</span>");
//        searchSourceBuilder.highlighter(highlightBuilder);


        // 如果职位 id 不为 null 则根据id查询
        if (positionId != 0) {
            termQueryBuilder.must(QueryBuilders.termQuery(POSITION_ID, positionId));
        }else {
            // 分页
            searchSourceBuilder.from((searchCondition.getPage() - 1) * searchCondition.getPageSize());
            searchSourceBuilder.size(searchCondition.getPageSize());
        }

        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        // (3)条件投入
        searchSourceBuilder.query(termQueryBuilder);

        // 3.添加条件到请求
        searchRequest.source(searchSourceBuilder);
        // 4.客户端查询请求
        SearchResponse search = null;
        try {
            search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 5.查看返回结果
        SearchHits hits = search.getHits();
//        System.out.println(JSON.toJSONString(hits));
//        System.out.println("=======================");

        if (multipleInt == 0)
            multipleInt = 80 / hits.getHits()[0].getScore();
        List<Resume> resultArr = new ArrayList<>();
        for (SearchHit documentFields : hits.getHits()) {
            // 使用新的字段值（高亮），覆盖旧的字段值
            Map<String, Object> sourceAsMap = documentFields.getSourceAsMap();
            // 高亮字段
            Map<String, HighlightField> highlightFields = documentFields.getHighlightFields();
            HighlightField name = highlightFields.get(POSITION_NAME);

            // 替换
            if (name != null) {
                Text[] fragments = name.fragments();
                StringBuilder new_name = new StringBuilder();
                for (Text text : fragments) {
                    new_name.append(text);
                }
                sourceAsMap.put(POSITION_NAME, new_name.toString());
            }

            // 使用工具快速将Map转化为Bean
            Resume resume = BeanUtil.fillBeanWithMap(sourceAsMap, new Resume(), false);

            // 转化为 Vo
            resume.setScore(documentFields.getScore() * multipleInt);

            resultArr.add(resume);
        }
        // 完整分页数据
        System.out.println(resultArr);

        // hits.getTotalHits().value  得到符合条件的总数
        int totalCount = Math.toIntExact(hits.getTotalHits().value);

        return new PageBean<>(searchCondition.getQuery(), totalCount, totalCount / searchCondition.getPageSize() + (totalCount % searchCondition.getPageSize() != 0 ? 1 : 0),
                searchCondition.getPage(), resultArr);
    }



    // 根据索引名称删除索引
    private boolean deleteIndex(String indexName) {
        // 先判断是否存在这个索引
        boolean isExists = false;
        try {
            isExists = indexIsExists(indexName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!isExists)
            return false;

        // 发出删除请求
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        AcknowledgedResponse response = null;
        try {
            response = restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
//            System.out.println(response.isAcknowledged());// 是否删除成功
            return response.isAcknowledged();
        } catch (Exception e) {
            return false;
        }
    }

    // 判断是否存在这个索引
    private boolean indexIsExists(String indexName) throws IOException {
        GetIndexRequest request = new GetIndexRequest(indexName);
        boolean exists = restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exists);// 索引是否存在
        return exists;
    }
}
