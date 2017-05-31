package net.client.im.event;

import net.server.im.protocal.Protocol;

import java.util.ArrayList;

/**
 * Created by kiddo on 17-5-31.
 */

public abstract interface MessageQoSEvent {
    public abstract void messagesLost(ArrayList<Protocol> paramArrayList);

    public abstract void messagesBeReceived(String paramString);
}
