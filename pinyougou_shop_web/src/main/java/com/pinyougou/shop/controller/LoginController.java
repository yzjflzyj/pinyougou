package com.pinyougou.shop.controller;


import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {

    @RequestMapping("/info")
    public Map<String,Object> name(){

        //通过spring security框架中提供SecurityContextHolder来获取用户对象
        //SecurityContext 安全容器对象
        // 通过容器安全对象 获取 Authentication认证 ,
        // 最终通过Authentication认证获取用户对象principal(getPrincipal()方法获得对象,再getUsername()获得用户名,也可以直接获得用户名)
        //无法获得密码,但是shiro可以
        String name=SecurityContextHolder.getContext().getAuthentication().getName();

        Map<String, Object> map=new HashMap<>();
        map.put("loginName", name);
        return map;
    }
}
