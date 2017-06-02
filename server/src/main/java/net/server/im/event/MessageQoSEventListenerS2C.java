package net.server.im.event;

import net.server.im.protocol.Protocol;

import java.util.ArrayList;

/**
 * Created by kiddo on 17-5-31.
 */

public abstract interface MessageQoSEventListenerS2C {
    public abstract void messagesLost(ArrayList<Protocol> paramArrayList);

    public abstract void messagesBeReceived(String paramString);
}
