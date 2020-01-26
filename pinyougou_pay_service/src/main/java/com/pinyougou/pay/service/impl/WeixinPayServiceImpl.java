package com.pinyougou.pay.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.WXPayUtil;
import com.pinyougou.Utils.HttpClient;
import com.pinyougou.pay.service.WeixinPayService;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Service
public class WeixinPayServiceImpl implements WeixinPayService {

    @Value("${appid}")
    private String appid;
    @Value("${partner}")
    private String partner;
    @Value("${notifyurl}")
    private String notifyurl;
    @Value("${partnerkey}")
    private String partnerkey;
    @Value("${PayUrl}")
    private String PayUrl;
    @Value("${QueryUrl}")
    private String QueryUrl;
    @Value("${closeUrl}")
    private String closeUrl;


    /**
     * 生成微信支付二维码
     *
     * @param out_trade_no 支付单号
     * @param total_fee    金额(分)
     * @return
     */
    @Override
    public Map createNative(String out_trade_no, String total_fee) {
        Map resultMap = new HashMap();
        try {
            //1、包装微信接口需要的参数
            Map paramMap = new HashMap();
            paramMap.put("appid", appid);  //公众号id
            paramMap.put("mch_id", partner);  //商户号
            //生成随机字符串
            String nonce_str = WXPayUtil.generateNonceStr();
            paramMap.put("nonce_str", nonce_str);  //随机字符串
            paramMap.put("body", "品优购(*^_^*)");  //商品描述-用户扫码后看见的商品信息
            paramMap.put("out_trade_no", out_trade_no);  //商户订单号
            paramMap.put("total_fee", total_fee);  //支付金额-单位是分，类型是int
            paramMap.put("spbill_create_ip", "127.0.0.1");  //微信支付终端ip-一般我们是通过request来获取
            paramMap.put("notify_url", notifyurl);  //通知地址,回调地址
            paramMap.put("trade_type", "NATIVE");  //交易类型
            //sign-签名，我们一般不会直接设置，后续有专用api生成
            //map转换xml格式的参数
            String paramXml = WXPayUtil.generateSignedXml(paramMap, partnerkey);
            System.out.println("正在发起统一下单接口，请求参数为：" + paramXml);
            //2、生成xml，通过httpClient发送请求得到数据
            HttpClient client = new HttpClient(PayUrl);
            client.setHttps(true);//是否是https协议
            client.setXmlParam(paramXml);//发送xml数据格式的参数
            client.post();//执行post请求
            String xmlResult = client.getContent(); //获取结果
            //3、解析结果,将xml格式的返回数据再封装为map
            Map<String, String> map  = WXPayUtil.xmlToMap(xmlResult);

            resultMap.put("code_url", map.get("code_url"));//支付地址
            resultMap.put("total_fee", total_fee);//总金额
            resultMap.put("out_trade_no",out_trade_no);//订单号

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultMap;
    }


    /**
     * 查询支付状态
     * @param out_trade_no 支付单号
     */
    @Override
    public Map queryPayStatus(String out_trade_no) {
        try {
            //1、组装请求参数
            Map paramMap = new HashMap();
            paramMap.put("appid", appid);  //公众号id
            paramMap.put("mch_id", partner);  //商户号
            paramMap.put("out_trade_no", out_trade_no);  //商户订单号
            //生成随机字符串
            String nonce_str = WXPayUtil.generateNonceStr();
            paramMap.put("nonce_str", nonce_str);  //随机字符串
            //sign-签名，我们一般不会直接设置，后续有专用api生成
            String paramXml = WXPayUtil.generateSignedXml(paramMap, partnerkey);
            System.out.println("正在发起查询订单接口，请求参数为：" + paramXml);
            //2、通过HttpClient发起微信统一下单请求
            HttpClient httpClient = new HttpClient(QueryUrl);
            httpClient.setHttps(true);  //使用加密协议
            //设置方法入参
            httpClient.setXmlParam(paramXml);
            httpClient.post();   //发起psot请求
            String content = httpClient.getContent();//读取内容
            System.out.println("发起查询订单接口成功，响应参数为：" + content);
            //解释内容:把xml转成Map对象
            Map<String, String> resultMap = WXPayUtil.xmlToMap(content);
            //3、读取与接收结果,组装返回
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 关闭支付
     * @param out_trade_no
     * @return
     */
    @Override
    public Map closePay(String out_trade_no) {
        try {
            //1、包装微信接口需要的参数
            Map param = new HashMap();
            param.put("appid", appid);  //公众号ID
            param.put("mch_id", partner);  //商户号
            param.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
            param.put("out_trade_no", out_trade_no); //订单号
            //签名
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            System.out.println("请求参数:" + xmlParam);
            //2、生成xml，通过httpClient发送请求得到数据
            HttpClient httpClient = new HttpClient(closeUrl);
            httpClient.setHttps(true);
            httpClient.setXmlParam(xmlParam);
            httpClient.post();
            //3、解析结果
            String xmlResult = httpClient.getContent();
            System.out.println("微信返回结果：" + xmlResult);
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xmlResult);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
