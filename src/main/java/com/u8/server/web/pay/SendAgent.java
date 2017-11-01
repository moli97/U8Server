package com.u8.server.web.pay;

import com.u8.server.cache.UApplicationContext;
import com.u8.server.constants.PayState;
import com.u8.server.constants.StateCode;
import com.u8.server.data.UGame;
import com.u8.server.data.UOrder;
import com.u8.server.data.UUser;
import com.u8.server.log.Log;
import com.u8.server.sdk.UHttpAgent;
import com.u8.server.sdk.appchina.RSAUtil;
import com.u8.server.sdk.kuaiyong.kuaiyong.RSAEncrypt;
import com.u8.server.service.UGameManager;
import com.u8.server.service.UOrderManager;
import com.u8.server.service.UUserManager;
import com.u8.server.task.OrderTaskManager;
import com.u8.server.utils.EncryptUtils;
import com.u8.server.utils.RSAUtils;
import com.u8.server.utils.UGenerator;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ByteArrayEntity;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * U8Server向游戏服发送回调通知
 * Created by ant on 2015/2/9.
 */
public class SendAgent {

    public static final String SIGN_MD5 = "md5";
    public static final String SIGN_RSA = "rsa";

    public static final String CONTENT_TYPE_WWW = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_JSON = "application/json";

    private static String contentType = CONTENT_TYPE_WWW;
    private static String signType = SIGN_MD5;


    /**
     * U8Server支付成功，通知游戏服务器
     *
     * 协议格式，通过上面contentType指定；签名方式通过signType指定。
     *
     * @param orderManager
     * @param order
     * @return
     */
    public static boolean sendCallbackToServer(UOrderManager orderManager, UOrder order){


        if(CONTENT_TYPE_JSON.equals(contentType)){
            return sendCallbackToServerWithJSON(orderManager, order, signType);
        }else{
            return sendCallbackToServerWithWWW(orderManager, order, signType);
        }

    }

    /**
     * U8Server订单补发接口
     *
     * 协议格式，通过上面contentType指定；签名方式通过signType指定。
     *
     * @param orderManager
     * @param order
     * @return
     */
    public static boolean resendCallbackToServer(UOrderManager orderManager, UOrder order){


        if(CONTENT_TYPE_JSON.equals(contentType)){
            return resendCallbackToServerWithJSON(orderManager, order, signType);
        }else{
            return resendCallbackToServerWithWWW(orderManager, order, signType);
        }
    }


    /**
     * U8Server支付成功，通知游戏服务器
     * @param orderManager
     * @param order
     * @return
     */
    public static boolean sendCallbackToServerWithJSON(UOrderManager orderManager, UOrder order, String signType){

        UGameManager gameManager = (UGameManager)UApplicationContext.getBean("gameManager");

        UGame game = gameManager.queryGame(order.getAppID());
        if(game == null){
            return false;
        }

        String callbackUrl = order.getNotifyUrl();
        if(StringUtils.isEmpty(callbackUrl)){
            callbackUrl = game.getPayCallback();
        }

        if(StringUtils.isEmpty(callbackUrl)){

            Log.d("the order paycallback url is not configed. no in order. no in game.");
            return false;
        }

        try{


            JSONObject data = new JSONObject();
            data.put("productID", order.getProductID());
            data.put("orderID", order.getOrderID());
            data.put("userID", order.getUserID());
            data.put("channelID", order.getChannelID());
            data.put("gameID", order.getAppID());
            data.put("serverID", order.getServerID());
            data.put("roleID", order.getRoleID());          //不参与签名
            data.put("money", order.getMoney());
            data.put("currency", order.getCurrency());
            data.put("extension", order.getExtension());

            //如果需要将签名方式改为MD5，把下面两行SIGN_RSA改为SIGN_MD5
            String sign = generateSign(order, signType, game.getAppSecret(), game.getAppRSAPriKey());
            data.put("signType", signType);
            data.put("sign", sign);

            JSONObject response = new JSONObject();
            response.put("state", StateCode.CODE_SUCCESS);
            response.put("data", data);



            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json");

            Log.d("callback game %s server url:%s", order.getAppID(), callbackUrl);
            String serverRes = UHttpAgent.getInstance().post(callbackUrl, headers, new ByteArrayEntity(response.toString().getBytes(Charset.forName("UTF-8"))));

            Log.d("game server returned:"+serverRes);

            if("SUCCESS".equals(serverRes)){
                order.setState(PayState.STATE_COMPLETE);
                order.setCompleteTime(new Date());
                orderManager.saveOrder(order);

                return true;
            }

        }catch (Exception e){
            Log.e(e.getMessage(), e);
            e.printStackTrace();
        }

        //失败了，加入重发队列，尝试6次
        OrderTaskManager.getInstance().addOrder(order);


        return false;
    }


    /**
     * U8Server支付成功，通知游戏服务器
     * @param orderManager
     * @param order
     * @return
     */
    public static boolean sendCallbackToServerWithWWW(UOrderManager orderManager, UOrder order, String signType){

        UGameManager gameManager = (UGameManager)UApplicationContext.getBean("gameManager");

        UGame game = gameManager.queryGame(order.getAppID());
        if(game == null){
            return false;
        }


        String callbackUrl = order.getNotifyUrl();
        if(StringUtils.isEmpty(callbackUrl)){
            callbackUrl = game.getPayCallback();
        }

        if(StringUtils.isEmpty(callbackUrl)){

            Log.d("the order paycallback url is not configed. no in order. no in game.");
            return false;
        }

        try{


            //JSONObject data = new JSONObject();
            Map<String,String> data = new HashMap<String,String>();
            data.put("productID", order.getProductID());
            data.put("orderID", order.getOrderID()+"");
            data.put("userID", order.getUserID()+"");
            data.put("channelID", order.getChannelID()+"");
            data.put("gameID", order.getAppID()+"");
            data.put("serverID", order.getServerID());
            data.put("roleID", order.getRoleID());          //不参与签名
            data.put("money", order.getMoney()+"");
            data.put("currency", order.getCurrency());
            data.put("extension", order.getExtension());

            //如果需要将签名方式改为MD5，把下面两行SIGN_RSA改为SIGN_MD5
            String sign = generateSign(order, signType, game.getAppSecret(), game.getAppRSAPriKey());
            data.put("signType", signType);
            data.put("sign", sign);

//            JSONObject response = new JSONObject();
//            response.put("state", StateCode.CODE_SUCCESS);
//            response.put("data", data);


//            Map<String, String> headers = new HashMap<String, String>();
//            headers.put("Content-Type", "application/json");



            Log.d("callback game %s server url:%s", order.getAppID(), callbackUrl);
            String serverRes = UHttpAgent.getInstance().post(callbackUrl, data);

            Log.d("game server returned:"+serverRes);

            if("SUCCESS".equals(serverRes)){
                order.setState(PayState.STATE_COMPLETE);
                order.setCompleteTime(new Date());
                orderManager.saveOrder(order);

                return true;
            }

        }catch (Exception e){
            Log.e(e.getMessage(), e);
            e.printStackTrace();
        }

        //失败了，加入重发队列，尝试6次
        OrderTaskManager.getInstance().addOrder(order);


        return false;
    }


    /***
     * 重发到游戏服
     * @param orderManager
     * @param order
     * @return
     */
    public static boolean resendCallbackToServerWithJSON(UOrderManager orderManager, UOrder order, String signType){

        UGameManager gameManager = (UGameManager)UApplicationContext.getBean("gameManager");

        UGame game = gameManager.queryGame(order.getAppID());

        if(game == null){
            return false;
        }

        String callbackUrl = order.getNotifyUrl();
        if(StringUtils.isEmpty(callbackUrl)){
            callbackUrl = game.getPayCallback();
        }

        if(StringUtils.isEmpty(callbackUrl)){

            Log.d("the order paycallback url is not configed. no in order. no in game.");
            return false;
        }

        try{


            JSONObject data = new JSONObject();
            data.put("productID", order.getProductID());
            data.put("orderID", order.getOrderID()+"");
            data.put("userID", order.getUserID());
            data.put("channelID", order.getChannelID());
            data.put("gameID", order.getAppID());
            data.put("serverID", order.getServerID());
            data.put("roleID", order.getRoleID());          //不参与签名
            data.put("money", order.getMoney());
            data.put("currency", order.getCurrency());
            data.put("extension", order.getExtension());

            //如果需要将签名方式改为MD5，把下面两行SIGN_RSA改为SIGN_MD5
            String sign = generateSign(order, signType, game.getAppSecret(), game.getAppRSAPriKey());
            data.put("signType", signType);
            data.put("sign", sign);
            Log.d("callback to game server sign: %s", sign);

            JSONObject response = new JSONObject();
            response.put("state", StateCode.CODE_SUCCESS);
            response.put("data", data);



            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json");


            String serverRes = UHttpAgent.getInstance().post(callbackUrl, headers, new ByteArrayEntity(response.toString().getBytes(Charset.forName("UTF-8"))));

            if("SUCCESS".equals(serverRes)){
                order.setState(PayState.STATE_COMPLETE);
                order.setCompleteTime(new Date());
                orderManager.saveOrder(order);

                return true;
            }

        }catch (Exception e){
            Log.e(e.getMessage(), e);
            e.printStackTrace();
        }


        return false;
    }


    /***
     * 重发到游戏服
     * @param orderManager
     * @param order
     * @return
     */
    public static boolean resendCallbackToServerWithWWW(UOrderManager orderManager, UOrder order, String signType){

        UGameManager gameManager = (UGameManager)UApplicationContext.getBean("gameManager");

        UGame game = gameManager.queryGame(order.getAppID());

        if(game == null){
            return false;
        }

        String callbackUrl = order.getNotifyUrl();
        if(StringUtils.isEmpty(callbackUrl)){
            callbackUrl = game.getPayCallback();
        }

        if(StringUtils.isEmpty(callbackUrl)){

            Log.d("the order paycallback url is not configed. no in order. no in game.");
            return false;
        }

        try{


            Map<String,String> data = new HashMap<String,String>();
            data.put("productID", order.getProductID());
            data.put("orderID", order.getOrderID()+"");
            data.put("userID", order.getUserID()+"");
            data.put("channelID", order.getChannelID()+"");
            data.put("gameID", order.getAppID()+"");
            data.put("serverID", order.getServerID());
            data.put("roleID", order.getRoleID());          //不参与签名
            data.put("money", order.getMoney()+"");
            data.put("currency", order.getCurrency());
            data.put("extension", order.getExtension());

            //如果需要将签名方式改为MD5，把下面两行SIGN_RSA改为SIGN_MD5
            String sign = generateSign(order, signType, game.getAppSecret(), game.getAppRSAPriKey());
            data.put("signType", signType);
            data.put("sign", sign);
            Log.d("callback to game server sign: %s", sign);

//            JSONObject response = new JSONObject();
//            response.put("state", StateCode.CODE_SUCCESS);
//            response.put("data", data);



//            Map<String, String> headers = new HashMap<String, String>();
//            headers.put("Content-Type", "application/json");


            String serverRes = UHttpAgent.getInstance().post(callbackUrl, data);

            if("SUCCESS".equals(serverRes)){
                order.setState(PayState.STATE_COMPLETE);
                order.setCompleteTime(new Date());
                orderManager.saveOrder(order);

                return true;
            }

        }catch (Exception e){
            Log.e(e.getMessage(), e);
            e.printStackTrace();
        }


        return false;
    }

    //生成签名
    private static String generateSign(UOrder order, String signType, String appSecret, String priKey){

        StringBuilder sb = new StringBuilder();
        sb.append("channelID=").append(order.getChannelID()).append("&")
                .append("currency=").append(order.getCurrency() == null ? "" : order.getCurrency()).append("&")
                .append("extension=").append(order.getExtension() == null ? "" : order.getExtension()).append("&")
                .append("gameID=").append(order.getAppID()).append("&")
                .append("money=").append(order.getMoney()).append("&")
                .append("orderID=").append(order.getOrderID()).append("&")
                .append("productID=").append(order.getProductID() == null ? "" : order.getProductID()).append("&")
                .append("serverID=").append(order.getServerID()).append("&")
                .append("userID=").append(order.getUserID()).append("&")
                .append(appSecret);

        Log.d("callback to game server sign str:%s", sb.toString());

        if("md5".equalsIgnoreCase(signType)){
            return EncryptUtils.md5(sb.toString()).toLowerCase();
        }else{
            return RSAUtils.sign(sb.toString(), priKey, "UTF-8", "SHA1withRSA");
        }

    }


}
