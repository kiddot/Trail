package net.server.im.protocal;


import com.google.gson.Gson;

import net.server.im.protocal.client.PKeepAlive;
import net.server.im.protocal.client.PLoginInfo;
import net.server.im.protocal.server.PErrorResponse;
import net.server.im.protocal.server.PKeepAliveResponse;
import net.server.im.protocal.server.PLoginInfoResponse;
import net.server.im.util.CharsetHelper;

/**
 * Created by kiddo on 17-5-30.
 */

public class ProtocolFactory {
    private static String create(Object c)
    {
        return new Gson().toJson(c);
    }

    public static <T> T parse(byte[] fullProtocolJASOnBytes, int len, Class<T> clazz)
    {
        return parse(CharsetHelper.getString(fullProtocolJASOnBytes, len), clazz);
    }

    public static <T> T parse(String dataContentOfProtocal, Class<T> clazz)
    {
        return new Gson().fromJson(dataContentOfProtocal, clazz);
    }

    public static Protocol parse(byte[] fullProtocolJASOnBytes, int len)
    {
        return (Protocol)parse(fullProtocolJASOnBytes, len, Protocol.class);
    }

    public static Protocol createPKeepAliveResponse(int to_user_id)
    {
        return new Protocol(ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$KEEP$ALIVE,
                create(new PKeepAliveResponse()), 0, to_user_id);
    }

    public static PKeepAliveResponse parsePKeepAliveResponse(String dataContentOfProtocol)
    {
        return (PKeepAliveResponse)parse(dataContentOfProtocol, PKeepAliveResponse.class);
    }

    public static Protocol createPKeepAlive(int from_user_id)
    {
        return new Protocol(ProtocolType.C.FROM_CLIENT_TYPE_OF_KEEP$ALIVE,
                create(new PKeepAlive()), from_user_id, 0);
    }

    public static PKeepAlive parsePKeepAlive(String dataContentOfProtocol)
    {
        return (PKeepAlive)parse(dataContentOfProtocol, PKeepAlive.class);
    }

    public static Protocol createPErrorResponse(int errorCode, String errorMsg, int user_id)
    {
        return new Protocol(ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR,
                create(new PErrorResponse(errorCode, errorMsg)), 0, user_id);
    }

    public static PErrorResponse parsePErrorResponse(String dataContentOfProtocol)
    {
        return (PErrorResponse)parse(dataContentOfProtocol, PErrorResponse.class);
    }

    public static Protocol createPLoginoutInfo(int user_id, String loginName)
    {
        return new Protocol(ProtocolType.C.FROM_CLIENT_TYPE_OF_LOGOUT
//				, create(new PLogoutInfo(user_id, loginName))
                , null
                , user_id, 0);
    }

    public static Protocol createPLoginInfo(String loginName, String loginPsw, String extra)
    {
        return new Protocol(ProtocolType.C.FROM_CLIENT_TYPE_OF_LOGIN
                , create(new PLoginInfo(loginName, loginPsw, extra)), -1, 0);
    }

    public static PLoginInfo parsePLoginInfo(String dataContentOfProtocol)
    {
        return (PLoginInfo)parse(dataContentOfProtocol, PLoginInfo.class);
    }

    public static Protocol createPLoginInfoResponse(int code, int user_id)
    {
        return new Protocol(ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$LOGIN,
                create(new PLoginInfoResponse(code, user_id)),
                0,
                user_id,
                true, Protocol.genFingerPrint());
    }

    public static PLoginInfoResponse parsePLoginInfoResponse(String dataContentOfProtocol)
    {
        return (PLoginInfoResponse)parse(dataContentOfProtocol, PLoginInfoResponse.class);
    }

    public static Protocol createCommonData(String dataContent, int from_user_id, int to_user_id, boolean QoS, String fingerPrint)
    {
        return new Protocol(ProtocolType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA,
                dataContent, from_user_id, to_user_id, QoS, fingerPrint);
    }

    public static Protocol createCommonData(String dataContent, int from_user_id, int to_user_id)
    {
        return new Protocol(ProtocolType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA,
                dataContent, from_user_id, to_user_id);
    }

    public static Protocol createRecivedBack(int from_user_id, int to_user_id, String recievedMessageFingerPrint)
    {
        return new Protocol(ProtocolType.C.FROM_CLIENT_TYPE_OF_RECEIVED
                , recievedMessageFingerPrint, from_user_id, to_user_id);// 该包当然不需要QoS支持！
    }
}
