package com.pinyougou.cart.controller;


import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.Utils.CookieUtil;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.entity.Result;
import com.pinyougou.pojogroup.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("cart")
public class CartController {

    @Reference
    private CartService cartService;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private HttpServletResponse response;

    //获取购物车列表
    @RequestMapping("findCartList")
    private List<Cart> findCartList() {
        //获取登陆名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //无论登陆与否,都需要读取cookie中的未登陆的购物车数据
        System.out.println("从cookies中读取购物车...");
        String cartListStr = CookieUtil.getCookieValue(request, "cartList", true);
        List<Cart> cartList = new ArrayList<>();
        if (StringUtils.isNotEmpty(cartListStr)) {
            //把json串转成list
            cartList = JSON.parseArray(cartListStr, Cart.class);
        }
        //未登录,上面已经获得cookie中的购物车数据,无须再操作
        if ("anonymousUser".equals(username)) {
            System.out.println("未登陆状态噢");
        } else {
            System.out.println("从Redis中读取了购物车数据....");
            //登录版本购物车查询方案....查询redis
            List<Cart> cartListFromRedis = cartService.findCartListFromRedis(username);
            //如果cookie中存在购物车数据
            if (cartList.size() > 0) {
                System.out.println("开始合并购物车....");
                //开始合并........合并后，要把合并的结果返回，并接收作为返回值
                cartList = cartService.mergeCartList(cartList, cartListFromRedis);
                //保存合并后的结果，保存到redis中
                cartService.saveCartListToRedis(username, cartList);
                //清除cookie中的购物车数据
                CookieUtil.deleteCookie(request, response, "cartList");
            } else {
                cartList = cartListFromRedis;
            }
        }
        return cartList;
    }

    //修改购物车
    @RequestMapping("addGoodsToCartList")
    @CrossOrigin(origins = "http://localhost:8085",allowCredentials = "true")
    public Result addGoodsToCartList(Long itemId, Integer num) {
        try {
          /*  //设置可以访问的域，值设置为*时，允许所有域
            response.setHeader("Access-Control-Allow-Origin", "http://localhost:8085");
           //如果需要操作cookies，必须加上此配置，标识服务端可以写cookies，
           // 并且Access-Control-Allow-Origin不能设置为*，因为cookies操作需要域名
            response.setHeader("Access-Control-Allow-Credentials", "true");*/

            //获取登陆名
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            //获得原有购物车
            List<Cart> carts = findCartList();
            //得到修改后的购物车
            List<Cart> cartList = cartService.addGoodsToCartList(carts, itemId, num);
            if ("anonymousUser".equals(username)) {
                //未登录则保存到cookie,将购物车存入cookie  存一天
                System.out.println("保存cookie中的购物车...");
                CookieUtil.setCookie(request, response, "cartList", JSON.toJSONString(cartList), 3600 * 24, true);
            } else {
                //已登录则保存在redis,并清除cookie
                System.out.println("保存redis中的购物车...");
                cartService.saveCartListToRedis(username, cartList);
            }
            return new Result(true, "保存购物车成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "保存购物车失败");
        }
    }


}
