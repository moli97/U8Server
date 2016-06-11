package com.u8.server.web;

import com.u8.server.constants.PayState;
import com.u8.server.constants.StateCode;
import com.u8.server.data.UGame;
import com.u8.server.data.UMsdkOrder;
import com.u8.server.data.UOrder;
import com.u8.server.log.Log;
import com.u8.server.sdk.UHttpAgent;
import com.u8.server.sdk.appchina.RSAUtil;
import com.u8.server.sdk.kuaiyong.kuaiyong.RSAEncrypt;
import com.u8.server.service.UOrderManager;
import com.u8.server.utils.EncryptUtils;
import com.u8.server.utils.RSAUtils;
import com.u8.server.utils.UGenerator;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ByteArrayEntity;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * U8Server向游戏服发送回调通知
 * Created by ant on 2015/2/9.
 */
public class SendAgent {

    public static final String SIGN_MD5 = "md5";
    public static final String SIGN_RSA = "rsa";

    public static boolean sendCallbackToServer(UOrderManager orderManager, UOrder order){

        UGame game = order.getGame();
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

        JSONObject data = new JSONObject();
        data.put("productID", order.getProductID());
        data.put("orderID", order.getOrderID());
        data.put("userID", order.getUserID());
        data.put("channelID", order.getChannelID());
        data.put("gameID", order.getAppID());
        data.put("serverID", order.getServerID());
        data.put("money", order.getMoney());
        data.put("currency", order.getCurrency());
        data.put("extension", order.getExtension());

        //如果需要将签名方式改为MD5，把下面两行SIGN_RSA改为SIGN_MD5
        String sign = generateSign(order, SIGN_RSA);
        data.put("signType", SIGN_RSA);
        data.put("sign", sign);

        JSONObject response = new JSONObject();
        response.put("state", StateCode.CODE_SUCCESS);
        response.put("data", data);



        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "text/html");


        String serverRes = UHttpAgent.getInstance().post(callbackUrl, headers, new ByteArrayEntity(response.toString().getBytes(Charset.forName("UTF-8"))));

        if(serverRes.equals("SUCCESS")){
            order.setState(PayState.STATE_COMPLETE);
            orderManager.saveOrder(order);
            return true;
        }

        return false;
    }

    //生成签名
    private static String generateSign(UOrder order, String signType){

        StringBuilder sb = new StringBuilder();
        sb.append("channelID=").append(order.getChannelID()).append("&")
                .append("currency=").append(order.getCurrency()).append("&")
                .append("extension=").append(order.getExtension()).append("&")
                .append("gameID=").append(order.getAppID()).append("&")
                .append("money=").append(order.getMoney()).append("&")
                .append("orderID=").append(order.getOrderID()).append("&")
                .append("productID=").append(order.getProductID()).append("&")
                .append("serverID=").append(order.getServerID()).append("&")
                .append("userID=").append(order.getUserID()).append("&")
                .append(order.getGame().getAppSecret());

        if("md5".equalsIgnoreCase(signType)){
            return EncryptUtils.md5(sb.toString()).toLowerCase();
        }else{
            return RSAUtils.sign(sb.toString(), order.getGame().getAppRSAPriKey(), "UTF-8");
        }

    }

    /**
     * 应用宝需要游戏服务器做特殊处理，这里让游戏服务器提供一个独立的回调通知地址,来处理应用宝等场景
     *
     * 应用宝现在是YSDK了，不再需要这个接口
     * @param orderManager
     * @param order
     * @return
     */
    @Deprecated
    public static boolean sendMSDKCallbackToServer(UOrderManager orderManager, UMsdkOrder order){

        UGame game = order.getGame();
        if(game == null){

            Log.e("send msdk order to game server failed. game is null. orderID:%s "+ order.getId());
            return false;
        }

        if(StringUtils.isEmpty(game.getMsdkPayCallback())){
            Log.e("send msdk order to game server failed. msdk pay callback url is not configed.");
            return false;
        }

        JSONObject data = new JSONObject();
        data.put("userID", order.getUserID());
        data.put("gameID", order.getAppID());
        data.put("channelID", order.getChannelID());
        data.put("coinNum", order.getCoinNum());
        data.put("firstPay", order.getFirstPay());
        data.put("allMoney", order.getAllMoney());

        JSONObject response = new JSONObject();
        response.put("state", StateCode.CODE_SUCCESS);
        response.put("data", data);
        response.put("sign", RSAUtils.sign(data.toString(),game.getAppRSAPriKey(), "UTF-8"));


        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Content-Type", "text/html");

        String serverRes = UHttpAgent.getInstance().post(game.getMsdkPayCallback(), headers, new ByteArrayEntity(response.toString().getBytes(Charset.forName("UTF-8"))));

        if(serverRes.equals("SUCCESS")){
            order.setState(PayState.STATE_COMPLETE);
            orderManager.saveUMsdkOrder(order);
            return true;
        }

        return false;
    }

}
