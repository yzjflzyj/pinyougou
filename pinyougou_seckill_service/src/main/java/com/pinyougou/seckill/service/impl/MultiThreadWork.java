package com.pinyougou.seckill.service.impl;

import com.pinyougou.Utils.IdWorker;
import com.pinyougou.entity.QueueTag;
import com.pinyougou.mapper.TbSeckillGoodsMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.pojo.TbSeckillOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

//多线程入门demo
@Component
@Transactional
public class MultiThreadWork {

    //加入此注解标识以下方法是开启新线程执行的-异步
    @Async
    public void doSomeThing(int i) {
        try {
            System.out.println(i + "我正在处理些事..." + new Date());
            Thread.sleep(1000);
            System.out.println(i + "事情处理完了，我很开心..." + new Date());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private IdWorker idWorker;
    //redis事物
    @Autowired
    private RedisTemplate transRedisTemplate;

    /**
     * 多线程下单方法
     *
     * @param seckillId 抢购商品id
     */
    @Async
    public void createOrder(Long seckillId) {
        try {
            System.out.println("模拟当前业务处理时间较长，这里用时5秒...");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //1.先从redis队列信息中取出当前商品排在最前面的人-右取
        String userId = (String) redisTemplate.boundListOps("seckill_goods_order_queue_" + seckillId).rightPop();
        //2.扣减库存
        Long count = redisTemplate.boundHashOps("seckillGoodsStockCount").increment(seckillId, -1);
        try {
            //开启Redis事务
            transRedisTemplate.multi();
            if (count < 0) {
                //库存不足时，记录排队标识
                redisTemplate.boundHashOps("user_order_info_" + userId).put(seckillId, QueueTag.NO_STOCK);
                throw new RuntimeException("你来晚了一步，商品已抢购一空!");
            }
            //3.从redis中查询商品
            TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps("seckillGoods").get(seckillId);

            //扣减库存-页面展示用的
            //seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
            seckillGoods.setStockCount(count.intValue());
            redisTemplate.boundHashOps("seckillGoods").put(seckillId, seckillGoods);
            //如果库存不足了
            //高并发场景中，good.StockCount可能不准，此时seckillGoodsStockCount一定是准确的,所以以seckillGoodsCount为标准
            if (count == 0) {
                seckillGoods.setStockCount(0);
                //更新到数据库
                seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
                //从redis中删除相关商品
                redisTemplate.boundHashOps("seckillGoods").delete(seckillId);
            }
            //在支付之前，保存订单到redis
            long orderId = idWorker.nextId();
            TbSeckillOrder seckillOrder = new TbSeckillOrder();
            seckillOrder.setId(orderId);
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setMoney(seckillGoods.getCostPrice());//秒杀价格
            seckillOrder.setSeckillId(seckillId);
            seckillOrder.setSellerId(seckillGoods.getSellerId());
            seckillOrder.setUserId(userId);//设置用户ID
            seckillOrder.setStatus("0");//状态
            //保存订单到redis
            redisTemplate.boundHashOps("seckillOrder").put(userId, seckillOrder);

            //最后，一定要记得，下单后，把redis中的排队标识改为已下单
            redisTemplate.boundHashOps("user_order_info_" + userId).put(seckillId, QueueTag.CREATE_ORDER);

            System.out.println("用户:" + userId + ",抢购商品id:" + seckillId + "，成功！");

            //模拟业务异常
          /*  int i = 1 / 0;*/
            //提交redis事务
            transRedisTemplate.exec();
        } catch (Exception e) {
            //回滚事务
            transRedisTemplate.discard();
            //抢购失败，回滚库存
            redisTemplate.boundHashOps("seckillGoodsStockCount").increment(seckillId, 1);
            //标识抢购时，秒杀失败
            redisTemplate.boundHashOps("user_order_info_" + userId).put(seckillId, QueueTag.SECKILL_FAIL);
            //如果刚好在最后一个报异常，要把mysql数据库的数据也还原
            if (count == 0) {
                //这里一定要把异常抛出去，让spring捕获到异常把mysql回滚
                throw new RuntimeException(e);
            }
        }

    }


}
