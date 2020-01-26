package com.pinyougou.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.entity.MessageInfo;
import com.pinyougou.entity.PageResult;
import com.pinyougou.entity.Result;
import com.pinyougou.manager.utils.MessageSender;
import com.pinyougou.pojo.TbSeckillGoods;
import com.pinyougou.seckill.service.SeckillGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckillGoods")
public class SeckillGoodsController {

    @Reference
    private SeckillGoodsService seckillGoodsService;

    /**
     * 分页查询数据
     * @return
     */
    @RequestMapping("/findPage")
    public PageResult findPage(int pageNo, int pageSize, @RequestBody TbSeckillGoods tbSeckillGoods){
        return seckillGoodsService.findPage(pageNo, pageSize,tbSeckillGoods);
    }

    /**
     * 跟据id列表，更新状态
     * @param ids
     * @param status
     * @return
     */
    @Autowired
    private MessageSender messageSender;
    @RequestMapping("updateStatus")
    public Result updateStatus(Long[] ids, String status){
        try {
            seckillGoodsService.updateStatus(ids, status);

            //如果是审核通过
            if("1".equals(status)) {
                //发送MQ消息
                MessageInfo info = new MessageInfo(
                        MessageInfo.METHOD_ADD,  //指定增加详情标识
                        ids,  //内容
                        "topic-seckill-goods",
                        "tag- seckill-goods-add",
                        "key- seckill-goods-add"
                );
                messageSender.sendMessage(info);
            }
                return new Result(true,"操作成功!");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(true,"操作失败!");
        }
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @RequestMapping("/delete")
    public Result delete(Long [] ids){
        try {
            seckillGoodsService.delete(ids);
            //发送MQ消息到RocketMQ中-新增操作
            MessageInfo info = new MessageInfo(
                    MessageInfo.METHOD_DELETE,  //操作业务方式-删除
                    ids, //发送内容
                    "topic-seckill-goods",
                    "tag- seckill-goods-add",
                    "key- seckill-goods-add"
            );
            messageSender.sendMessage(info);
            return new Result(true, "删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "删除失败");
        }
    }

}
