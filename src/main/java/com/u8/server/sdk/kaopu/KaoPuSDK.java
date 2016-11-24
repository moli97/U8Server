package com.u8.server.sdk.kaopu;

import com.u8.server.data.UChannel;
import com.u8.server.data.UOrder;
import com.u8.server.data.UUser;
import com.u8.server.log.Log;
import com.u8.server.sdk.*;
import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 靠谱助手SDK
 * Created by xiaohei on 16/10/16.
 */
public class KaoPuSDK implements ISDKScript {


    @Override
    public void verify(final UChannel channel, String extension, final ISDKVerifyListener callback) {


        try{

            JSONObject json = JSONObject.fromObject(extension);
            final String uid = json.getString("uid");
            final String username = json.getString("username");
            final String verifyUrl = json.getString("verifyUrl");

            Log.d("the kaopu verify url is %s", verifyUrl);

            UHttpAgent httpClient = UHttpAgent.getInstance();

//            Map<String,String> params = new HashMap<String, String>();
//            params.put("token",token);

            httpClient.post(verifyUrl, null, new UHttpFutureCallback() {
                @Override
                public void completed(String result) {

                    Log.d("The auth result is " + result);

                    JSONObject json = JSONObject.fromObject(result);

                    if(json.containsKey("code") && json.getInt("code") == 1){

                        callback.onSuccess(new SDKVerifyResult(true, uid, username, ""));
                        return;
                    }

                    callback.onFailed(channel.getMaster().getSdkName() + " verify failed. the post result is " + result);

                }

                @Override
                public void failed(String e) {

                    callback.onFailed(channel.getMaster().getSdkName() + " verify failed. " + e);
                }


            });


        }catch (Exception e){
            callback.onFailed(channel.getMaster().getSdkName() + " verify execute failed. the exception is "+e.getMessage());
            Log.e(e.getMessage());
        }

    }

    @Override
    public void onGetOrderID(UUser user, UOrder order, ISDKOrderListener callback) {
        if(callback != null){
            callback.onSuccess(user.getChannel().getPayCallbackUrl());
        }
    }

}
