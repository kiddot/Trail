package net.client.im.core;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import net.client.im.ClientCoreSDK;
import net.client.im.conf.ConfigClient;
import net.client.im.qos.QoS4SendDaemon;
import net.client.im.util.UDPUtils;
import net.server.im.protocol.ErrorCode;
import net.server.im.protocol.Protocol;
import net.server.im.protocol.ProtocolFactory;
import net.server.im.protocol.ProtocolType;
import net.server.im.util.CharsetHelper;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * Created by kiddo on 17-5-30.
 */

public class LocalUDPDataSender {
    private static final String TAG = LocalUDPDataSender.class.getSimpleName();
    private static LocalUDPDataSender mInstance = null;

    private Context mContext = null;

    public static LocalUDPDataSender getInstance(Context context) {
        if (mInstance == null)
            mInstance = new LocalUDPDataSender(context);
        return mInstance;
    }

    private LocalUDPDataSender(Context context) {
        this.mContext = context;
    }

    int sendKeepAlive() {
        byte[] b = ProtocolFactory.createPKeepAlive(ClientCoreSDK.getInstance().getCurrentUserId()).toBytes();
        return send(b, b.length);
    }

    int sendLogin(String loginName, String loginPsw, String extra) {
        byte[] b = ProtocolFactory.createPLoginInfo(loginName, loginPsw, extra).toBytes();
        int code = send(b, b.length);
        // 登陆信息成功发出时就把登陆名存下来
        if (code == 0) {
            ClientCoreSDK.getInstance().setCurrentLoginName(loginName);
            ClientCoreSDK.getInstance().setCurrentLoginPsw(loginPsw);
            ClientCoreSDK.getInstance().setCurrentLoginExtra(extra);
        }

        return code;
    }

    public int sendLoginOut() {
        int code = ErrorCode.COMMON_CODE_OK;
        if (ClientCoreSDK.getInstance().isLoginHasInit()) {
            byte[] b = ProtocolFactory.createPLoginoutInfo(
                    ClientCoreSDK.getInstance().getCurrentUserId()
                    , ClientCoreSDK.getInstance().getCurrentLoginName()).toBytes();
            code = send(b, b.length);
            // 登出信息成功发出时
            if (code == 0) {
//				// 发出退出登陆的消息同时也关闭心跳线程
//				KeepAliveDaemon.getInstance(context).stop();
//				// 重置登陆标识
//				ClientCoreSDK.getInstance().setLoginHasInit(false);
            }
        }

        // 释放SDK资源
        ClientCoreSDK.getInstance().release();

        return code;
    }

    public int sendOnline(){
        Log.d(TAG, "请求当前在线用户");
        int type = ProtocolType.C.FROM_CLIENT_TYPE_OF_ONLINE;
        Protocol protocol = new Protocol(type, null, ClientCoreSDK.getInstance().getCurrentUserId(), 0);
        byte[] bytes = protocol.toBytes();
        return send(bytes, bytes.length);
    }

    @Deprecated
    public int sendChatP2P(String chatIP, int chatPort) {
        byte[] b = ProtocolFactory.createPNatInfo(ClientCoreSDK.getInstance().getCurrentUserId(), 10002).toBytes();
        return sendChatMsg(b, b.length, chatIP, chatPort);
    }

    private int send(byte[] fullProtocolBytes, int dataLen) {
        if (!ClientCoreSDK.getInstance().isInitialed())
            return ErrorCode.ForC.CLIENT_SDK_NO_INITIALED;

        if (!ClientCoreSDK.getInstance().isLocalDeviceNetworkOk()) {
            Log.e(TAG, "【IMCORE】本地网络不能工作，send数据没有继续!");
            return ErrorCode.ForC.LOCAL_NETWORK_NOT_WORKING;
        }

        DatagramSocket ds = LocalUDPSocketProvider.getInstance().getLocalUDPSocket();
        // 如果Socket没有连接上服务端
        SocketAddress target = new InetSocketAddress(ConfigClient.serverIP, ConfigClient.serverUDPPort);
        try {
            ds.connect(target);
        } catch (SocketException e) {
            e.printStackTrace();
        }
//        if (ds != null && !ds.isConnected()) {
//            try {
//                if (ConfigClient.serverIP == null) {
//                    Log.w(TAG, "【IMCORE】send数据没有继续，原因是ConfigEntity.server_ip==null!");
//                    return ErrorCode.ForC.TO_SERVER_NET_INFO_NOT_SETUP;
//                }
//
//                // 即刻连接上服务端（如果不connect，即使在DataProgram中设置了远程id和地址则服务端MINA也收不到，跟普通的服
//                // 务端UDP貌似不太一样，普通UDP时客户端无需先connect可以直接send设置好远程ip和端口的DataPragramPackage）
//                //TODO : 此处进行了连接，只能与该地址端口通信
//                ds.connect(InetAddress.getByName(ConfigClient.serverIP), ConfigClient.serverUDPPort);
//            } catch (Exception e) {
//                Log.w(TAG, "【IMCORE】send时出错，原因是：" + e.getMessage(), e);
//                return ErrorCode.ForC.BAD_CONNECT_TO_SERVER;
//            }
//        }
        return UDPUtils.send(ds, fullProtocolBytes, dataLen) ? ErrorCode.COMMON_CODE_OK : ErrorCode.COMMON_DATA_SEND_FAILD;
    }

    @Deprecated
    private int sendAfterSuccess(byte[] fullProtocolBytes, int dataLen){
        if (!ClientCoreSDK.getInstance().isInitialed())
            return ErrorCode.ForC.CLIENT_SDK_NO_INITIALED;

        if (!ClientCoreSDK.getInstance().isLocalDeviceNetworkOk()) {
            Log.e(TAG, "【IMCORE】本地网络不能工作，send数据没有继续!");
            return ErrorCode.ForC.LOCAL_NETWORK_NOT_WORKING;
        }

        final DatagramSocket datagramSocket = LocalUDPSocketProvider.getInstance().getLocalUDPSocket();
        return UDPUtils.send(datagramSocket, fullProtocolBytes, dataLen) ? ErrorCode.COMMON_CODE_OK : ErrorCode.COMMON_DATA_SEND_FAILD;
    }

    @Deprecated
    private int sendChatMsg(byte[] fullProtocolBytes, int dataLen, String chatIP, int chatPort) {
        if (!ClientCoreSDK.getInstance().isInitialed())
            return ErrorCode.ForC.CLIENT_SDK_NO_INITIALED;

        if (!ClientCoreSDK.getInstance().isLocalDeviceNetworkOk()) {
            Log.e(TAG, "【IMCORE】本地网络不能工作，send数据没有继续!");
            return ErrorCode.ForC.LOCAL_NETWORK_NOT_WORKING;
        }

        final DatagramSocket datagramSocket = LocalUDPSocketProvider.getInstance().getLocalUDPSocket();
        final SocketAddress target = new InetSocketAddress(chatIP, chatPort);
        Thread runnable = new Thread() {
            @Override
            public void run() {
                try {
                    datagramSocket.connect(target);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        };
        runnable.start();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        if (datagramSocket != null && !datagramSocket.isClosed()) {
//            try {
//                //datagramSocket.connect(InetAddress.getByName(chatIP), chatPort);
//            } catch (Exception e) {
//                Log.w(TAG, "【IMCORE】 sendChatMsg时出错，原因是：" + e.getMessage(), e);
//                return ErrorCode.ForC.BAD_CONNECT_TO_SERVER;
//            }
//        }
        return UDPUtils.send(datagramSocket, fullProtocolBytes, dataLen) ? ErrorCode.COMMON_CODE_OK : ErrorCode.COMMON_DATA_SEND_FAILD;
    }

    public static abstract class SendCommonDataAsync extends AsyncTask<Object, Integer, Integer> {
        protected Context context = null;
        protected Protocol p = null;

        public SendCommonDataAsync(Context context, byte[] dataContent, int dataLen, int to_user_id) {
            this(context, CharsetHelper.getString(dataContent, dataLen), to_user_id);
        }

        public SendCommonDataAsync(Context context, String dataContentWidthStr, int to_user_id, boolean QoS) {
            this(context, dataContentWidthStr, to_user_id, QoS, null);
        }

        public SendCommonDataAsync(Context context, String dataContentWidthStr, int to_user_id, boolean QoS, String fingerPrint) {
            this(context,
                    ProtocolFactory.createCommonData(dataContentWidthStr,
                            ClientCoreSDK.getInstance().getCurrentUserId(), to_user_id, QoS, fingerPrint));
        }

        public SendCommonDataAsync(Context context, String dataContentWidthStr, int to_user_id) {
            this(context,
                    ProtocolFactory.createCommonData(dataContentWidthStr,
                            ClientCoreSDK.getInstance().getCurrentUserId(), to_user_id));
        }

        public SendCommonDataAsync(Context context, Protocol p) {
            if (p == null) {
                Log.w(TAG, "【IMCORE】无效的参数p==null!");
                return;
            }
            this.context = context;
            this.p = p;
        }

        protected Integer doInBackground(Object[] params) {
            if (this.p != null)
                return Integer.valueOf(LocalUDPDataSender.getInstance(context).sendCommonData(p));
            return Integer.valueOf(-1);
        }

        protected abstract void onPostExecute(Integer paramInteger);
    }

    public int sendCommonData(Protocol p) {
        if (p != null) {
            byte[] b = p.toBytes();
            int code = send(b, b.length);
            if (code == 0) {
                // 如果需要进行QoS质量保证，则把它放入质量保证队列中供处理(已在存在于列
                // 表中就不用再加了，已经存在则意味当前发送的这个是重传包)
                if (p.isQoS() && !QoS4SendDaemon.getInstance(mContext).exist(p.getFp()))
                    QoS4SendDaemon.getInstance(mContext).put(p);
            }
            return code;
        } else
            return ErrorCode.COMMON_INVALID_PROTOCAL;
    }

    public static abstract class SendLoginDataAsync extends AsyncTask<Object, Integer, Integer> {
        protected Context context = null;
        protected String loginName = null;
        protected String loginPsw = null;
        protected String extra = null;

        public SendLoginDataAsync(Context context, String loginName, String loginPsw) {
            this(context, loginName, loginPsw, null);
        }

        public SendLoginDataAsync(Context context, String loginName, String loginPsw, String extra) {
            this.context = context;
            this.loginName = loginName;
            this.loginPsw = loginPsw;
            this.extra = extra;
        }

        protected Integer doInBackground(Object[] params) {
            int code = LocalUDPDataSender.getInstance(this.context)
                    .sendLogin(this.loginName, this.loginPsw, this.extra);
            return Integer.valueOf(code);
        }

        protected void onPostExecute(Integer code) {
            if (code.intValue() == 0) {
                LocalUDPDataReceiver.getInstance(this.context).startup();
            } else {
                Log.d(LocalUDPDataSender.TAG, "【IMCORE】数据发送失败, 错误码是：" + code + "！");
            }

            fireAfterSendLogin(code.intValue());
        }

        protected void fireAfterSendLogin(int code) {
            // default do nothing
        }
    }
}
