package net.client.im.core;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import net.client.im.ClientCoreSDK;

import java.util.Observer;


/**
 * Created by kiddo on 17-5-30.
 */

public class KeepAliveDaemon {
    private static final String TAG = KeepAliveDaemon.class.getSimpleName();

    public static int NETWORK_CONNECTION_TIME_OUT = 10000;

    public static int KEEP_ALIVE_INTERVAL = 3000;

    private Handler mHandler = null;
    private Runnable mRunnable = null;
    private boolean mKeepAliveRunning = false;
    private long mLastGetKeepAliveResponseFromServerTimeStamp = 0L;

    private Observer mNetworkConnectionLostObserver = null;
    private boolean mIsExecuting = false;
    private Context mContext = null;

    private static KeepAliveDaemon instance = null;

    public static KeepAliveDaemon getInstance(Context context) {
        if (instance == null)
            instance = new KeepAliveDaemon(context);
        return instance;
    }

    private KeepAliveDaemon(Context context) {
        this.mContext = context;
        init();
    }

    private void init() {
        this.mHandler = new Handler();
        this.mRunnable = new Runnable() {
            public void run() {
                // 极端情况下本次循环内可能执行时间超过了时间间隔，此处是防止在前一
                // 次还没有运行完的情况下又重复过劲行，从而出现无法预知的错误
                if (!KeepAliveDaemon.this.mIsExecuting) {
                    new AsyncTask<Object, Integer, Integer>() {
                        private boolean willStop = false;

                        protected Integer doInBackground(Object[] params) {
                            KeepAliveDaemon.this.mIsExecuting = true;
                            if (ClientCoreSDK.DEBUG)
                                Log.d(KeepAliveDaemon.TAG, "【IMCORE】心跳线程执行中...");
                            int code = LocalUDPDataSender.getInstance(KeepAliveDaemon.this.mContext).sendKeepAlive();

                            return Integer.valueOf(code);
                        }

                        protected void onPostExecute(Integer code) {
                            boolean isInitialedForKeepAlive =
                                    KeepAliveDaemon.this.mLastGetKeepAliveResponseFromServerTimeStamp == 0L;
                            if ((code.intValue() == 0)
                                    && (KeepAliveDaemon.this.mLastGetKeepAliveResponseFromServerTimeStamp == 0L)) {
                                KeepAliveDaemon.this.mLastGetKeepAliveResponseFromServerTimeStamp = System.currentTimeMillis();
                            }

                            if (!isInitialedForKeepAlive) {
                                long now = System.currentTimeMillis();

                                // 当当前时间与最近一次服务端的心跳响应包时间间隔>= 10秒就判定当前与服务端的网络连接已断开
                                if (now - KeepAliveDaemon.this.mLastGetKeepAliveResponseFromServerTimeStamp
                                        >= KeepAliveDaemon.NETWORK_CONNECTION_TIME_OUT) {
                                    KeepAliveDaemon.this.stop();

                                    if (KeepAliveDaemon.this.mNetworkConnectionLostObserver != null) {
                                        KeepAliveDaemon.this.mNetworkConnectionLostObserver.update(null, null);
                                    }
                                    this.willStop = true;
                                }
                            }

                            KeepAliveDaemon.this.mIsExecuting = false;
                            if (!this.willStop) {
                                // 开始下一个心跳循环
                                KeepAliveDaemon.this.mHandler.postDelayed(
                                        KeepAliveDaemon.this.mRunnable
                                        , KeepAliveDaemon.KEEP_ALIVE_INTERVAL);
                            }
                        }
                    }
                            .execute(new Object[0]);
                }
            }
        };
    }

    public void stop() {
        this.mHandler.removeCallbacks(this.mRunnable);
        this.mKeepAliveRunning = false;
        this.mLastGetKeepAliveResponseFromServerTimeStamp = 0L;
    }

    public void start(boolean immediately) {
        stop();

        this.mHandler.postDelayed(this.mRunnable, immediately ? 0 : KEEP_ALIVE_INTERVAL);
        this.mKeepAliveRunning = true;
    }

    public boolean isKeepAliveRunning() {
        return this.mKeepAliveRunning;
    }

    public void updateGetKeepAliveResponseFromServerTimeStamp() {
        this.mLastGetKeepAliveResponseFromServerTimeStamp = System.currentTimeMillis();
    }

    public void setNetworkConnectionLostObserver(Observer networkConnectionLostObserver) {
        this.mNetworkConnectionLostObserver = networkConnectionLostObserver;
    }
}
