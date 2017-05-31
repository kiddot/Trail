package net.client.im;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import net.client.im.core.KeepAliveDaemon;
import net.client.im.core.LocalUDPSocketProvider;

/**
 * Created by kiddo on 17-5-30.
 */

public class ClientCoreSDK {
    private static final String TAG = ClientCoreSDK.class.getSimpleName();

    public static boolean DEBUG = true;


    public static boolean autoReLogin = true;

    private static ClientCoreSDK instance = null;

    private boolean mIsInit = false;

    private boolean localDeviceNetworkOk = true;

    private boolean connectedToServer = true;

    private boolean loginHasInit = false;

    private int currentUserId = -1;

    private String currentLoginName = null;

    private String currentLoginPsw = null;

    private String currentLoginExtra = null;

    private ChatBaseEvent chatBaseEvent = null;

    private ChatTransDataEvent chatTransDataEvent = null;

    private MessageQoSEvent messageQoSEvent = null;

    private Context context = null;

    /**
     * 用于监听本地网络的变化状况
     */
    private final BroadcastReceiver networkConnectionStatusBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectMgr = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
            NetworkInfo mobNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (!(mobNetInfo != null && mobNetInfo.isConnected())
                    && !(wifiNetInfo != null && wifiNetInfo.isConnected())) {
                Log.e(ClientCoreSDK.TAG, "【IMCORE】【本地网络通知】检测本地网络连接断开了!");

                ClientCoreSDK.this.localDeviceNetworkOk = false;
                LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();
            }
            else {
                if (ClientCoreSDK.DEBUG) {
                    Log.e(ClientCoreSDK.TAG, "【IMCORE】【本地网络通知】检测本地网络已连接上了!");
                }

                ClientCoreSDK.this.localDeviceNetworkOk = true;
                LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();
            }
        }
    };

    public static ClientCoreSDK getInstance() {
        if (instance == null)
            instance = new ClientCoreSDK();
        return instance;
    }

    public void init(Context context) {
        if (!this.mIsInit) {
            if (context == null) {
                throw new IllegalArgumentException("context can't be null!");
            }

            // 将全局Application作为context上下文句柄：
            //   由于Android程序的特殊性，整个APP的生命周中除了Application外，其它包括Activity在内
            //   都可能是短命且不可靠的（随时可能会因虚拟机资源不足而被回收），所以MobileIMSDK作为跟
            //   整个APP的生命周期保持一致的全局资源，它的上下文用Application是最为恰当的。
            if(context instanceof Application)
                this.context = context;
            else {
                this.context = context.getApplicationContext();
            }

            // 注册广播用于监听网络的变化状况
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            this.context.registerReceiver(networkConnectionStatusBroadcastReceiver, intentFilter);

            this.mIsInit = true;
        }
    }

    public void release()
    {
        // 尝试停掉掉线重连线程（如果线程正在运行的话）
        AutoReLoginDaemon.getInstance(context).stop();
        // 尝试停掉QoS质量保证（发送）心跳线程
        QoS4SendDaemon.getInstance(context).stop();
        // 尝试停掉Keep Alive心跳线程
        KeepAliveDaemon.getInstance(context).stop();
        // 尝试停掉消息接收者
        LocalUDPDataReciever.getInstance(context).stop();
        // 尝试停掉QoS质量保证（接收防重复机制）心跳线程
        QoS4ReciveDaemon.getInstance(context).stop();
        // 尝试关闭本地Socket
        LocalUDPSocketProvider.getInstance().closeLocalUDPSocket();

        try
        {
            this.context.unregisterReceiver(this.networkConnectionStatusBroadcastReceiver);
        }
        catch (Exception e)
        {
            Log.w(TAG, e.getMessage(), e);
        }

        this.mIsInit = false;

        setLoginHasInit(false);
        setConnectedToServer(false);
    }

    public int getCurrentUserId() {
        return this.currentUserId;
    }

    public ClientCoreSDK setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
        return this;
    }

    public String getCurrentLoginName()
    {
        return this.currentLoginName;
    }

    public ClientCoreSDK setCurrentLoginName(String currentLoginName) {
        this.currentLoginName = currentLoginName;
        return this;
    }

    public String getCurrentLoginPsw() {
        return this.currentLoginPsw;
    }

    public void setCurrentLoginPsw(String currentLoginPsw) {
        this.currentLoginPsw = currentLoginPsw;
    }

    public String getCurrentLoginExtra() {
        return currentLoginExtra;
    }

    public ClientCoreSDK setCurrentLoginExtra(String currentLoginExtra) {
        this.currentLoginExtra = currentLoginExtra;
        return this;
    }

    public boolean isLoginHasInit() {
        return this.loginHasInit;
    }

    public ClientCoreSDK setLoginHasInit(boolean loginHasInit) {
        this.loginHasInit = loginHasInit;

        return this;
    }

    public boolean isConnectedToServer() {
        return this.connectedToServer;
    }

    public void setConnectedToServer(boolean connectedToServer) {
        this.connectedToServer = connectedToServer;
    }

    public boolean isInitialed() {
        return this.mIsInit;
    }

    public boolean isLocalDeviceNetworkOk() {
        return this.localDeviceNetworkOk;
    }

    public void setChatBaseEvent(ChatBaseEvent chatBaseEvent) {
        this.chatBaseEvent = chatBaseEvent;
    }

    public ChatBaseEvent getChatBaseEvent() {
        return this.chatBaseEvent;
    }

    public void setChatTransDataEvent(ChatTransDataEvent chatTransDataEvent) {
        this.chatTransDataEvent = chatTransDataEvent;
    }

    public ChatTransDataEvent getChatTransDataEvent() {
        return this.chatTransDataEvent;
    }

    public void setMessageQoSEvent(MessageQoSEvent messageQoSEvent) {
        this.messageQoSEvent = messageQoSEvent;
    }

    public MessageQoSEvent getMessageQoSEvent() {
        return this.messageQoSEvent;
    }
}
