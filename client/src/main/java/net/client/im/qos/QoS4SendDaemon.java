package net.client.im.qos;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import net.client.im.ClientCoreSDK;
import net.client.im.core.LocalUDPDataSender;
import net.server.im.protocal.Protocol;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于暂时缓存发送过的数据报，用于重传等
 * <p>
 * Created by kiddo on 17-5-31.
 */

public class QoS4SendDaemon {
    private static final String TAG = QoS4SendDaemon.class.getSimpleName();

    // 并发Hash，因为本类中可能存在不同的线程同时remove或遍历之
    private ConcurrentHashMap<String, Protocol> sentMessages;
    // 关发Hash，因为本类中可能存在不同的线程同时remove或遍历之
    private ConcurrentHashMap<String, Long> sendMessagesTimeStamp;

    public static final int CHECK_INTERVAL = 5000;
    public static final int MESSAGES_JUST$NOW_TIME = 3000;
    public static final int QOS_TRY_COUNT = 3;

    private Handler handler = null;
    private Runnable runnable = null;
    private boolean running = false;
    private boolean mIsExecuting = false;
    private Context context = null;

    private static QoS4SendDaemon instance = null;

    public static QoS4SendDaemon getInstance(Context context) {
        if (instance == null) {
            instance = new QoS4SendDaemon(context);
        }
        return instance;
    }

    private QoS4SendDaemon(Context context) {
        this.context = context;
        sentMessages = new ConcurrentHashMap<>();
        sendMessagesTimeStamp = new ConcurrentHashMap<>();
        init();
    }

    private void init() {
        this.handler = new Handler();
        this.runnable = new Runnable() {
            public void run() {
                // 极端情况下本次循环内可能执行时间超过了时间间隔，此处是防止在前一
                // 次还没有运行完的情况下又重复执行，从而出现无法预知的错误
                if (!QoS4SendDaemon.this.mIsExecuting) {
                    new AsyncTask<Object, ArrayList<Protocol>, ArrayList<Protocol>>() {
                        private ArrayList<Protocol> lostMessages = new ArrayList<Protocol>();

                        protected ArrayList<Protocol> doInBackground(Object[] params) {
                            QoS4SendDaemon.this.mIsExecuting = true;
                            try {
                                if (ClientCoreSDK.DEBUG) {
                                    Log.d(QoS4SendDaemon.TAG
                                            , "【IMCORE】【QoS】=========== 消息发送质量保证线程运行中" +
                                                    ", 当前需要处理的列表长度为"
                                                    + QoS4SendDaemon.this.sentMessages.size() + "...");
                                }

                                for (String key : QoS4SendDaemon.this.sentMessages.keySet()) {
                                    Protocol p = (Protocol) QoS4SendDaemon.this.sentMessages.get(key);
                                    if ((p != null) && (p.isQoS())) {
                                        if (p.getRetryCount() >= QOS_TRY_COUNT) {
                                            if (ClientCoreSDK.DEBUG) {
                                                Log.d(QoS4SendDaemon.TAG
                                                        , "【IMCORE】【QoS】指纹为" + p.getFp() +
                                                                "的消息包重传次数已达" + p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次)上限，将判定为丢包！");
                                            }

                                            this.lostMessages.add((Protocol) p.clone());
                                            QoS4SendDaemon.this.remove(p.getFp());
                                        } else {
                                            long delta = System.currentTimeMillis() - ((Long) QoS4SendDaemon.this.sendMessagesTimeStamp.get(key)).longValue();

                                            if (delta <= MESSAGES_JUST$NOW_TIME) {
                                                if (ClientCoreSDK.DEBUG) {
                                                    Log.w(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为"
                                                            + key + "的包距\"刚刚\"发出才" + delta
                                                            + "ms(<=" + MESSAGES_JUST$NOW_TIME
                                                            + "ms将被认定是\"刚刚\"), 本次不需要重传哦.");
                                                }
                                            } else {
//                                                new LocalUDPDataSender.SendCommonDataAsync(QoS4SendDaemon.this.context, p) {
//                                                    protected void onPostExecute(Integer code) {
//                                                        if (code.intValue() == 0) {
//                                                            this.p.increaseRetryCount();
//
//                                                            if (ClientCoreSDK.DEBUG)
//                                                                Log.d(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为" + this.p.getFp() +
//                                                                        "的消息包已成功进行重传，此次之后重传次数已达" +
//                                                                        this.p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次).");
//                                                        } else {
//                                                            Log.w(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为" + this.p.getFp() +
//                                                                    "的消息包重传失败，它的重传次数之前已累计为" +
//                                                                    this.p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次).");
//                                                        }
//                                                    }
//                                                }.execute(new Object[0]);
                                                int code = LocalUDPDataSender.getInstance(context).sendCommonData(p);
                                                if(code == 0) {
                                                    p.increaseRetryCount();

                                                    if (ClientCoreSDK.DEBUG)
                                                        Log.d(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为" + p.getFp() +
                                                                "的消息包已成功进行重传，此次之后重传次数已达" +
                                                                p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次).");
                                                }else{
                                                    Log.w(QoS4SendDaemon.TAG, "【IMCORE】【QoS】指纹为" + p.getFp() +
                                                            "的消息包重传失败，它的重传次数之前已累计为" +
                                                            p.getRetryCount() + "(最多" + QOS_TRY_COUNT + "次).");
                                                }
                                            }
                                        }
                                    } else {
                                        QoS4SendDaemon.this.remove(key);
                                    }
                                }
                            } catch (Exception e) {
                                Log.w(QoS4SendDaemon.TAG, "【IMCORE】【QoS】消息发送质量保证线程运行时发生异常," + e.getMessage(), e);
                            }

                            return this.lostMessages;
                        }

                        @Override
                        protected void onPostExecute(ArrayList<Protocol> al) {
                            if ((al != null) && (al.size() > 0)) {
                                QoS4SendDaemon.this.notifyMessageLost(al);
                            }

                            QoS4SendDaemon.this.mIsExecuting = false;
                            QoS4SendDaemon.this.handler.postDelayed(QoS4SendDaemon.this.runnable, 5000L);
                        }
                    }.execute();
                }
            }
        };
    }

    protected void notifyMessageLost(ArrayList<Protocol> lostMessages) {
        if (ClientCoreSDK.getInstance().getMessageQoSEvent() != null)
            ClientCoreSDK.getInstance().getMessageQoSEvent().messagesLost(lostMessages);
    }

    public void startup(boolean immediately) {
        stop();

        this.handler.postDelayed(this.runnable, immediately ? 0 : CHECK_INTERVAL);
        this.running = true;
    }

    public void stop() {
        this.handler.removeCallbacks(this.runnable);
        this.running = false;
    }

    public boolean isRunning() {
        return this.running;
    }

    boolean exist(String fingerPrint) {
        return this.sentMessages.get(fingerPrint) != null;
    }

    public void put(Protocol p) {
        if (p == null) {
            Log.w(TAG, "Invalid arg p==null.");
            return;
        }
        if (p.getFp() == null) {
            Log.w(TAG, "Invalid arg p.getFp() == null.");
            return;
        }

        if (!p.isQoS()) {
            Log.w(TAG, "This protocal is not QoS pkg, ignore it!");
            return;
        }

        if (this.sentMessages.get(p.getFp()) != null) {
            Log.w(TAG, "【IMCORE】【QoS】指纹为" + p.getFp() + "的消息已经放入了发送质量保证队列，该消息为何会重复？（生成的指纹码重复？还是重复put？）");
        }

        // save it
        sentMessages.put(p.getFp(), p);
        // 同时保存时间戳
        sendMessagesTimeStamp.put(p.getFp(), System.currentTimeMillis());
    }

    public void remove(final String fingerPrint) {
        new AsyncTask<Object, Void, Object>() {
            @Override
            protected Object doInBackground(Object... params) {
                sendMessagesTimeStamp.remove(fingerPrint);
                return sentMessages.remove(fingerPrint);
            }

            protected void onPostExecute(Object result) {
                Log.w(TAG, "【IMCORE】【QoS】指纹为" + fingerPrint + "的消息已成功从发送质量保证队列中移除(可能是收到接收方的应答也可能是达到了重传的次数上限)，重试次数="
                        + (result != null ? ((Protocol) result).getRetryCount() : "none呵呵."));
            }
        }.execute();
    }

    public int size() {
        return this.sentMessages.size();
    }
}
