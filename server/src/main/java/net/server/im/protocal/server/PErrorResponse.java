package net.server.im.protocal.server;

/**
 * Created by kiddo on 17-5-30.
 */

public class PErrorResponse {
    private int errorCode = -1;
    private String errorMsg = null;

    public PErrorResponse(int errorCode, String errorMsg)
    {
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public int getErrorCode()
    {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
    }

    public String getErrorMsg()
    {
        return this.errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }
}
