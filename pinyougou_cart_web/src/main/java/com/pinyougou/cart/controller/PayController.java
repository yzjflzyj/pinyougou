package com.pinyougou.cart.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.entity.Result;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.pojo.TbPayLog;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PayController {

    @Reference
    private WeixinPayService weixinPayService;
    @Reference
    private OrderService orderService;

    /**
     * 生成二维码
     * @return
     */
    @RequestMapping("/createNative")
    public Map createNative(){
            //获取当前用户,从缓存中拿到日志对象
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            TbPayLog tbPayLog = orderService.searchPayLogFromRedis(userId);
            //购物车订单是long,而参数需要string
            return weixinPayService.createNative(tbPayLog.getOutTradeNo(), tbPayLog.getTotalFee() + "");
    }

    /**
     * 查询支付状态
     * @param out_trade_no
     * @return
     */
    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no){
        Result result = null;
        //限制5分钟内完成支付
        int i = 0;
        while (true){
            i++;
            //超过5分钟没支付 = 60 * 5 / 3 = 100
            if(i > 4){
                result = new Result(false,"支付超时");
                break;
            }
            Map<String,String> resultMap = weixinPayService.queryPayStatus(out_trade_no);
            if(resultMap == null){
                result = new Result(false,"支付失败！");
                break;
            }
            //已支付
            if ("SUCCESS".equals(resultMap.get("trade_state"))) {
                result = new Result(true,"支付成功");

                //调用更新订单状态与支付日志状态
                orderService.updateOrderStatus(out_trade_no,resultMap.get("transaction_id"));
                break;
            }
            try {
                //注意我们这里不能太快去调用，微信后台的防御，会拉黑你,合理的话3秒左右查询一次
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


}