package net.client.im.qos;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import net.client.im.ClientCoreSDK;
import net.server.im.protocol.Protocol;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kiddo on 17-5-31.
 */

/**
 * 用于暂时缓存接受到的数据报
 */
public class QoS4ReceiveDaemon {
    private static final String TAG = QoS4ReceiveDaemon.class.getSimpleName();
    public static final int CHECK_INTERVAL = 300000;
    public static final int MESSAGES_VALID_TIME = 600000;
    private ConcurrentHashMap<String, Long> mReceivedMessages = new ConcurrentHashMap();

    private Handler mHandler = null;
    private Runnable mRunnable = null;

    private boolean mRunning = false;

    private boolean mIsExecuting = false;

    private Context context = null;

    private static QoS4ReceiveDaemon instance = null;

    public static QoS4ReceiveDaemon getInstance(Context context) {
        if (instance == null) {
            instance = new QoS4ReceiveDaemon(context);
        }

        return instance;
    }

    public QoS4ReceiveDaemon(Context context) {
        this.context = context;
        init();
    }

    private void init() {
        this.mHandler = new Handler();
        this.mRunnable = new Runnable() {
            public void run() {
                // 极端情况下本次循环内可能执行时间超过了时间间隔，此处是防止在前一
                // 次还没有运行完的情况下又重复过劲行，从而出现无法预知的错误
                if (!QoS4ReceiveDaemon.this.mIsExecuting) {
                    QoS4ReceiveDaemon.this.mIsExecuting = true;

                    if (ClientCoreSDK.DEBUG) {
                        Log.d(QoS4ReceiveDaemon.TAG, "【IMCORE】【QoS接收方】++++++++++ START 暂存处理线程正在运行中，当前长度" + QoS4ReceiveDaemon.this.mReceivedMessages.size() + ".");
                    }

                    for (String key : QoS4ReceiveDaemon.this.mReceivedMessages.keySet()) {
                        long delta = System.currentTimeMillis() - ((Long) QoS4ReceiveDaemon.this.mReceivedMessages.get(key)).longValue();

                        if (delta < MESSAGES_VALID_TIME)
                            continue;
                        if (ClientCoreSDK.DEBUG)
                            Log.d(QoS4ReceiveDaemon.TAG, "【IMCORE】【QoS接收方】指纹为" + key + "的包已生存" + delta +
                                    "ms(最大允许" + MESSAGES_VALID_TIME + "ms), 马上将删除之.");
                        QoS4ReceiveDaemon.this.mReceivedMessages.remove(key);
                    }

                }

                if (ClientCoreSDK.DEBUG) {
                    Log.d(QoS4ReceiveDaemon.TAG, "【IMCORE】【QoS接收方】++++++++++ END 暂存处理线程正在运行中，当前长度" + QoS4ReceiveDaemon.this.mReceivedMessages.size() + ".");
                }

                QoS4ReceiveDaemon.this.mIsExecuting = false;

                QoS4ReceiveDaemon.this.mHandler.postDelayed(QoS4ReceiveDaemon.this.mRunnable, CHECK_INTERVAL);
            }
        };
    }

    public void startup(boolean immediately) {
        stop();

        if ((this.mReceivedMessages != null) && (this.mReceivedMessages.size() > 0)) {
            for (String key : this.mReceivedMessages.keySet()) {
                putImpl(key);
            }

        }

        this.mHandler.postDelayed(this.mRunnable, immediately ? 0 : CHECK_INTERVAL);

        this.mRunning = true;
    }

    public void stop() {
        this.mHandler.removeCallbacks(this.mRunnable);

        this.mRunning = false;
    }

    public boolean isRunning() {
        return this.mRunning;
    }

    public void addReceived(Protocol p) {
        if ((p != null) && (p.isQoS()))
            addReceived(p.getFp());
    }

    public void addReceived(String fingerPrintOfProtocal) {
        if (fingerPrintOfProtocal == null) {
            Log.w(TAG, "【IMCORE】无效的 fingerPrintOfProtocal==null!");
            return;
        }

        if (this.mReceivedMessages.containsKey(fingerPrintOfProtocal)) {
            Log.w(TAG, "【IMCORE】【QoS接收方】指纹为" + fingerPrintOfProtocal +
                    "的消息已经存在于接收列表中，该消息重复了（原理可能是对方因未收到应答包而错误重传导致），更新收到时间戳哦.");
        }

        putImpl(fingerPrintOfProtocal);
    }

    private void putImpl(String fingerPrintOfProtocol) {
        if (fingerPrintOfProtocol != null)
            this.mReceivedMessages.put(fingerPrintOfProtocol, Long.valueOf(System.currentTimeMillis()));
    }

    public boolean hasReceived(String fingerPrintOfProtocol) {
        return this.mReceivedMessages.containsKey(fingerPrintOfProtocol);
    }

    public int getSize() {
        return this.mReceivedMessages.size();
    }
}
