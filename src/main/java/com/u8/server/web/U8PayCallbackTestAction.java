package com.u8.server.web;

import com.u8.server.common.UActionSupport;
import com.u8.server.log.Log;
import com.u8.server.sdk.uc.PayCallbackResponse;
import com.u8.server.utils.JsonUtils;
import net.sf.json.JSONObject;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.springframework.stereotype.Controller;

import java.io.BufferedReader;

/**
 * 这个是U8Server通知游戏服的Demo
 * 模拟游戏服务器的处理支付回调的接口
 * Created by ant on 2015/2/9.
 */
@Controller
@Namespace("/pay/game")
public class U8PayCallbackTestAction extends UActionSupport{



    @Action("payCallback")
    public void payCallback(){

        try{

            BufferedReader br = this.request.getReader();
            String line;
            StringBuilder sb = new StringBuilder();
            while((line=br.readLine()) != null){
                sb.append(line).append("\r\n");
            }

            Log.d("U8Server Pay Callback . response params:" + sb.toString());

            JSONObject json = JSONObject.fromObject(sb.toString());
            if(json.containsKey("state")){
                renderText("SUCCESS");

                int state = json.getInt("state");
                if(state == 1){
                    String dataStr = json.getString("data");
                    //游戏自己的逻辑，写在这里
                }

            }else{
                renderText("FAIL");
            }


        }catch (Exception e){
            renderText("FAIL");
            e.printStackTrace();
        }

    }

}
