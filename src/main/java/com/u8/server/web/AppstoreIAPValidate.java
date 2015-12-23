package com.u8.server.web;

import com.u8.server.common.UActionSupport;
import com.u8.server.log.Log;
import com.u8.server.sdk.UHttpAgent;
import com.u8.server.sdk.UHttpFutureCallback;
import com.u8.server.utils.Base64;
import com.u8.server.utils.TimeFormater;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.codehaus.jackson.map.JsonDeserializer;
import org.springframework.stereotype.Controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xzy on 15/12/21.
 */
@Controller
@Namespace("/pay/apple")
public class AppstoreIAPValidate extends UActionSupport {

    private String orderId;
    private String transactionIdentifier;
    private String productId;
    private String transactionReceipt;

    @Action("validate")
    public void validate() {
        JSONObject params = new JSONObject();

        params.put("receipt-data", Base64.encode(transactionReceipt, "UTF-8"));

        //String url = "https://buy.itunes.apple.com/verifyReceipt";
        String url = "https://sandbox.itunes.apple.com/verifyReceipt";

        Log.d("apple iap validate " + transactionReceipt);

        StringEntity entity = new StringEntity(params.toString(), "UTF-8");
        entity.setContentType("application/json");

        UHttpAgent.getInstance().post(url, null, entity, new UHttpFutureCallback() {
            public void completed(String content) {
                Log.d("apple iap validate suc:" + content);
                JSONObject json = JSONObject.fromObject(content);
            }

            public void failed(String err) {
                Log.d("apple iap validate error: " + err);
            }
        });
    }

    public void setTransactionReceipt(String transactionReceipt)
    {
        this.transactionReceipt = transactionReceipt;
    }

    public String getTransactionReceipt()
    {
        return this.transactionReceipt;
    }

    public void setTransactionIdentifier(String transactionIdentifier)
    {
        this.transactionIdentifier = transactionIdentifier;
    }

    public String getTransactionIdentifier()
    {
        return this.transactionIdentifier;
    }

    public void setOrderId(String orderId)
    {
        this.orderId = orderId;
    }

    public String getOrderId()
    {
        return this.orderId;
    }

    public void setProductId(String productId)
    {
        this.productId = productId;
    }

    public String getProductId()
    {
        return this.productId;
    }
}
