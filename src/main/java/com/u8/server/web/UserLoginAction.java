package com.u8.server.web;

import com.u8.server.common.UActionSupport;
import com.u8.server.constants.StateCode;
import com.u8.server.log.Log;
import com.u8.server.sdk.UHttpAgent;
import com.u8.server.sdk.UHttpFutureCallback;
import com.u8.server.utils.JsonUtils;
import net.sf.json.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AUTH;
import org.apache.http.message.BasicNameValuePair;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 这个类是模拟游戏服
 * 客户端-》第三方SDK登录成功之后，访问u8server获取token。
 * 当token获取成功之后，就会开始连接游戏服。这个类就是模拟
 * 登录流程的最后一步操作
 *
 * 客户端拿着userID,token等信息连接游戏服务器，游戏服需要去u8server
 * 验证token，验证成功，则登录合法，否则登录失败
 *
 * Created by ant on 2015/4/17.
 */
@Controller
@Namespace("/user")
public class UserLoginAction extends UActionSupport{

    private static final String AUTH_URL = "http://localhost:8080/user/verifyAccount";

    private int userID;
    private String token;

    private String sign;            //签名

    public static void main(String[] args){
        String url = "http://localhost:8080/pay/lenovo/payCallback";
        Map<String, String> params = new HashMap<String, String>();
        params.put("transdata", "{\"transtype\":0,\"result\":0,\"transtime\":\"2016-05-24 11:52:13\",\"count\":1,\"paytype\":5,\"money\":600,\"waresid\":73978,\"appid\":\"1605170981014.app.ln\",\"exorderno\":\"968481474179235842\",\"feetype\":0,\"transid\":\"2160524115213122227904357\",\"cpprivate\":null}");
        params.put("sign", "WUzOZ4sLSD4ksSaXDHk7c3vYpphpPNbGjJDagj2zSxfubt/6Ft+Uk8FoLkKJjCOK75H6fYKwcgGN4TMRVYXk/BxOtkOYTOTczRsBcPXtSn8kMdYPnM6XYKr+LtJqyZplM3ARgPdcbRm2HXN4QjZ1dhXSFvxjVcdzyD2Mj61PMKA=");
        UHttpAgent.newInstance().post(url, params, new UHttpFutureCallback() {
            @Override
            public void completed(String content) {

                Log.d("test result: "+content);

            }

            @Override
            public void failed(String err) {
                Log.d("test faild:"+err);
            }
        });
    }

    @Action("loginServer")
    public void login(){

        try{

            Log.d("The userID is "+userID);
            Log.d("The token is "+token);
            Log.d("The sign is "+sign);

            Map<String, String> params = new HashMap<String, String>();
            params.put("userID", ""+userID);
            params.put("token", token);
            params.put("sign", sign);

            UHttpAgent.newInstance().post(AUTH_URL, params, new UHttpFutureCallback() {
                @Override
                public void completed(String content) {
                    Log.d("The loginServer check token result is " + content);
                    AuthResult result = (AuthResult) JsonUtils.decodeJson(content, AuthResult.class);

                    if (result.getState() == StateCode.CODE_SUCCESS) {
                        Log.d("login game server success. return account info ...");
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("accountID", 1);
                        jsonObject.put("accountName", "game-account-01");
                        renderState(0, jsonObject);
                    } else {
                        renderState(1, null);
                    }


                }

                @Override
                public void failed(String e) {
                    Log.e(e);
                    renderState(1, null);
                }

            });

        }catch(Exception e){
            e.printStackTrace();
            renderState(1, null);
        }

    }

    private void renderState(int state, JSONObject data){
        try{

            JSONObject json = new JSONObject();
            json.put("state", state);
            json.put("data", data);

            super.renderJson(json.toString());

        }catch(Exception e){
            e.printStackTrace();
            Log.e(e.getMessage());
        }


    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    static class AuthResult{
        private int state;
        private AuthUserInfo data;

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public AuthUserInfo getData() {
            return data;
        }

        public void setData(AuthUserInfo data) {
            this.data = data;
        }
    }

    static class AuthUserInfo{
        private String userID;
        private String username;

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
