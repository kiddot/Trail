package net.client.im.core;

import android.util.Log;

import net.client.im.ClientCoreSDK;
import net.client.im.conf.ConfigClient;

import java.net.DatagramSocket;

/**
 * Created by kiddo on 17-5-31.
 */

public class LocalUDPSocketProvider {
    private static final String TAG = LocalUDPSocketProvider.class.getSimpleName();

    private DatagramSocket localUDPSocket = null;

    private DatagramSocket chatUDPSocket = null;

    private static LocalUDPSocketProvider instance = null;

    public static LocalUDPSocketProvider getInstance() {
        if (instance == null)
            instance = new LocalUDPSocketProvider();
        return instance;
    }

    private DatagramSocket resetLocalUDPSocket() {
        try {
            closeLocalUDPSocket();
            if (ClientCoreSDK.DEBUG)
                Log.d(TAG, "【IMCORE】new DatagramSocket()中...");
            this.localUDPSocket = (ConfigClient.localUDPPort == 0 ?
                    new DatagramSocket() : new DatagramSocket(ConfigClient.localUDPPort));
            this.localUDPSocket.setReuseAddress(true);
            if (ClientCoreSDK.DEBUG) {
                Log.d(TAG, "【IMCORE】new DatagramSocket()已成功完成.");
            }

            return this.localUDPSocket;
        } catch (Exception e) {
            Log.w(TAG, "【IMCORE】localUDPSocket创建时出错，原因是：" + e.getMessage(), e);

            closeLocalUDPSocket();
            return null;
        }
    }

    private DatagramSocket resetChatUDPSocket() {
        try {
            closeChatUDPSocket();
            if (ClientCoreSDK.DEBUG){
                Log.d(TAG, "resetChatUDPSocket: ");
            }
            this.chatUDPSocket = (ConfigClient.localUDPPort == 0 ?
                    new DatagramSocket() : new DatagramSocket(ConfigClient.localUDPPort));
            this.chatUDPSocket.setReuseAddress(true);

            if (ClientCoreSDK.DEBUG) {
                Log.d(TAG, "【IMCORE】new DatagramSocket()已成功完成.");
            }
            return chatUDPSocket;
        } catch (Exception e) {
            Log.w(TAG, "【IMCORE】chatUDPSocket创建时出错，原因是：" + e.getMessage(), e);

            closeChatUDPSocket();
            return null;
        }
    }

    private boolean isLocalUDPSocketReady() {
        return (this.localUDPSocket != null) && (!this.localUDPSocket.isClosed());
    }

    private boolean isChatUDPSocketReady() {
        return (chatUDPSocket != null) && (!chatUDPSocket.isClosed());
    }

    public DatagramSocket getLocalUDPSocket() {
        if (isLocalUDPSocketReady()) {
            if (ClientCoreSDK.DEBUG)
                Log.d(TAG, "【IMCORE】isLocalUDPSocketReady()==true，直接返回本地socket引用哦。");
            return this.localUDPSocket;
        }

        if (ClientCoreSDK.DEBUG)
            Log.d(TAG, "【IMCORE】isLocalUDPSocketReady()==false，需要先resetLocalUDPSocket()...");
        return resetLocalUDPSocket();
    }

    public DatagramSocket getChatUDPSocket() {
        if (isChatUDPSocketReady()) {
            if (ClientCoreSDK.DEBUG)
                Log.d(TAG, "【IMCORE】getChatUDPSocket==true，直接返回打洞聊天的socket引用哦。");
            return this.chatUDPSocket;
        }

        if (ClientCoreSDK.DEBUG)
            Log.d(TAG, "【IMCORE】getChatUDPSocket==true，需要先resetChatUDPSocket()...");
        return resetChatUDPSocket();
    }

    public void closeLocalUDPSocket() {
        try {
            if (ClientCoreSDK.DEBUG)
                Log.d(TAG, "【IMCORE】正在closeLocalUDPSocket()...");
            if (this.localUDPSocket != null) {
                this.localUDPSocket.close();
                this.localUDPSocket = null;
            } else {
                Log.d(TAG, "【IMCORE】Socket处于未初化状态（可能是您还未登陆），无需关闭。");
            }
        } catch (Exception e) {
            Log.w(TAG, "【IMCORE】lcloseLocalUDPSocket时出错，原因是：" + e.getMessage(), e);
        }
    }

    public void closeChatUDPSocket() {
        try {
            if (ClientCoreSDK.DEBUG) {
                Log.d(TAG, "closeChatUDPSocket: ing");
            }

            if (this.chatUDPSocket != null) {
                this.chatUDPSocket.close();
                chatUDPSocket = null;
            } else {
                Log.d(TAG, "closeChatUDPSocket: chatUDPSocket未进行初始化");
            }
        } catch (Exception e){
            Log.d(TAG, "closeChatUDPSocket: " + e.getMessage());
        }
    }
}
