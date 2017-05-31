package net.client.im.core;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import net.client.im.ClientCoreSDK;
import net.client.im.conf.ConfigClient;
import net.client.im.qos.QoS4SendDaemon;
import net.client.im.util.UDPUtils;
import net.server.im.protocal.ErrorCode;
import net.server.im.protocal.Protocol;
import net.server.im.protocal.ProtocolFactory;
import net.server.im.util.CharsetHelper;

import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by kiddo on 17-5-30.
 */

public class LocalUDPDataSender {
    private static final String TAG = LocalUDPDataSender.class.getSimpleName();
    private static LocalUDPDataSender instance = null;

    private Context context = null;

    public static LocalUDPDataSender getInstance(Context context) {
        if (instance == null)
            instance = new LocalUDPDataSender(context);
        return instance;
    }

    private LocalUDPDataSender(Context context) {
        this.context = context;
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

    private int send(byte[] fullProtocolBytes, int dataLen) {
        if (!ClientCoreSDK.getInstance().isInitialed())
            return ErrorCode.ForC.CLIENT_SDK_NO_INITIALED;

        if (!ClientCoreSDK.getInstance().isLocalDeviceNetworkOk()) {
            Log.e(TAG, "【IMCORE】本地网络不能工作，send数据没有继续!");
            return ErrorCode.ForC.LOCAL_NETWORK_NOT_WORKING;
        }

        DatagramSocket ds = LocalUDPSocketProvider.getInstance().getLocalUDPSocket();
        // 如果Socket没有连接上服务端
        if (ds != null && !ds.isConnected()) {
            try {
                if (ConfigClient.serverIP == null) {
                    Log.w(TAG, "【IMCORE】send数据没有继续，原因是ConfigEntity.server_ip==null!");
                    return ErrorCode.ForC.TO_SERVER_NET_INFO_NOT_SETUP;
                }

                // 即刻连接上服务端（如果不connect，即使在DataProgram中设置了远程id和地址则服务端MINA也收不到，跟普通的服
                // 务端UDP貌似不太一样，普通UDP时客户端无需先connect可以直接send设置好远程ip和端口的DataPragramPackage）
                //TODO : 此处进行了连接，只能与该地址端口通信
                ds.connect(InetAddress.getByName(ConfigClient.serverIP), ConfigClient.serverUDPPort);
            } catch (Exception e) {
                Log.w(TAG, "【IMCORE】send时出错，原因是：" + e.getMessage(), e);
                return ErrorCode.ForC.BAD_CONNECT_TO_SERVER;
            }
        }
        return UDPUtils.send(ds, fullProtocolBytes, dataLen) ? ErrorCode.COMMON_CODE_OK : ErrorCode.COMMON_DATA_SEND_FAILD;
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
                // 【【C2C或C2S模式下的QoS机制1/4步：将包加入到发送QoS队列中】】
                // 如果需要进行QoS质量保证，则把它放入质量保证队列中供处理(已在存在于列
                // 表中就不用再加了，已经存在则意味当前发送的这个是重传包哦)
                if (p.isQoS() && !QoS4SendDaemon.getInstance(context).exist(p.getFp()))
                    QoS4SendDaemon.getInstance(context).put(p);
            }
            return code;
        } else
            return ErrorCode.COMMON_INVALID_PROTOCAL;
    }
}
