package net.server.im.event;

import org.apache.mina.core.session.IoSession;

/**
 * Created by kiddo on 17-5-31.
 */

public abstract interface ServerEventListener {
    public abstract int onVerifyUserCallBack(String paramString1, String paramString2, String extra);

    public abstract void onUserLoginAction_CallBack(int paramInt, String paramString, IoSession paramIoSession);

    public abstract void onUserLogoutAction_CallBack(int paramInt, Object paramObject);

    public abstract boolean onTransBuffer_CallBack(int paramInt1, int paramInt2, String paramString1, String paramString2);

    public abstract void onTransBuffer_C2C_CallBack(int paramInt1, int paramInt2, String paramString);

    public abstract boolean onTransBuffer_C2C_RealTimeSendFaild_CallBack(int paramInt1, int paramInt2, String paramString1, String paramString2);
}
