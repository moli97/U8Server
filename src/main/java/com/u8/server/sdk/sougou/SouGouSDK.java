package com.u8.server.sdk.sougou;

import com.u8.server.data.UChannel;
import com.u8.server.data.UOrder;
import com.u8.server.data.UUser;
import com.u8.server.log.Log;
import com.u8.server.sdk.*;
import com.u8.server.utils.EncryptUtils;
import net.sf.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜狗SDK
 * Created by ant on 2016/5/10.
 */
public class SouGouSDK implements ISDKScript{
    @Override
    public void verify(final UChannel channel, String extension, final ISDKVerifyListener callback) {

        try{

            JSONObject json = JSONObject.fromObject(extension);
            String sessionKey = json.getString("sessionKey");
            final String userID = json.getString("userID");

            StringBuilder sb = new StringBuilder();
            sb.append("gid=").append(URLEncoder.encode(channel.getCpAppID(), "UTF-8"))
                    .append("&session_key=").append(URLEncoder.encode(sessionKey, "UTF-8"))
                    .append("&user_id=").append(URLEncoder.encode(userID, "UTF-8"))
                    .append("&"+channel.getCpAppSecret());

            String sign = EncryptUtils.md5(sb.toString()).toLowerCase();

            Map<String,String> params = new HashMap<String, String>();
            params.put("gid", channel.getCpAppID());
            params.put("session_key", sessionKey);
            params.put("user_id", userID);
            params.put("auth", sign);


            String url = channel.getChannelAuthUrl();

            UHttpAgent.getInstance().post(url, params, new UHttpFutureCallback() {
                @Override
                public void completed(String result) {

                    try {

                        JSONObject json = JSONObject.fromObject(result);

                        if (json.containsKey("result") && json.getBoolean("result")) {
                            SDKVerifyResult vResult = new SDKVerifyResult(true, userID, "", "");

                            callback.onSuccess(vResult);

                            return;
                        }else{

                            JSONObject error = json.getJSONObject("error");
                            int code = error.getInt("code");
                            String msg = error.getString("message");
                            Log.d("sougou verify error.code:"+code+";msg:"+msg);
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    callback.onFailed(channel.getMaster().getSdkName() + " verify failed. the result is " + result);
                }

                @Override
                public void failed(String e) {
                    callback.onFailed(channel.getMaster().getSdkName() + " verify failed. " + e);
                }

            });

        }catch(Exception e){
            e.printStackTrace();
            callback.onFailed(channel.getMaster().getSdkName() + " verify execute failed. the exception is "+e.getMessage());
        }
    }

    @Override
    public void onGetOrderID(UUser user, UOrder order, ISDKOrderListener callback) {
        if(callback != null){
            callback.onSuccess("");
        }
    }
}
