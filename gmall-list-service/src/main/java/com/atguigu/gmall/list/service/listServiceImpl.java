package com.atguigu.gmall.list.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
public class listServiceImpl implements ListService {
    @Autowired
    private JestClient jestClient;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";


    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        // 保存数据
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            DocumentResult documentResult = jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        // 先声明好dsl 语句
        String query = makeQueryStringForSearch(skuLsParams);
        // 创建查询对象
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 将执行的结果集 searchResult 转换为 SkuLsResult
        SkuLsResult skuLsResult =  makeResultForSearch(skuLsParams,searchResult);
        // 返回结果集数据
        return skuLsResult;


    }


    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 构建一个查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 创建 query - bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 设置过滤{skuAttrValueList.valueId,catalog3Id}，以及匹配{skuName}
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // must :   match "skuName": "小米"
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            // 将match 放入must
            boolQueryBuilder.must(matchQueryBuilder);
            // 设置高亮，获得高亮对象
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            // 设置高亮字段
            highlighter.field("skuName");
            // 设置前缀，后缀
            highlighter.preTags("<span style='color:red'>");
            highlighter.postTags("</span>");
            // 放入设置好的高亮对象
            searchSourceBuilder.highlight(highlighter);
        }
        // 设置三级分类Id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            // 设置term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            // 将term 放入filter
            boolQueryBuilder.filter(termQueryBuilder);
        }
        // 平台属性值Id
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环遍历里面的值
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                // 取得平台属性值Id
                String valueId = skuLsParams.getValueId()[i];
                // 设置term
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                // 将term 放入filter
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // 设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        // 分页 from : 表示从第几页开始显示
        // 分页公式：(pageNo-1)*pageSize
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        // size:每页显示的条数
        searchSourceBuilder.size(skuLsParams.getPageSize());

        // 聚合：按照平台属性值Id 进行聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        // 聚合对象放入查询体
        searchSourceBuilder.aggregation(groupby_attr);
        // 查询 --- query
        searchSourceBuilder.query(boolQueryBuilder);
        // 返回字符串
        String query  = searchSourceBuilder.toString();
        System.out.println("query:"+query);

        return query;
    }
    // 将查询出来的结果进行转换 SkuLsResult
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();
//        SkuLsResult
//        List<SkuLsInfo> skuLsInfoList;
//        long total;
//        long totalPages;
//        List<String> attrValueIdList;
        // 开始对属性赋值！skuLsInfoArrayList 该集合中存放的数据应该是来自于searchResult
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        //  提取数据searchResult
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        // 循环遍历取值
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            // 取得里面的每一个数据对象
            SkuLsInfo skuLsInfo = hit.source;
            // 取出高亮字段，并将原来的skuName 进行覆盖
            if (hit.highlight!=null && hit.highlight.size()>0){
                // 取出高亮 skuName: <span style='color:red'>小米</span>0715二代
                List<String> list = hit.highlight.get("skuName");
                // 取出高亮字段
                String skuNameHl  = list.get(0);
                // 开始替换
                skuLsInfo.setSkuName(skuNameHl);
            }
            skuLsInfoArrayList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
        // 赋值总条数
        skuLsResult.setTotal(searchResult.getTotal());
        // 设置总页数totalPages
        // 如何计算总页数：总条数/每页显示的条数   10 3 4  ||  9 ，3 ，3
        // long totalPage = (searchResult.getTotal()%skuLsParams.getPageSize()==0)?searchResult.getTotal()/skuLsParams.getPageSize():(searchResult.getTotal()/skuLsParams.getPageSize()+1);
        long totalPage =(searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        // 实践公式：
        skuLsResult.setTotalPages(totalPage);

        // 声明一个集合来存储平台属性值Id
        ArrayList<String> arrayList = new ArrayList<>();
        // 向集合中添加平台属性值Id，先获得当前所有的平台属性值Id
        // 可以通过聚合取得
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            arrayList.add(valueId);
        }
        // 设置平台属性值Id
        skuLsResult.setAttrValueIdList(arrayList);
        return skuLsResult;
    }

}

