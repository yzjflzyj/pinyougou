package com.pinyougou.seckill.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.abel533.entity.Example;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.entity.PageResult;
import com.pinyougou.mapper.TbSeckillGoodsMapper;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.seckill.service.SeckillGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;


/**
 * 业务逻辑实现
 *
 * @author Steven
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbSeckillGoods> findAll() {
        return seckillGoodsMapper.select(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize, TbSeckillGoods seckillGoods) {
        PageResult<TbSeckillGoods> result = new PageResult<TbSeckillGoods>();
        //设置分页条件
        PageHelper.startPage(pageNum, pageSize);
        //构建查询条件
        Example example = new Example(TbSeckillGoods.class);
        Example.Criteria criteria = example.createCriteria();

        if (seckillGoods != null) {
            //如果字段不为空
            if (seckillGoods.getTitle() != null && seckillGoods.getTitle().length() > 0) {
                criteria.andLike("title", "%" + seckillGoods.getTitle() + "%");
            }
            //如果字段不为空
            if (seckillGoods.getSmallPic() != null && seckillGoods.getSmallPic().length() > 0) {
                criteria.andLike("smallPic", "%" + seckillGoods.getSmallPic() + "%");
            }
            //如果字段不为空
            if (seckillGoods.getSellerId() != null && seckillGoods.getSellerId().length() > 0) {
                criteria.andLike("sellerId", "%" + seckillGoods.getSellerId() + "%");
            }
            //如果字段不为空
            if (seckillGoods.getStatus() != null && seckillGoods.getStatus().length() > 0) {
                criteria.andLike("status", "%" + seckillGoods.getStatus() + "%");
            }
            //如果字段不为空
            if (seckillGoods.getIntroduction() != null && seckillGoods.getIntroduction().length() > 0) {
                criteria.andLike("introduction", "%" + seckillGoods.getIntroduction() + "%");
            }


        }
        //审核通过才展示
        criteria.andEqualTo("status", 1);
        //查询数据
        List<TbSeckillGoods> list = seckillGoodsMapper.selectByExample(example);
        //返回数据列表
        result.setRows(list);

        //获取总页数
        PageInfo<TbSeckillGoods> info = new PageInfo<TbSeckillGoods>(list);
        result.setPages(info.getPages());

        return result;
    }

    /**
     * 增加
     */
    @Override
    public void add(TbSeckillGoods seckillGoods) {
        seckillGoodsMapper.insertSelective(seckillGoods);
    }


    /**
     * 修改
     */
    @Override
    public void update(TbSeckillGoods seckillGoods) {
        seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbSeckillGoods getById(Long id) {
        return seckillGoodsMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        TbSeckillGoods record = new TbSeckillGoods();
        record.setStatus("0");
        //数组转list
        List longs = Arrays.asList(ids);
        //构建查询条件
        Example example = new Example(TbSeckillGoods.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", longs);

        //跟据查询条件删除数据
        seckillGoodsMapper.updateByExampleSelective(record, example);
    }

    //跟据id列表，更新状态,  商品审核
    @Override
    public void updateStatus(Long[] ids, String status) {
        //要更新的内容
        TbSeckillGoods record = new TbSeckillGoods();
        record.setStatus(status);
        //更新条件组装
        Example example = new Example(TbGoods.class);
        Example.Criteria criteria = example.createCriteria();
        List longs = Arrays.asList(ids);
        criteria.andIn("id", longs);
        //执行更新
        seckillGoodsMapper.updateByExampleSelective(record, example);
    }

    //从缓存中查秒杀商品,在首页中分页显示
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisTemplate stringHashRedisTemplate;


    @Override
    public List<TbSeckillGoods> findList() {
        //从Redis中查找商品列表
        List<TbSeckillGoods> seckillGoods = redisTemplate.boundHashOps("seckillGoods").values();
        return seckillGoods;
    }


    /***
     * 获取商品库存和当前时间距离结束时间的时间差
     * @return
     */
    @Override
    public Map<String, Object> getGoodsInfoById(Long id) {
        Map map = new HashMap();
        //库存数
        String count = (String) stringHashRedisTemplate.boundHashOps("seckillGoodsStockCount").get(id);
        /*
        不用get方式获得,就不会出现序列化异常的问题
        Long count = (Long) redisTemplate.boundHashOps("seckillGoodsStockCount").increment(id,0);
        */
        map.put("count", count);
        //取出商品信息
        TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps("seckillGoods").get(id);
        //计算时间差-总秒数
        long allsecond = (seckillGoods.getEndTime().getTime() - new Date().getTime()) / 1000;
        map.put("allsecond", allsecond);
        return map;
    }


}
