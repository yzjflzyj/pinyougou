package com.pinyougou.search.service.Impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.pinyougou.entity.EsItem;
import com.pinyougou.search.dao.ItemDao;
import com.pinyougou.search.service.ItemSearchService;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map search(Map searchMap) {
        Map map = new HashMap();
        //1.跟据查询条件-高亮搜索数据列表
        searchList(searchMap,map);
        //2.跟据查询条件-分组查询商品分类列表
        searchCategoryList(searchMap, map);

        //3.跟据商品分类名称查询商品品牌与规格列表
        String category = searchMap.get("category") == null ? "" : searchMap.get("category").toString();
        //如果用户没有传入商品分类
        if(category.trim().length() < 1){
            //默认使用查询第一个分类的品牌与规格
            List<String> categoryList = (List<String>) map.get("categoryList");
            if(categoryList != null && categoryList.size() > 0){
                category = categoryList.get(0);
            }
        }
        //查询商品品牌与规格列表
        this.searchBrandAndSpecList(category,map);


        return map;

    }



    /**
     * 跟据商品分类名称查询商品品牌与规格列表
     * @param category 商品分类名称
     * @param map 查询结果集
     */
    private void searchBrandAndSpecList(String category, Map map) {
        //跟据商品分类名称查询模板id(tb_Item_cat)
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        if (typeId != null) {
            //跟据模板ID查询品牌列表(tb_type_template)
            List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
            map.put("brandList", brandList);
            //跟据模板ID查询品牌列表
            List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
            map.put("specList", specList);
        }
    }

    /**
     * 跟据查询条件-分组查询商品分类列表(tb_Item表中的category)
     *
     * @param searchMap
     * @param map
     */
    private void searchCategoryList(Map searchMap, Map map) {

        //1.复制之前的NativeSearchQueryBuilder组装关键字查询条件的代码(不需要高亮查询部分)
        //1.1、创建查询条件构建器-builder = new NativeSearchQueryBuilder()
        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder();
        //1.2、先默认搜索所有商品-builder.withQuery(QueryBuilders.matchAllQuery())添加一个搜索条件
        builder.withQuery(QueryBuilders.matchAllQuery());
        //1.3、组装查询条件
        if (searchMap != null) {
            //1.3.1关键字搜索-builder.withQuery(QueryBuilders.matchQuery(域名，内容))
            String keyword = searchMap.get("keyword") == null ? "" : searchMap.get("keyword").toString();
            if (keyword.trim().length() > 0) {
                builder.withQuery(QueryBuilders.matchQuery("keyword", keyword));
            }
        }

        //2.设置分组域名-termsAggregationBuilder = AggregationBuilders.terms(别名).field(域名); .size设置显示分组条数,默认是10
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("group_category").field("category").size(100);
        //3.添加分组查询参数-builder.addAggregation(termsAggregationBuilder)
        builder.addAggregation(termsAggregationBuilder);
        //4.分组查询查询数据-aggregation = elasticsearchTemplate.query(builder.build(), new ResultsExtractor<Aggregations>(){})
        Aggregations aggregations = elasticsearchTemplate.query(builder.build(), new ResultsExtractor<Aggregations>() {
            //4.1实现extract(查询结果)方法
            @Override
            public Aggregations extract(SearchResponse response) {
                //4.2返回方法需要参数-response.getAggregations()
                 return  response.getAggregations();
            }
        });

        //5.提取分组结果数据-stringTerms = aggregations.get(填入刚才查询时的别名)
        StringTerms stringTerms = aggregations.get("group_category");
        //6.定义分类名字列表-categoryList = new ArrayList<String>()
        List<String> categoryList = new ArrayList<String>();
        //7.遍历读取分组查询结果-stringTerms.getBuckets().for
        for (StringTerms.Bucket bucket : stringTerms.getBuckets()) {
            //7.1获取分类名字，并将分类名字存入到集合中
            categoryList.add(bucket.getKeyAsString());
        }
        //8.返回分类数据列表-map.put("categoryList", categoryList)
        map.put("categoryList", categoryList);
        
    }


    /**
     * 跟据查询条件-高亮搜索数据列表
     *
     * @param searchMap 查询条件map
     * @return 查询结果
     */
    private Map searchList(Map searchMap,Map map) {
        //1、创建查询条件构建器-builder = new NativeSearchQueryBuilder()
        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder();
        //2、先默认搜索所有商品-builder.withQuery(QueryBuilders.matchAllQuery())添加一个搜索条件
        builder.withQuery(QueryBuilders.matchAllQuery());

        //h1.配置高亮查询信息-hField = new HighlightBuilder.Field()
        //h1.1:设置高亮域名-在构造函数中设置(设置位置,域)
        HighlightBuilder.Field hField = new HighlightBuilder.Field("title");
        //h1.2：设置高亮前缀-hField.preTags
        hField.preTags("<em style='color:red;'>"                                    );
        //h1.3：设置高亮后缀-hField.postTags
        hField.postTags("</em>");
        //h1.4：设置碎片大小-hField.fragmentSize
        hField.fragmentSize(100);
        //h1.5：追加高亮查询信息-builder.withHighlightFields()
        builder.withHighlightFields(hField);

        //3、组装查询条件
        if (searchMap != null) {
            //3.1关键字搜索-builder.withQuery(QueryBuilders.matchQuery(域名，内容))
            String keyword = searchMap.get("keyword") == null ? "" : searchMap.get("keyword").toString();
            if (keyword.trim().length() > 0) {

                //h2.不能使用拷贝域做高亮搜索-builder.withQuery(QueryBuilders.multiMatchQuery(内容,域名1，域名2...))
                //builder.withQuery(QueryBuilders.matchQuery("keyword",keyword));
                builder.withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "brand", "category", "seller"));
            }

            //创建BoolQueryBuilder，用于组装多个查询条件
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            //3.2 商品分类搜索
            String category = searchMap.get("category") == null ? "" : searchMap.get("category").toString();
            if (category.trim().length() > 0) {
                //注意商品分类查询不应该分词，所以用wildcardQuery而不是用matchQuery
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("category", category));
            }
            //3.3 品牌搜索
            String brand = searchMap.get("brand") == null ? "" : searchMap.get("brand").toString();
            if (brand.trim().length() > 0) {
                //注意品牌查询不应该分词，所以用wildcardQuery而不是用matchQuery
                boolQueryBuilder.must(QueryBuilders.wildcardQuery("brand", brand));
            }
            //3.4 规格搜索-前端传入spec: {'网络'：'移动4G','机身内存':'64G'}
            String spec = searchMap.get("spec") == null ? "" : searchMap.get("spec").toString();
            if (spec.trim().length() > 0) {
                //先把json串转换成map
                Map<String, String> specMap = JSON.parseObject(spec, Map.class);
                for (String key : specMap.keySet()) {
                    //嵌套域的name="spec.域名.keyword"
                    String name = "spec." + key + ".keyword";
                    //QueryBuilders.nestedQuery(nested根节点，wildcardQuery(),ScoreMode.Max(最大得分排序))
                    boolQueryBuilder.must(QueryBuilders.nestedQuery("spec",QueryBuilders.wildcardQuery(name,specMap.get(key)), ScoreMode.Max));
                }
            }

            //3.5 价格区间搜索-前端传入price:0-500|500-1000....3000-*
            String price = searchMap.get("price") == null ? "" : searchMap.get("price").toString();
            if(price.trim().length() > 0){
                //以中划线分隔价格:[0,500]
                String[] split = price.split("-");
                //价格比较表达式：0 <= price <= 500
                RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("price");
                //先处理:price >= 0
                priceRange.gte(split[0]);
                //再处理:price <= 500，传入3000-*时，不需要比较
                if(!"*".equals(split[1])){
                    priceRange.lte(split[1]);
                }
                //追加价格过滤条件
                boolQueryBuilder.must(priceRange);
            }


            //将BoolQueryBuilder设置为过滤查询
            builder.withFilter(boolQueryBuilder);

        }
        //4、获取NativeSearchQuery搜索条件对象-builder.build()
        NativeSearchQuery query = builder.build();

        //4.1 分页搜索
        //pageNo:当前页-默认第1页
        Integer pageNo = searchMap.get("pageNo") == null ? 1 : new Integer(searchMap.get("pageNo").toString());
        //pageSize:每页查询的条数-默认第20条
        Integer pageSize = searchMap.get("pageSize") == null ? 20 : new Integer(searchMap.get("pageSize").toString());
        //设置分页参数:PageRequest.of(当前页，从0开始, 每页查询的条数)
        query.setPageable(PageRequest.of(pageNo - 1, pageSize));
        //4.2 排序查询
        //sort:排序方式-asc|desc
        String sort = searchMap.get("sort") == null ? "" : searchMap.get("sort").toString();
        //sortField:排序业务域
        String sortField = searchMap.get("sortField") == null ? "" : searchMap.get("sortField").toString();
        if(sort.trim().length() > 0 && sortField.trim().length() > 0){
            //设置排序信息new Sort(排序方式Sort.Direction.ASC|DESC,排序的域名)
            Sort orderby = new Sort(sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
            query.addSort(orderby);
        }


        //5.查询数据-esTemplate.queryForPage(条件对象,搜索结果对象)
        //AggregatedPage<EsItem> page = elasticsearchTemplate.queryForPage(query, EsItem.class);

        //h3.高亮数据读取-AggregatedPage<EsItem> page = elasticsearchTemplate.queryForPage(query, EsItem.class, new SearchResultMapper(){})
        //h3.1实现mapResults(查询到的结果,数据列表的类型,分页选项)方法
        AggregatedPage<EsItem> page = elasticsearchTemplate.queryForPage(query, EsItem.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {

                //h3.2 先定义一组查询结果列表-List<T> list = new ArrayList<T>()
                List<T> list = new ArrayList<T>();
                //h3.3 遍历查询到的所有高亮数据-response.getHits().for
                for (SearchHit hit : response.getHits()) {
                    //h3.3.1 先获取当次结果的原始数据(无高亮)-hit.getSourceAsString()
                    String source = hit.getSourceAsString();
                    //h3.3.2 把json串转换为EsItem对象-esItem = JSON.parseObject()
                    EsItem esItem = JSON.parseObject(source, EsItem.class);
                    //h3.3.3 获取title域的高亮数据-titleHighlight = hit.getHighlightFields().get("title")
                    HighlightField titleHighlight = hit.getHighlightFields().get("title");

                    //h3.3.4 如果高亮数据不为空-读取高亮数据
                    if (titleHighlight != null) {
                        //h3.3.4.1 定义一个StringBuffer用于存储高亮碎片-buffer = new StringBuffer()
                        StringBuffer buffer = new StringBuffer();
                        //h3.3.4.2 循环组装高亮碎片数据-titleHighlight.getFragments().for(追加数据)
                        for (Text fragment : titleHighlight.getFragments()) {
                            buffer.append(fragment);
                        }
                        //h3.3.4.3 将非高亮数据替换成高亮数据-esItem.setTitle()
                        esItem.setTitle(buffer.toString());
                    }
                    //h3.3.5 将替换了高亮数据的对象封装到List中-list.add((T) esItem)
                    list.add((T) esItem);
                }
                //h3.4 返回当前方法所需要参数-new AggregatedPageImpl<T>(数据列表，分页选项,总记录数)
                //h3.4 参考new AggregatedPageImpl<T>(list,pageable,response.getHits().getTotalHits())
                return new AggregatedPageImpl<T>(list, pageable, response.getHits().getTotalHits());
            }
        });
        //6、包装结果并返回
        map.put("rows", page.getContent());

        //返回分页数据
        //总记录数
        map.put("total", page.getTotalElements());
        //总页数
        map.put("totalPages", page.getTotalPages());

        return map;
    }

    //更新索引库
    @Autowired
    private ItemDao itemDao;
    @Override
    public void importList(List list) {
        itemDao.saveAll(list);
    }

    //删除索引库
    @Override
    public void deleteByGoodsId(Long[] goodsIdList) {
        itemDao.deleteByGoodsIdIn(goodsIdList);
    }


}
