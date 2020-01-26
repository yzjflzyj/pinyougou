package com.pinyougou.seckill.service;

import com.pinyougou.entity.PageResult;
import com.pinyougou.entity.QueueTag;
import com.pinyougou.pojo.TbSeckillOrder;

import java.util.List;


/**
 * 业务逻辑接口
 * @author Steven
 *
 */
public interface SeckillOrderService {

	/**
	 * 返回全部列表
	 * @return
	 */
	public List<TbSeckillOrder> findAll();
	
	
	/**
     * 分页查询列表
     * @return
     */
    public PageResult<TbSeckillOrder> findPage(int pageNum, int pageSize, TbSeckillOrder seckillOrder);
	
	
	/**
	 * 增加
	*/
	public void add(TbSeckillOrder seckillOrder);
	
	
	/**
	 * 修改
	 */
	public void update(TbSeckillOrder seckillOrder);
	

	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	public TbSeckillOrder getById(Long id);
	
	
	/**
	 * 批量删除
	 * @param ids
	 */
	public void delete(Long[] ids);


	/**
	 * 立即抢购
	 * @param seckillId 抢购商品id
	 * @param userId 下单用户
	 */
	public void submitOrder(Long seckillId,String userId);


	/**
	 * 根据商品id与用户id查询排队状态
	 * @param seckillId 商品id
	 * @param userId 用户id
	 * @return 排队标识
	 */
	public QueueTag getQueueStatus(Long seckillId, String userId);


	/**
	 * 根据用户名查询秒杀订单
	 * @param userId
	 */
	public TbSeckillOrder searchOrderFromRedisByUserId(String userId);


	/**
	 * 支付成功保存订单
	 * @param userId 用户id
	 * @param transactionId 流水号
	 */
	public void saveOrderFromRedisToDb(String userId, String transactionId);

	/**
	 * 从缓存中删除订单并还原库存
	 * @param userId  用户id
	 * @param orderId 订单id
	 */
	public void deleteOrderFromRedis(String userId,Long orderId);



}
