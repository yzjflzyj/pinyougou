package com.pinyougou.search.dao;

import com.pinyougou.entity.EsItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 商品信息ES接口
 */
public interface ItemDao extends ElasticsearchRepository<EsItem,Long> {
    //ElasticsearchRepository删除语法：deleteBy+域名+匹配方式
    void deleteByGoodsIdIn(Long[] ids);
}