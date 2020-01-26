package com.pinyougou.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.abel533.entity.Example;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.entity.PageResult;
import com.pinyougou.mapper.*;
import com.pinyougou.pojo.*;
import com.pinyougou.pojogroup.Goods;
import com.pinyougou.sellergoods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 业务逻辑实现
 *
 * @author Steven
 */
@Service(interfaceClass = GoodsService.class)
@Transactional
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private TbGoodsMapper goodsMapper;

    @Autowired
    private TbGoodsDescMapper goodsDescMapper;

    @Autowired
    private TbItemMapper itemMapper;

    @Autowired
    private TbBrandMapper brandMapper;

    @Autowired
    private TbItemCatMapper itemCatMapper;

    @Autowired
    private TbSellerMapper sellerMapper;


    /**
     * 查询全部
     */
    @Override
    public List<TbGoods> findAll() {
        return goodsMapper.select(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize, TbGoods goods) {
        PageResult<TbGoods> result = new PageResult<TbGoods>();
        //设置分页条件
        PageHelper.startPage(pageNum, pageSize);

        //构建查询条件
        Example example = new Example(TbGoods.class);
        Example.Criteria criteria = example.createCriteria();
        //查询的是删除状态为null的数据(删除的状态为1,未删除的状态为null)
        criteria.andIsNull("isDelete");
        if (goods != null) {
            //如果字段不为空
            if (goods.getSellerId() != null && goods.getSellerId().length() > 0) {
                // criteria.andLike("sellerId", "%" + goods.getSellerId() + "%");
                criteria.andEqualTo("sellerId", goods.getSellerId());
            }
            //如果字段不为空
            if (goods.getGoodsName() != null && goods.getGoodsName().length() > 0) {
                criteria.andLike("goodsName", "%" + goods.getGoodsName() + "%");
            }
            //如果字段不为空
            if (goods.getAuditStatus() != null && goods.getAuditStatus().length() > 0) {
                criteria.andLike("auditStatus", "%" + goods.getAuditStatus() + "%");
            }
            //如果字段不为空
            if (goods.getIsMarketable() != null && goods.getIsMarketable().length() > 0) {
                criteria.andLike("isMarketable", "%" + goods.getIsMarketable() + "%");
            }
            //如果字段不为空
            if (goods.getCaption() != null && goods.getCaption().length() > 0) {
                criteria.andLike("caption", "%" + goods.getCaption() + "%");
            }
            //如果字段不为空
            if (goods.getSmallPic() != null && goods.getSmallPic().length() > 0) {
                criteria.andLike("smallPic", "%" + goods.getSmallPic() + "%");
            }
            //如果字段不为空
            if (goods.getIsEnableSpec() != null && goods.getIsEnableSpec().length() > 0) {
                criteria.andLike("isEnableSpec", "%" + goods.getIsEnableSpec() + "%");
            }
            //如果字段不为空
            if (goods.getIsDelete() != null && goods.getIsDelete().length() > 0) {
                criteria.andLike("isDelete", "%" + goods.getIsDelete() + "%");
            }

        }

        //查询数据
        List<TbGoods> list = goodsMapper.selectByExample(example);
        //返回数据列表
        result.setRows(list);

        //获取总页数
        PageInfo<TbGoods> info = new PageInfo<TbGoods>(list);
        result.setPages(info.getPages());

        return result;
    }

    @Override
    public void add(Goods goods) {
        //设置状态为未审核
        goods.getGoods().setAuditStatus("0");
        goodsMapper.insertSelective(goods.getGoods());

        //回滚检测// int x=1/0;

        //设置商品id
        goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());
        goodsDescMapper.insertSelective(goods.getGoodsDesc());

        //保存SKU
        saveItemList(goods);

    }


    /**
     * 设置SKU详细信息
     *
     * @param goods
     * @param item
     */
    private void setItemValus(Goods goods, TbItem item) {
        //商品图片取SPU的第一张
        List<Map> imgList = JSON.parseArray(goods.getGoodsDesc().getItemImages(), Map.class);
        if (imgList.size() > 0) {
            item.setImage(imgList.get(0).get("url").toString());
        }

        //商品类目id
        item.setCategoryid(goods.getGoods().getCategory3Id());
        //查询商品类目内容
        TbItemCat itemCat = itemCatMapper.selectByPrimaryKey(item.getCategoryid());
        item.setCategory(itemCat.getName());

        //创建日期
        item.setCreateTime(new Date());
        //更新日期
        item.setUpdateTime(item.getCreateTime());

        //所属SPU-id
        item.setGoodsId(goods.getGoods().getId());
        //所属商家
        item.setSellerId(goods.getGoods().getSellerId());
        TbSeller seller = sellerMapper.selectByPrimaryKey(item.getSellerId());
        item.setSeller(seller.getNickName());

        //品牌信息
        TbBrand brand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
        item.setBrand(brand.getName());
    }


    /**
     * 修改
     */
    @Override
    public void update(Goods goods) {
        //修改过的商品，状态设置为未审核，重新审核一次
        goods.getGoods().setAuditStatus("0");
        //更新商品基本信息
        goodsMapper.updateByPrimaryKeySelective(goods.getGoods());
        //更新商品扩展信息
        goodsDescMapper.updateByPrimaryKeySelective(goods.getGoodsDesc());
        //更新sku信息，更新前先删除原来的sku
        TbItem where = new TbItem();
        where.setGoodsId(goods.getGoods().getId());
        itemMapper.delete(where);
        //保存新的SKU
        saveItemList(goods);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public Goods getById(Long id) {
        Goods goods = new Goods();
        TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
        goods.setGoods(tbGoods);
        TbGoodsDesc tbGoodsDesc = goodsDescMapper.selectByPrimaryKey(id);
        goods.setGoodsDesc(tbGoodsDesc);
        //查询商品SKU信息
        TbItem where = new TbItem();
        where.setGoodsId(id);
        List<TbItem> items = itemMapper.select(where);
        goods.setItemList(items);

        return goods;

    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        TbGoods tbGoods = new TbGoods();
        tbGoods.setIsDelete("1");
        //数组转list
        List longs = Arrays.asList(ids);
        //构建查询条件
        Example example = new Example(TbGoods.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", longs);

        //跟据查询条件更改isDelete的状态
        goodsMapper.updateByExampleSelective(tbGoods, example);
    }

    /**
     * 商品审核,根据ids更新状态
     *
     * @param ids
     * @param status
     */
    @Override
    public void updateStatus(Long[] ids, String status) {
        TbGoods tbgoods = new TbGoods();
        tbgoods.setAuditStatus(status);
        Example example = new Example(TbGoods.class);
        Example.Criteria criteria = example.createCriteria();
        List longs = Arrays.asList(ids);
        criteria.andIn("id", longs);
        goodsMapper.updateByExampleSelective(tbgoods, example);

    }

    /**
     * 插入SKU列表数据
     *
     * @param goods
     */
    private void saveItemList(Goods goods) {

        if ("1".equals(goods.getGoods().getIsEnableSpec())) {
            //保存SKU
            for (TbItem item : goods.getItemList()) {
                //标题由SPU+ SKU标题组成
                String title = goods.getGoods().getGoodsName();
                Map<String, Object> skuMap = JSON.parseObject(item.getSpec());
                for (String key : skuMap.keySet()) {
                    title = title + " " + skuMap.get(key);
                }
                item.setTitle(title);
                setItemValus(goods, item);
                //保存SKU
                itemMapper.insertSelective(item);
            }
        } else {
            TbItem item = new TbItem();
            item.setTitle(goods.getGoods().getGoodsName());//商品KPU+规格描述串作为SKU名称
            item.setPrice(goods.getGoods().getPrice());//价格
            item.setStatus("1");//状态
            item.setIsDefault("1");//是否默认
            item.setNum(99999);//库存数量
            item.setSpec("{}");
            setItemValus(goods, item);
            itemMapper.insertSelective(item);
        }
    }


    /**
     * 跟据SPU-ID列表和状态，查询SKU列表
     * @param goodsIds
     * @param status
     * @return
     */
    @Override
    public List<TbItem> findItemListByGoodsIdsAndStatus(Long[] goodsIds, String status) {
        Example example = new Example(TbItem.class);
        Example.Criteria criteria = example.createCriteria();
        //查询条件设置-spu列表
        List longs = Arrays.asList(goodsIds);
        criteria.andIn("goodsId", longs);
        //查询条件设置-商品状态
        criteria.andEqualTo("status", status);
        //查询结果
        List<TbItem> items = itemMapper.selectByExample(example);
        return items;
    }



}
