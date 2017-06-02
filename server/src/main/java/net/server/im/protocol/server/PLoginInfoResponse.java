package net.server.im.protocol.server;

/**
 * Created by kiddo on 17-5-30.
 */

public class PLoginInfoResponse {
    private int code = 0;
    private int user_id = -1;

    public PLoginInfoResponse(int code, int user_id)
    {
        this.code = code;
        this.user_id = user_id;
    }

    public int getCode()
    {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getUser_id()
    {
        return this.user_id;
    }

    public void setUser_id(int user_id)
    {
        this.user_id = user_id;
    }
}
