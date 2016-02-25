/**
 * Created by xzy on 16/2/2.
 * AppStore内购订单验证
 * https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateRemotely.html
 */

package com.u8.server.sdk.appstore;

import com.u8.server.cache.UApplicationContext;
import com.u8.server.constants.PayState;
import com.u8.server.data.UChannel;
import com.u8.server.data.UChannelMaster;
import com.u8.server.data.UOrder;
import com.u8.server.log.Log;
import com.u8.server.sdk.UHttpAgent;
import com.u8.server.sdk.UHttpFutureCallback;
import com.u8.server.service.UOrderManager;
import com.u8.server.utils.Base64;
import com.u8.server.web.SendAgent;
import net.sf.json.JSONObject;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class ValidateTask implements Runnable, Delayed {

    public static final int STATE_INIT = 1;        //第一次状态
    public static final int STATE_COMPLETE = 2;     //完成状态
    public static final int STATE_RETRY = 3;        //重试状态
    public static final int STATE_FAILED = 4;       //失败状态

    final String urlSandbox = "https://sandbox.itunes.apple.com/verifyReceipt";
    final String urlProduct = "https://buy.itunes.apple.com/verifyReceipt";

    private boolean sandboxMode = false;              //是否是沙盒模式
    private int state = STATE_INIT;          //任务状态
    private long time;                       //任务执行时间
    private int retryCount = 0;              //已经重试的次数
    private int maxRetryCount;

    private String transactionReceipt;
    private UOrder order;

    @Autowired
    private UOrderManager orderManager;

    public ValidateTask(UOrder order, String receipt, long delayMillis, int maxRetryCount){
        this.order = order;
        this.transactionReceipt = receipt;
        this.state = STATE_INIT;
        this.time = System.nanoTime() + TimeUnit.NANOSECONDS.convert(delayMillis, TimeUnit.MILLISECONDS);
        this.retryCount = 0;
        this.maxRetryCount = maxRetryCount;
    }

    public UOrder getOrder() { return order; }
    public String getReceipt() {
        return transactionReceipt;
    }

    public void setReceipt(String receipt) {
        this.transactionReceipt = receipt;
    }

    public boolean getSandboxMode() {
        return sandboxMode;
    }

    public void setSandboxMode(boolean value) {
        sandboxMode = value;
    }

    public void setDelay(long delayMillis){
        this.time = System.nanoTime() + TimeUnit.NANOSECONDS.convert(delayMillis, TimeUnit.MILLISECONDS);
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }


    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(time - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        ValidateTask task = (ValidateTask)o;
        long result = task.getTime() - this.getTime();

        return result > 0 ? 1 : (result < 0 ? -1 : 0);
    }

    /**
     * 时间到了，执行支付逻辑
     */
    @Override
    public void run() {
        if(this.state == STATE_COMPLETE || this.state == STATE_FAILED){
            return;
        }

        try{
            JSONObject params = new JSONObject();

            params.put("receipt-data", transactionReceipt);

            StringEntity entity = new StringEntity(params.toString(), "UTF-8");
            entity.setContentType("application/json");

            doValidate(entity);

            this.retryCount++;
            if(this.retryCount >= this.maxRetryCount){
                this.state = STATE_FAILED;

            }else{
                this.state = STATE_RETRY;
            }

        }catch (Exception e){
            Log.e(e.getMessage());
            e.printStackTrace();
        }
    }

    private void doValidate(final StringEntity httpParams)
    {
        String url = sandboxMode ? urlSandbox : urlProduct;

        //首先尝试生产环境请求验证
        UHttpAgent.getInstance().post(url, null, httpParams, new UHttpFutureCallback() {
            public void completed(String content) {
                Log.d("apple iap validate suc:" + content);
                JSONObject json = JSONObject.fromObject(content);

                //如果返回21007状态，转到sandbox环境验证
                if (!sandboxMode && json.getInt("status") == 21007) {
                    sandboxMode = true;
                    doValidate(httpParams);
                } else if (json.getInt("status") == 0) {
                    //验证成功
                    OnValidatedSuccess(json);
                } else {
                    OnValidateFail(json);
                }
            }

            public void failed(String err) {
                Log.d("apple iap validate error: " + err);
                //TODO: 更新receipt记录状态
            }
        });
    }

    private void OnValidatedSuccess(JSONObject json)
    {
        //TODO: 更新receipt记录状态
        if(order == null || order.getChannel() == null){
            Log.d("The order is null or the channel is null.");
            return;
        }

        if(order.getState() == PayState.STATE_COMPLETE){
            Log.d("The state of the order is complete. The state is "+order.getState());
            return;
        }

        if (order.getExtension().equals(json.getString("product_id")))
        {
            order.setCompleteTime(new Date());
            order.setState(PayState.STATE_SUC);
            orderManager.saveOrder(order);
        }
    }

    private void OnValidateFail(JSONObject json)
    {
        //TODO: 更新receipt记录状态
        if(order == null || order.getChannel() == null){
            Log.d("The order is null or the channel is null.");
            return;
        }

        if(order.getState() == PayState.STATE_COMPLETE){
            Log.d("The state of the order is complete. The state is " + order.getState());
            return;
        }

        order.setCompleteTime(new Date());
        order.setState(PayState.STATE_FAILED);
        orderManager.saveOrder(order);
    }
}

