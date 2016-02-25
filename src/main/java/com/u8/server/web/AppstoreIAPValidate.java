package com.u8.server.web;

import com.u8.server.common.UActionSupport;
import com.u8.server.constants.PayState;
import com.u8.server.data.UOrder;
import com.u8.server.log.Log;
import com.u8.server.sdk.UHttpAgent;
import com.u8.server.sdk.UHttpFutureCallback;
import com.u8.server.sdk.appstore.AppstoreIAPManager;
import com.u8.server.service.UOrderManager;
import com.u8.server.utils.Base64;
import com.u8.server.utils.TimeFormater;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.codehaus.jackson.map.JsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by xzy on 15/12/21.
 */
@Controller
@Namespace("/pay/apple")
public class AppstoreIAPValidate extends UActionSupport {

    private String orderID;
    private String transactionIdentifier;
    private String productId;
    private String transactionReceipt;

    private UOrder order;
    @Autowired
    private UOrderManager orderManager;

    @Action("validate")
    public void validate() {
        try {
            if (transactionReceipt.startsWith("{"))
            {
                transactionReceipt = Base64.encode(transactionReceipt, "UTF-8");
            }

            order = orderManager.getOrder(Long.parseLong(orderID));

            // TODO: 保存receipt记录
            Log.d("apple iap validate " + transactionReceipt);

            AppstoreIAPManager mgr = AppstoreIAPManager.getInstance();
            mgr.addPayRequest(order, transactionReceipt);

            this.renderState(true, "Success");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            try {
                this.renderState(false, "未知错误");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void renderState(boolean suc, String msg) throws IOException {

        PrintWriter out = this.response.getWriter();

        if(suc){
            out.write("SUCCESS");
        }else{
            out.write("FAILURE");
        }
        out.flush();
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

    public void setOrderID(String orderID)
    {
        this.orderID = orderID;
    }

    public String getOrderID()
    {
        return this.orderID;
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
