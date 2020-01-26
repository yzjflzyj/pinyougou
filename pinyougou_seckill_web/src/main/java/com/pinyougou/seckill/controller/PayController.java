package com.pinyougou.seckill.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.entity.Result;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.pojo.TbSeckillOrder;
import com.pinyougou.seckill.service.SeckillOrderService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("pay")
public class PayController {

    @Reference
    private WeixinPayService weixinPayService;
    @Reference
    private SeckillOrderService seckillOrderService;
    /**
     * 生成二维码
     * @return
     */
    @RequestMapping("/createNative")
    public Map
    createNative() {
        //获取当前用户
        String userId= SecurityContextHolder.getContext().getAuthentication().getName();
        //到redis查询支付日志
        TbSeckillOrder seckillOrder = seckillOrderService.searchOrderFromRedisByUserId(userId);
        //判断支付日志存在
        if(seckillOrder!=null){
            //金额（分）
            String fen =(long)(seckillOrder.getMoney().doubleValue() * 100) + "";
            //统一下单
            return weixinPayService.createNative(seckillOrder.getId() + "",fen);
        }else{
            return new HashMap();
        }
    }


    /**
     * 查询支付状态
     * @param out_trade_no
     * @return
     */
    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no) {
        String userId= SecurityContextHolder.getContext().getAuthentication().getName();
        Result result = null;
        int i = 1;
        while (true) {
            //调用查询接口
            Map<String, String> map = weixinPayService.queryPayStatus(out_trade_no);
            if (map == null) {//出错
                result = new Result(false, "支付出错");
                break;
            }
            if (map.get("trade_state").equals("SUCCESS")) {//如果成功
                result = new Result(true, "支付成功");
                //创建抢购订单到数据库
                seckillOrderService.saveOrderFromRedisToDb(userId, map.get("transaction_id"));
                break;
            }
            try {
                Thread.sleep(3000);//间隔三秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //为了不让循环无休止地运行，我们定义一个循环变量，
            // 如果这个变量超过了这个值则退出循环，设置时间为5分钟
            i++;
            //300秒为5分钟，3秒执行一次，所以i >= 100约5分钟
            if (i >= 100) {
                //1.调用微信的关闭订单接口
                Map<String,String> payresult = weixinPayService.closePay(out_trade_no);
                if( !"SUCCESS".equals(payresult.get("result_code")) ){//如果返回结果是正常关闭
                    //如果订单已被支付
                    if("ORDERPAID".equals(payresult.get("err_code"))){
                        result=new Result(true, "支付成功");
                        //正常发货
                        seckillOrderService.saveOrderFromRedisToDb(userId, map.get("transaction_id"));
                    }
                }
                if(result.isSuccess()==false){
                    System.out.println("超时，取消订单");
                    //2.清除缓存
                    seckillOrderService.deleteOrderFromRedis(userId, new Long(out_trade_no));
                }

                result = new Result(false, "支付超时");
                break;
            }
        }

        return result;
    }



}
