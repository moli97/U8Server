package com.u8.server.test;

import com.u8.server.cache.UApplicationContext;
import com.u8.server.data.UChannel;
import com.u8.server.log.Log;
import com.u8.server.sdk.UHttpAgent;
import com.u8.server.utils.EncryptUtils;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ant on 2016/1/18.
 */
public class CallbackTest {

    public static void main(String[] args){


        //testOuWanPayCallback();
        //testYouKuPayCallback();
        testKuGouPayCallback();
    }

    private static void testKuGouPayCallback(){

        String url = "http://localhost:8080/pay/kugou/payCallback/39";

        Map<String,String> params = new HashMap<>();
        params.put("orderid","65489839570");
        params.put("outorderid","654898395705507841");
        params.put("amount","1");
        params.put("username","usesfrd");
        params.put("status","1");
        params.put("time","23453435");
        params.put("ext1","");
        params.put("ext2","");

        StringBuilder sb = new StringBuilder();
        sb.append("65489839570").append("654898395705507841")
                .append("1").append("usesfrd")
                .append("1").append("23453435")
                .append("").append("")
                .append("CFxI2KQeiL32S79HCcATlru5Ls6M4HLI");

        Log.d("sign txt:" + sb.toString());


        String md5 = EncryptUtils.md5(sb.toString()).toLowerCase();

        Log.d("md5:"+md5);

        params.put("sign", md5);

        String result = UHttpAgent.newInstance().get(url, params);

        System.out.println("the result is "+result);
    }

    private static void testYouKuPayCallback(){

        String url = "http://localhost:8080/pay/youku/payCallback/38";

        Map<String,String> params = new HashMap<>();
        params.put("apporderID", "654898395705507841");
        params.put("uid", "11111");
        params.put("price", "345");
        params.put("passthrough", "111");

        StringBuilder sb = new StringBuilder();
        sb.append(url).append("?apporderID=").append("654898395705507841")
                .append("&price=").append("345").append("&uid=").append("11111");

        String sign = EncryptUtils.hmac(sb.toString(), "cfb56ead9fd3b1fc30c0c18bad4a1a84");

        params.put("sign", sign);

        String result = UHttpAgent.newInstance().post(url, params);

        System.out.println("the result is "+result);
    }

    private static void testOuWanPayCallback(){

        String url = "http://localhost:8080/pay/ouwan/payCallback/33";
        String appSecret = "c3657cdd837eb9dd";

        Map<String,String> params = new HashMap<>();
        params.put("serverId", "1");
        params.put("callbackInfo", "654898395705507841");
        params.put("openId", "34534535345");
        params.put("orderId", "23543245325435345");
        params.put("orderStatus", "1");
        params.put("payType", "alipay");
        params.put("amount", "100.00");
        params.put("remark", "ddd");

        StringBuilder sb = new StringBuilder();
        sb.append("amount=").append("100.00")
                .append("callbackinfo=").append("654898395705507841")
                .append("openId=").append("34534535345")
                .append("orderId=").append("23543245325435345")
                .append("orderStatus=").append("1")
                .append("payType=").append("alipay")
                .append("remark=").append("ddd")
                .append("serverId=").append("1")
                .append(appSecret);

        String md5 = EncryptUtils.md5(sb.toString()).toLowerCase();

        params.put("sign", md5);

        String result = UHttpAgent.newInstance().get(url, params);

        System.out.println("the result is "+result);
    }
}
