package com.u8.server.sdk.appstore;

/**
 * Created by xzy on 16/2/2.
 * AppStore 内购相关逻辑
 */

import com.u8.server.data.UOrder;
import com.u8.server.log.Log;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component("appstoreIAPManager")
@Scope("singleton")
public class AppstoreIAPManager {

    private static final long DELAY_MILLIS = 20000;      //每次延迟执行间隔,ms
    private static final int MAX_RETRY_NUM = 6;         //最多重试6次

    private static AppstoreIAPManager instance;

    private DelayQueue<ValidateTask> tasks;

    private ExecutorService executor;

    private boolean isRunning = false;

    private AppstoreIAPManager()
    {
        this.tasks = new DelayQueue<ValidateTask>();
        executor = Executors.newFixedThreadPool(3);
    }

    public static AppstoreIAPManager getInstance(){
        if(instance == null){
            instance = new AppstoreIAPManager();
        }
        return instance;
    }

    //添加一个新支付请求到队列中
    public void addPayRequest(UOrder order, String receipt){
        ValidateTask task = new ValidateTask(order, receipt, 100, MAX_RETRY_NUM);
        this.tasks.add(task);

        if(!isRunning){
            isRunning = true;
            execute();
        }
    }

    public void execute(){
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {

                    while (isRunning) {
                        ValidateTask task = tasks.take();
                        task.run();
                        if (task.getState() == ValidateTask.STATE_RETRY) {
                            task.setDelay(DELAY_MILLIS);
                            tasks.add(task);
                        } else if (task.getState() == ValidateTask.STATE_FAILED) {
                            UOrder order = task.getOrder();
                            Log.e("the user[%s](channel userID:%s) charge failed.", order.getUsername(), order.getUserID());
                        }
                    }

                } catch (Exception e) {
                    Log.e(e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    public void destory(){
        this.isRunning = false;
        if(executor != null){
            executor.shutdown();
            executor = null;
        }
    }

}
