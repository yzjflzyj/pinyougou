package com.pinyougou.test;

import com.alibaba.fastjson.JSON;
import com.pinyougou.entity.EsItem;
import com.pinyougou.es.dao.ItemDao;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ElasticSearch商品信息导入工具
 * @author Steven
 * @version 1.0
 * @description com.pinyougou.test
 * @date 2019-5-20
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:spring/applicationContext-*.xml")
public class EsUtils {
    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private ItemDao itemDao;

    @Test
    public void testImportData(){
        TbItem where = new TbItem();
        //只导入已审核的商品
        where.setStatus("1");
        List<TbItem> items = itemMapper.select(where);
        System.out.println("总共将要导入 " + items.size() + " 个商品。");
        //System.out.println("-------------商品列表开始-------------");
        //es实体列表
        List<EsItem> esItemList=new ArrayList<>();
        EsItem esItem = null;
        for (TbItem item : items) {
            esItem = new EsItem();
            //System.out.println(item.getId() + " " + item.getTitle() + "  " + item.getPrice());
            //使用spring的BeanUtils深克隆对象,相当于把TbItem所有属性名与数据类型相同的属性值设置到EsItem中
            BeanUtils.copyProperties(item,esItem);
            //数据库中的price的数据类型为decimal
            esItem.setPrice(item.getPrice().doubleValue());
            //规格的嵌套域的数据绑定
            Map specMap = JSON.parseObject(item.getSpec(), Map.class);
            esItem.setSpec(specMap);
            esItemList.add(esItem);
        }
        System.out.println("开始导入到索引库");
        itemDao.saveAll(esItemList);
        System.out.println("导入结束");
    }
}
