package net.client.im.event;

/**
 * Created by kiddo on 17-5-31.
 */

public abstract interface ChatBaseEvent {
    public abstract void onLoginMessage(int paramInt1, int paramInt2);

    public abstract void onLinkCloseMessage(int paramInt);
}