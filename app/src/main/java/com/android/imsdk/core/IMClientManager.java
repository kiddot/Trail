package com.android.imsdk.core;

import android.content.Context;

import com.android.imsdk.event.ChatBaseEventImpl;
import com.android.imsdk.event.ChatTransDataEventImpl;
import com.android.imsdk.event.MessageQoSEventImpl;

import net.client.im.ClientCoreSDK;
import net.client.im.conf.ConfigClient;

/**
 * Created by kiddo on 17-6-3.
 */

public class IMClientManager {
    private static String TAG = IMClientManager.class.getSimpleName();

    private static IMClientManager instance = null;

    /**
     * MobileIMSDK是否已被初始化. true表示已初化完成，否则未初始化.
     */
    private boolean init = false;

    //
    private ChatBaseEventImpl baseEventListener = null;
    //
    private ChatTransDataEventImpl transDataListener = null;
    //
    private MessageQoSEventImpl messageQoSListener = null;

    private Context context = null;

    public static IMClientManager getInstance(Context context) {
        if (instance == null)
            instance = new IMClientManager(context);
        return instance;
    }

    private IMClientManager(Context context) {
        this.context = context;
        initMobileIMSDK();
    }

    public void initMobileIMSDK() {
        if (!init) {
            // 设置AppKey
            ConfigClient.appKey = "5418023dfd98c579b6001741";


            ClientCoreSDK.getInstance().init(this.context);

            // 设置事件回调
            baseEventListener = new ChatBaseEventImpl();
            transDataListener = new ChatTransDataEventImpl();
            messageQoSListener = new MessageQoSEventImpl();
            ClientCoreSDK.getInstance().setChatBaseEvent(baseEventListener);
            ClientCoreSDK.getInstance().setChatTransDataEvent(transDataListener);
            ClientCoreSDK.getInstance().setMessageQoSEvent(messageQoSListener);

            init = true;
        }
    }

    public void release() {
        ClientCoreSDK.getInstance().release();
    }

    public ChatTransDataEventImpl getTransDataListener() {
        return transDataListener;
    }

    public ChatBaseEventImpl getBaseEventListener() {
        return baseEventListener;
    }

    public MessageQoSEventImpl getMessageQoSListener() {
        return messageQoSListener;
    }
}
