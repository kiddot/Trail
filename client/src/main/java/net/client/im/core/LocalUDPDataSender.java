package net.client.im.core;

import android.content.Context;
import android.util.Log;

import net.client.im.ClientCoreSDK;
import net.client.im.conf.ConfigClient;
import net.client.im.util.UDPUtils;
import net.server.im.protocal.ErrorCode;
import net.server.im.protocal.ProtocolFactory;

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

    private LocalUDPDataSender(Context context)
    {
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
        if(code == 0)
        {
            ClientCoreSDK.getInstance().setCurrentLoginName(loginName);
            ClientCoreSDK.getInstance().setCurrentLoginPsw(loginPsw);
            ClientCoreSDK.getInstance().setCurrentLoginExtra(extra);
        }

        return code;
    }

    private int send(byte[] fullProtocolBytes, int dataLen)
    {
        if(!ClientCoreSDK.getInstance().isInitialed())
            return ErrorCode.ForC.CLIENT_SDK_NO_INITIALED;

        if(!ClientCoreSDK.getInstance().isLocalDeviceNetworkOk())
        {
            Log.e(TAG, "【IMCORE】本地网络不能工作，send数据没有继续!");
            return ErrorCode.ForC.LOCAL_NETWORK_NOT_WORKING;
        }

        DatagramSocket ds = LocalUDPSocketProvider.getInstance().getLocalUDPSocket();
        // 如果Socket没有连接上服务端
        if(ds != null && !ds.isConnected())
        {
            try
            {
                if(ConfigClient.serverIP == null)
                {
                    Log.w(TAG, "【IMCORE】send数据没有继续，原因是ConfigEntity.server_ip==null!");
                    return ErrorCode.ForC.TO_SERVER_NET_INFO_NOT_SETUP;
                }

                // 即刻连接上服务端（如果不connect，即使在DataProgram中设置了远程id和地址则服务端MINA也收不到，跟普通的服
                // 务端UDP貌似不太一样，普通UDP时客户端无需先connect可以直接send设置好远程ip和端口的DataPragramPackage）
                //TODO : 此处进行了连接，只能与该地址端口通信
                ds.connect(InetAddress.getByName(ConfigClient.serverIP), ConfigClient.serverUDPPort);
            }
            catch (Exception e)
            {
                Log.w(TAG, "【IMCORE】send时出错，原因是："+e.getMessage(), e);
                return ErrorCode.ForC.BAD_CONNECT_TO_SERVER;
            }
        }
        return UDPUtils.send(ds, fullProtocolBytes, dataLen) ? ErrorCode.COMMON_CODE_OK : ErrorCode.COMMON_DATA_SEND_FAILD;
    }
}
