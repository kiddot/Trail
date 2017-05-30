package net.client.im.core;

import android.content.Context;

import net.client.im.ClientCoreSDK;

/**
 * Created by kiddo on 17-5-30.
 */

public class LocalUDPDataSender {
    private static final String TAG = LocalUDPDataSender.class.getSimpleName();
    private static LocalUDPDataSender instance = null;

    private Context context = null;

    public static LocalUDPDataSender getInstance(Context context)
    {
        if (instance == null)
            instance = new LocalUDPDataSender(context);
        return instance;
    }

    private LocalUDPDataSender(Context context)
    {
        this.context = context;
    }

    int sendKeepAlive()
    {
        byte[] b = ProtocalFactory.createPKeepAlive(ClientCoreSDK.getInstance().getCurrentUserId()).toBytes();
        return send(b, b.length);
    }

    int sendLogin(String loginName, String loginPsw, String extra)
    {
        byte[] b = ProtocalFactory.createPLoginInfo(loginName, loginPsw, extra).toBytes();
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
}
