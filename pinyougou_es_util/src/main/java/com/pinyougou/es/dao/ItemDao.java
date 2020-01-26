package com.pinyougou.es.dao;

import com.pinyougou.entity.EsItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 商品信息ES接口
 */
public interface ItemDao extends ElasticsearchRepository<EsItem,Long> {
}

