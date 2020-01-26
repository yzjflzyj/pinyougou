package com.pinyougou.seckill.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.abel533.entity.Example;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.Utils.IdWorker;
import com.pinyougou.entity.PageResult;
import com.pinyougou.entity.QueueTag;
import com.pinyougou.mapper.TbSeckillGoodsMapper;
import com.pinyougou.mapper.TbSeckillOrderMapper;
import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.seckill.service.SeckillOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * 业务逻辑实现
 * @author Steven
 *
 */
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

	@Autowired
	private TbSeckillOrderMapper seckillOrderMapper;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbSeckillOrder> findAll() {
		return seckillOrderMapper.select(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize, TbSeckillOrder seckillOrder) {
		PageResult<TbSeckillOrder> result = new PageResult<TbSeckillOrder>();
        //设置分页条件
        PageHelper.startPage(pageNum, pageSize);

        //构建查询条件
        Example example = new Example(TbSeckillOrder.class);
        Example.Criteria criteria = example.createCriteria();
		
		if(seckillOrder!=null){			
						//如果字段不为空
			if (seckillOrder.getUserId()!=null && seckillOrder.getUserId().length()>0) {
				criteria.andLike("userId", "%" + seckillOrder.getUserId() + "%");
			}
			//如果字段不为空
			if (seckillOrder.getSellerId()!=null && seckillOrder.getSellerId().length()>0) {
				criteria.andLike("sellerId", "%" + seckillOrder.getSellerId() + "%");
			}
			//如果字段不为空
			if (seckillOrder.getStatus()!=null && seckillOrder.getStatus().length()>0) {
				criteria.andLike("status", "%" + seckillOrder.getStatus() + "%");
			}
			//如果字段不为空
			if (seckillOrder.getReceiverAddress()!=null && seckillOrder.getReceiverAddress().length()>0) {
				criteria.andLike("receiverAddress", "%" + seckillOrder.getReceiverAddress() + "%");
			}
			//如果字段不为空
			if (seckillOrder.getReceiverMobile()!=null && seckillOrder.getReceiverMobile().length()>0) {
				criteria.andLike("receiverMobile", "%" + seckillOrder.getReceiverMobile() + "%");
			}
			//如果字段不为空
			if (seckillOrder.getReceiver()!=null && seckillOrder.getReceiver().length()>0) {
				criteria.andLike("receiver", "%" + seckillOrder.getReceiver() + "%");
			}
			//如果字段不为空
			if (seckillOrder.getTransactionId()!=null && seckillOrder.getTransactionId().length()>0) {
				criteria.andLike("transactionId", "%" + seckillOrder.getTransactionId() + "%");
			}
	
		}

        //查询数据
        List<TbSeckillOrder> list = seckillOrderMapper.selectByExample(example);
        //返回数据列表
        result.setRows(list);

        //获取总页数
        PageInfo<TbSeckillOrder> info = new PageInfo<TbSeckillOrder>(list);
        result.setPages(info.getPages());
		
		return result;
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbSeckillOrder seckillOrder) {
		seckillOrderMapper.insertSelective(seckillOrder);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbSeckillOrder seckillOrder){
		seckillOrderMapper.updateByPrimaryKeySelective(seckillOrder);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbSeckillOrder getById(Long id){
		return seckillOrderMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		//数组转list
        List longs = Arrays.asList(ids);
        //构建查询条件
        Example example = new Example(TbSeckillOrder.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", longs);

        //跟据查询条件删除数据
        seckillOrderMapper.deleteByExample(example);
	}


	//秒杀下单
	@Autowired
	private RedisTemplate redisTemplate;
	@Autowired
	private TbSeckillGoodsMapper seckillGoodsMapper;
	@Autowired
	private IdWorker idWorker;
	@Autowired
	private MultiThreadWork multiThreadWork;

	@Override
	public void submitOrder(Long seckillId, String userId) {

		//获取用户的排队信息,用于防止重复下单
		QueueTag tag = (QueueTag) redisTemplate.boundHashOps("user_order_info_" + userId).get(seckillId);
		//如果存在未付款订单
		if(tag == QueueTag.IN_LINE || tag == QueueTag.CREATE_ORDER){
			throw new RuntimeException("当前商品，您已存在未付款订单，请先支付！");
		}
		//登记排队信息，左进右取，以商品id为key，用户id为value
		redisTemplate.boundListOps("seckill_goods_order_queue_" + seckillId).leftPush(userId);
		//记录用户已经排队的信息，redis中key=用户id：｛商品id:排队标识｝
		redisTemplate.boundHashOps("user_order_info_" + userId).put(seckillId, QueueTag.IN_LINE);

		//调用多线程下单
		multiThreadWork.createOrder(seckillId);
		/*//先扣减库存
		Long count = redisTemplate.boundHashOps("seckillGoodsStockCount").increment(seckillId, -1);
		if (count < 0) {
			throw new RuntimeException("你来晚了一步，商品已抢购一空!");
		}

		//从redis中查询商品
		TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps("seckillGoods").get(seckillId);

		*//*if (seckillGoods == null || seckillGoods.getStockCount() <= 0) {
			throw new RuntimeException("你来晚了一步，商品已抢购一空!");
		}*//*

		//秒杀商品缓存库,扣减库存
		seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
		redisTemplate.boundHashOps("seckillGoods").put(seckillId, seckillGoods);
		//如果库存不足了
		//高并发场景中，seckillGood.StockCount可能不准，此时seckillGoodsStockCount一定是准确的,所以以seckillGoodsCount为标准
		if(count  == 0){
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
		System.out.println(seckillOrder.toString());*/
	}


	/**
	 * 根据商品id与用户id查询排队状态
	 * @param seckillId 商品id
	 * @param userId 用户id
	 * @return 排队标识
	 */
	@Override
	public QueueTag getQueueStatus(Long seckillId, String userId) {
		QueueTag tag = (QueueTag) redisTemplate.boundHashOps("user_order_info_" + userId).get(seckillId);
		return tag;
	}

    //到redis查询支付订单
    @Override
    public TbSeckillOrder searchOrderFromRedisByUserId(String userId) {
        TbSeckillOrder seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
        return seckillOrder;
    }

    /**
     * 支付成功保存订单
     * @param userId 用户id
     * @param transactionId 流水号
     */
    @Override
    public void saveOrderFromRedisToDb(String userId, String transactionId) {
        TbSeckillOrder seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
        if (seckillOrder == null) {
            throw new RuntimeException("订单不存在");
        }
        seckillOrder.setTransactionId(transactionId);//交易流水号
        seckillOrder.setPayTime(new Date());//支付时间
        seckillOrder.setStatus("1");//状态
        //保存到数据库
        seckillOrderMapper.insertSelective(seckillOrder);
        //从redis中清除用户订单
        redisTemplate.boundHashOps("seckillOrder").delete(userId);
        //标识抢购，支付成功
        redisTemplate.boundHashOps("user_order_info_" + userId).put(seckillOrder.getSeckillId(), QueueTag.PAY_SUCCESS);
    }


	/**
	 * 从缓存中删除订单并还原库存
	 * @param userId  用户id
	 * @param orderId 订单id
	 */
	@Override
	public void deleteOrderFromRedis(String userId, Long orderId) {
		TbSeckillOrder seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps("seckillOrder").get(userId);
		if(seckillOrder != null){
			redisTemplate.boundHashOps("seckillOrder").delete(userId);//删除缓存中的订单
			//1.从缓存中提取秒杀商品
			TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps("seckillGoods").get(seckillOrder.getSeckillId());
			seckillGoods.setStockCount(seckillGoods.getStockCount() + 1);
			//恢复库存-页面展示
			redisTemplate.boundHashOps("seckillGoods").put(seckillOrder.getSeckillId(), seckillGoods);
			//恢复库存-下单时用的
			redisTemplate.boundHashOps("seckillGoodsStockCount").increment(seckillOrder.getSeckillId(), 1);
		}
	}


}
