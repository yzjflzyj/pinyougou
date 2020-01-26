package com.pinyougou.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.abel533.entity.Example;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.pinyougou.Utils.IdWorker;
import com.pinyougou.entity.PageResult;
import com.pinyougou.mapper.TbOrderItemMapper;
import com.pinyougou.mapper.TbOrderMapper;
import com.pinyougou.mapper.TbPayLogMapper;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pojo.TbOrder;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.TbPayLog;
import com.pinyougou.pojogroup.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * 业务逻辑实现
 *
 * @author Steven
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private TbOrderMapper orderMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbOrder> findAll() {
        return orderMapper.select(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize, TbOrder order) {
        PageResult<TbOrder> result = new PageResult<TbOrder>();
        //设置分页条件
        PageHelper.startPage(pageNum, pageSize);

        //构建查询条件
        Example example = new Example(TbOrder.class);
        Example.Criteria criteria = example.createCriteria();

        if (order != null) {
            //如果字段不为空
            if (order.getPaymentType() != null && order.getPaymentType().length() > 0) {
                criteria.andLike("paymentType", "%" + order.getPaymentType() + "%");
            }
            //如果字段不为空
            if (order.getPostFee() != null && order.getPostFee().length() > 0) {
                criteria.andLike("postFee", "%" + order.getPostFee() + "%");
            }
            //如果字段不为空
            if (order.getStatus() != null && order.getStatus().length() > 0) {
                criteria.andLike("status", "%" + order.getStatus() + "%");
            }
            //如果字段不为空
            if (order.getShippingName() != null && order.getShippingName().length() > 0) {
                criteria.andLike("shippingName", "%" + order.getShippingName() + "%");
            }
            //如果字段不为空
            if (order.getShippingCode() != null && order.getShippingCode().length() > 0) {
                criteria.andLike("shippingCode", "%" + order.getShippingCode() + "%");
            }
            //如果字段不为空
            if (order.getUserId() != null && order.getUserId().length() > 0) {
                criteria.andLike("userId", "%" + order.getUserId() + "%");
            }
            //如果字段不为空
            if (order.getBuyerMessage() != null && order.getBuyerMessage().length() > 0) {
                criteria.andLike("buyerMessage", "%" + order.getBuyerMessage() + "%");
            }
            //如果字段不为空
            if (order.getBuyerNick() != null && order.getBuyerNick().length() > 0) {
                criteria.andLike("buyerNick", "%" + order.getBuyerNick() + "%");
            }
            //如果字段不为空
            if (order.getBuyerRate() != null && order.getBuyerRate().length() > 0) {
                criteria.andLike("buyerRate", "%" + order.getBuyerRate() + "%");
            }
            //如果字段不为空
            if (order.getReceiverAreaName() != null && order.getReceiverAreaName().length() > 0) {
                criteria.andLike("receiverAreaName", "%" + order.getReceiverAreaName() + "%");
            }
            //如果字段不为空
            if (order.getReceiverMobile() != null && order.getReceiverMobile().length() > 0) {
                criteria.andLike("receiverMobile", "%" + order.getReceiverMobile() + "%");
            }
            //如果字段不为空
            if (order.getReceiverZipCode() != null && order.getReceiverZipCode().length() > 0) {
                criteria.andLike("receiverZipCode", "%" + order.getReceiverZipCode() + "%");
            }
            //如果字段不为空
            if (order.getReceiver() != null && order.getReceiver().length() > 0) {
                criteria.andLike("receiver", "%" + order.getReceiver() + "%");
            }
            //如果字段不为空
            if (order.getInvoiceType() != null && order.getInvoiceType().length() > 0) {
                criteria.andLike("invoiceType", "%" + order.getInvoiceType() + "%");
            }
            //如果字段不为空
            if (order.getSourceType() != null && order.getSourceType().length() > 0) {
                criteria.andLike("sourceType", "%" + order.getSourceType() + "%");
            }
            //如果字段不为空
            if (order.getSellerId() != null && order.getSellerId().length() > 0) {
                criteria.andLike("sellerId", "%" + order.getSellerId() + "%");
            }

        }

        //查询数据
        List<TbOrder> list = orderMapper.selectByExample(example);
        //返回数据列表
        result.setRows(list);

        //获取总页数
        PageInfo<TbOrder> info = new PageInfo<TbOrder>(list);
        result.setPages(info.getPages());

        return result;
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private TbOrderItemMapper orderItemMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private TbPayLogMapper payLogMapper;

    /**
     * 增加
     */
    @Override
    public void add(TbOrder order) {
        //1、从redis中查询购物车数据
        List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(order.getUserId());
        List<String> orderIdList = new ArrayList();//订单ID列表
        double totalMoney = 0.0;//总金额 （元）
        //2、循环读取购物车数据，保存订单
        for (Cart cart : cartList) {
            long orderId = idWorker.nextId();
            TbOrder tbOrder = new TbOrder();//新创建订单对象
            tbOrder.setOrderId(orderId);//订单ID
            tbOrder.setUserId(order.getUserId());//用户名
            tbOrder.setPaymentType(order.getPaymentType());//支付类型
            tbOrder.setStatus("1");//状态：未付款
            tbOrder.setCreateTime(new Date());//订单创建日期
            tbOrder.setUpdateTime(tbOrder.getCreateTime());//订单更新日期
            tbOrder.setReceiverAreaName(order.getReceiverAreaName());//地址
            tbOrder.setReceiverMobile(order.getReceiverMobile());//手机号
            tbOrder.setReceiver(order.getReceiver());//收货人
            tbOrder.setSourceType(order.getSourceType());//订单来源
            tbOrder.setSellerId(cart.getSellerId());//商家ID
            //循环购物车明细
            double money = 0;
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                orderItem.setId(idWorker.nextId());
                orderItem.setOrderId(orderId);//订单ID
                orderItem.setSellerId(cart.getSellerId());
                money += orderItem.getTotalFee().doubleValue();//金额累加
                orderItemMapper.insertSelective(orderItem);
            }
            tbOrder.setPayment(new BigDecimal(money));
            orderMapper.insertSelective(tbOrder);
            orderIdList.add(orderId + "");//保存到订单Id列表
            totalMoney += money; //一个购物车为一个订单,累加到总金额.计算结算是的所有支付订单的总和

        }

        //如果是微信支付,将支付日志保存在数据库和缓存中
        if ("1".equals(order.getPaymentType())) {
            TbPayLog payLog = new TbPayLog();
            String outTradeNo = idWorker.nextId() + "";//支付订单号
            payLog.setOutTradeNo(outTradeNo);//支付订单号
            payLog.setCreateTime(new Date());//创建时间
            //订单号列表，逗号分隔
            String ids = orderIdList.toString().replace("[", "").replace("]", "").replace(" ", ",");
            payLog.setOrderList(ids);//订单号列表，逗号分隔
            payLog.setPayType("1");//支付类型
            payLog.setTotalFee((long) (totalMoney * 100));//总金额(分)
            payLog.setTradeState("0");//支付状态,未支付状态
            payLog.setUserId(order.getUserId());//用户ID
            payLogMapper.insertSelective(payLog);//插入到支付日志表
            //PayLog放入缓存
            redisTemplate.boundHashOps("payLogs").put(order.getUserId(), payLog);
        }

        //3、清除redis中当前用户的购物车数据
        redisTemplate.boundHashOps("cartList").delete(order.getUserId());
    }


    //根据登陆id从缓存中读取支付日志的信息
    @Override
    public TbPayLog searchPayLogFromRedis(String userId) {
        return (TbPayLog) redisTemplate.boundHashOps("payLogs").get(userId);
    }


    /**
     * 调用更新订单状态与支付日志状态
     * @param out_trade_no 支付订单号
     * @param transaction_id 微信支付订单号
     */
    @Override
    public void updateOrderStatus(String out_trade_no, String transaction_id) {

        //1. 修改支付日志状态
        TbPayLog payLog = payLogMapper.selectByPrimaryKey(out_trade_no);
        payLog.setPayTime(new Date());
        payLog.setTradeState("1");//已支付
        payLog.setTransactionId(transaction_id);//交易号
        payLogMapper.updateByPrimaryKey(payLog);
        //2. 修改关联的订单的状态
        String orderList = payLog.getOrderList();//获取订单号列表
        String[] orderIds = orderList.split(",");//获取订单号数组
        for (String orderId : orderIds) {
            TbOrder order = new TbOrder();
            order.setOrderId(new Long(orderId));
            order.setStatus("2");//已付款
            orderMapper.updateByPrimaryKey(order);
        }
        //3. 清除缓存中的支付日志对象
        redisTemplate.boundHashOps("payLog").delete(payLog.getUserId());
    }


    /**
     * 修改
     */
    @Override
    public void update(TbOrder order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbOrder getById(Long id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        //数组转list
        List longs = Arrays.asList(ids);
        //构建查询条件
        Example example = new Example(TbOrder.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", longs);

        //跟据查询条件删除数据
        orderMapper.deleteByExample(example);
    }


}
