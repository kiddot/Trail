package net.client.im.event;

/**
 * Created by kiddo on 17-5-31.
 */

public abstract interface ChatTransDataEvent {
    public abstract void onTransBuffer(String paramString1, int paramInt, String paramString2);

    public abstract void onErrorResponse(int paramInt, String paramString);
}