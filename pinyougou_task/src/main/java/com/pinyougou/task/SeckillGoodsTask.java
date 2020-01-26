package com.pinyougou.task;

import com.github.abel533.entity.Example;
import com.pinyougou.mapper.TbSeckillGoodsMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class SeckillGoodsTask {

  /*  //每秒执行
    @Scheduled(cron = "* * * * * ?")
    public void startTask(){
        System.out.println("定时执行了startTask:" + new Date());
    }*/


    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    //30秒执行一次
    @Scheduled(cron = "0/30 * * * * ?")
    public void refreshSeckillGoods() {

        Example example = new Example(TbSeckillGoods.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("status", "1");
        criteria.andGreaterThan("stockCount", 0);
        Date nowTime = new Date();  //入库的当前时间
        //开始时间小于等于当前时间
        criteria.andLessThanOrEqualTo("startTime", nowTime);
        //结束时间大于等当前时间
        criteria.andGreaterThanOrEqualTo("endTime", nowTime);

        //排除缓存中已经有的商品信息，因为接下来流程中，我们用到Redis库存数据,而数据库数据更新是在商品售罄时
        Set ids = redisTemplate.boundHashOps("seckillGoods").keys();
        if (ids != null && ids.size() > 0) {
            //set转list
            List idList = new ArrayList(ids);
            criteria.andNotIn("id", idList);
        }

        //查询正在活动的秒杀商品列表
        List<TbSeckillGoods> goodsList = seckillGoodsMapper.selectByExample(example);
        if (goodsList != null && goodsList.size() > 0) {
            //把数据存入缓存
            for (TbSeckillGoods goods : goodsList) {
                System.out.println("秒杀商品加入了缓存，id为：" + goods.getId());
                //构建秒杀商品缓存库
                redisTemplate.boundHashOps("seckillGoods").put(goods.getId(), goods);

                //构建库存的缓存库
                //increment(操作的key,操作的值(可以是负数))
                redisTemplate.boundHashOps("seckillGoodsStockCount").increment(goods.getId(), goods.getStockCount());
            }
        } else {
            System.out.println("本次定时任务，没有新的商品加入缓存...");
        }

    }
}
