package net.client.im.core;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.client.im.ClientCoreSDK;
import net.client.im.conf.ConfigClient;
import net.client.im.qos.QoS4ReceiveDaemon;
import net.client.im.qos.QoS4SendDaemon;
import net.server.im.protocol.Protocol;
import net.server.im.protocol.ProtocolFactory;
import net.server.im.protocol.ProtocolType;
import net.server.im.protocol.server.PErrorResponse;
import net.server.im.protocol.server.PLoginInfoResponse;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by kiddo on 17-5-31.
 */

public class LocalUDPDataReceiver {
    private static final String TAG = LocalUDPDataReceiver.class.getSimpleName();

    private Thread thread = null;

    private static LocalUDPDataReceiver instance = null;

    private static MessageHandler messageHandler = null;

    private Context context = null;

    public static LocalUDPDataReceiver getInstance(Context context) {
        if (instance == null) {
            instance = new LocalUDPDataReceiver(context);
            messageHandler = new MessageHandler(context);
        }
        return instance;
    }

    private LocalUDPDataReceiver(Context context) {
        this.context = context;
    }

    public void stop() {
        if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
        }
    }

    public void startup() {
        stop();
        try {
            this.thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        if (ClientCoreSDK.DEBUG) {
                            Log.d(TAG, "【IMCORE】本地UDP端口侦听中，端口=" + ConfigClient.localUDPPort + "...");
                        }

                        //开始侦听
                        p2pListeningImpl();
                    } catch (Exception e) {
                        Log.w(TAG, "【IMCORE】本地UDP监听停止了(socket被关闭了?)," + e.getMessage(), e);
                    }
                }
            });
            this.thread.start();
        } catch (Exception e) {
            Log.w(TAG, "【IMCORE】本地UDPSocket监听开启时发生异常," + e.getMessage(), e);
        }
    }

    private void p2pListeningImpl() throws Exception {
        while (true) {
            // 缓冲区
            byte[] data = new byte[1024];
            // 接收数据报的包
            DatagramPacket packet = new DatagramPacket(data, data.length);
            DatagramSocket localUDPSocket = LocalUDPSocketProvider.getInstance().getLocalUDPSocket();
            if ((localUDPSocket == null) || (localUDPSocket.isClosed())) {
                continue;
            }


            Log.d(TAG, "p2pListeningImpl: 接收到一个udp数据报");
            localUDPSocket.receive(packet);

            if (!packet.getAddress().getHostAddress().equals(InetAddress.getLocalHost().getHostAddress())){
                Log.d(TAG, "p2pListeningImpl: packet.getAddress().getHostAddress():"+packet.getAddress().getHostAddress()
                    +",InetAddress.getLocalHost().getHostAddress():"+InetAddress.getLocalHost().getHostAddress());
            }

            Message m = Message.obtain();
            m.obj = packet;
            messageHandler.sendMessage(m);
            Log.d(TAG, "p2pListeningImpl: 准备派发这个udp数据报");
        }
    }

    private static class MessageHandler extends Handler {
        private Context context = null;

        public MessageHandler(Context context) {
            this.context = context;
        }

        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: 成功接受到数据报的处理消息，开始处理");
            DatagramPacket packet = (DatagramPacket) msg.obj;
            if (packet == null) {
                Log.d(TAG, "handleMessage: 很遗憾获取的数据报为空");
                return;
            }

            try {
                Protocol pFromServer =
                        ProtocolFactory.parse(packet.getData(), packet.getLength());

                Log.d(TAG, "handleMessage: " + pFromServer.toString());
                if (pFromServer.isQoS()) {
                    //重复数据报检测
                    if (QoS4ReceiveDaemon.getInstance(this.context).hasReceived(pFromServer.getFp())) {
                        if (ClientCoreSDK.DEBUG) {
                            Log.d(TAG, "【IMCORE】【QoS机制】" + pFromServer.getFp() + "已经存在于发送列表中，这是重复包，通知应用层收到该包罗！");
                        }
                        QoS4ReceiveDaemon.getInstance(this.context).addReceived(pFromServer);
                        sendReceivedBack(pFromServer);

                        return;
                    }

                    QoS4ReceiveDaemon.getInstance(this.context).addReceived(pFromServer);

                    sendReceivedBack(pFromServer);
                }

                switch (pFromServer.getType()) {
                    case ProtocolType.C.FROM_CLIENT_TYPE_OF_COMMON$DATA: {
                        if (ClientCoreSDK.getInstance().getChatTransDataEvent() == null)
                            break;
                        ClientCoreSDK.getInstance().getChatTransDataEvent().onTransBuffer(
                                pFromServer.getFp(), pFromServer.getFrom(), pFromServer.getDataContent());

                        break;
                    }
                    case ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$KEEP$ALIVE: {
                        if (ClientCoreSDK.DEBUG) {
                            Log.d(TAG, "【IMCORE】收到服务端回过来的Keep Alive心跳响应包.");
                        }
                        KeepAliveDaemon.getInstance(this.context).updateGetKeepAliveResponseFromServerTimeStamp();
                        break;
                    }
                    case ProtocolType.C.FROM_CLIENT_TYPE_OF_RECEIVED: {
                        String theFingerPrint = pFromServer.getDataContent();
                        if (ClientCoreSDK.DEBUG) {
                            Log.d(TAG, "【IMCORE】【QoS】收到" + pFromServer.getFrom() + "发过来的指纹为" + theFingerPrint + "的应答包.");
                        }

                        if (ClientCoreSDK.getInstance().getMessageQoSEvent() != null) {
                            ClientCoreSDK.getInstance().getMessageQoSEvent().messagesBeReceived(theFingerPrint);
                        }

                        QoS4SendDaemon.getInstance(this.context).remove(theFingerPrint);
                        break;
                    }
                    case ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$LOGIN: {
                        Log.d(TAG, "handleMessage: 接收到了登录反馈回来的数据报");
                        PLoginInfoResponse loginInfoRes = ProtocolFactory.parsePLoginInfoResponse(pFromServer.getDataContent());

                        if (loginInfoRes.getCode() == 0) {
                            ClientCoreSDK.getInstance()
                                    .setLoginHasInit(true)
                                    .setCurrentUserId(loginInfoRes.getUser_id());
                            AutoReLoginDaemon.getInstance(context).stop();
                            KeepAliveDaemon.getInstance(context).setNetworkConnectionLostObserver(new Observer() {
                                public void update(Observable observable, Object data) {
                                    QoS4SendDaemon.getInstance(LocalUDPDataReceiver.MessageHandler.this.context).stop();
                                    QoS4ReceiveDaemon.getInstance(LocalUDPDataReceiver.MessageHandler.this.context).stop();
                                    ClientCoreSDK.getInstance().setConnectedToServer(false);
                                    ClientCoreSDK.getInstance().setCurrentUserId(-1);
                                    ClientCoreSDK.getInstance().getChatBaseEvent().onLinkCloseMessage(-1);
                                    AutoReLoginDaemon.getInstance(context).start(true);
                                }
                            });
                            KeepAliveDaemon.getInstance(context).start(false);
                            QoS4SendDaemon.getInstance(context).startup(true);
                            QoS4ReceiveDaemon.getInstance(context).startup(true);
                            ClientCoreSDK.getInstance().setConnectedToServer(true);
                        } else {
                            ClientCoreSDK.getInstance().setConnectedToServer(false);
                            ClientCoreSDK.getInstance().setCurrentUserId(-1);
                        }

                        if (ClientCoreSDK.getInstance().getChatBaseEvent() == null)
                            break;
                        ClientCoreSDK.getInstance().getChatBaseEvent().onLoginMessage(
                                loginInfoRes.getUser_id(), loginInfoRes.getCode());
                        break;
                    }
                    case ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR: {
                        PErrorResponse errorRes = ProtocolFactory.parsePErrorResponse(pFromServer.getDataContent());

                        Log.d(TAG, "handleMessage: FROM_SERVER_TYPE_OF_RESPONSE$FOR$ERROR" + errorRes);
                        if (errorRes.getErrorCode() == 301) {
                            ClientCoreSDK.getInstance().setLoginHasInit(false);
                            Log.e(TAG, "【IMCORE】收到服务端的“尚未登陆”的错误消息，心跳线程将停止，请应用层重新登陆.");
                            KeepAliveDaemon.getInstance(this.context).stop();
                            AutoReLoginDaemon.getInstance(this.context).start(false);
                        }

                        if (ClientCoreSDK.getInstance().getChatTransDataEvent() == null)
                            break;
                        ClientCoreSDK.getInstance().getChatTransDataEvent().onErrorResponse(
                                errorRes.getErrorCode(), errorRes.getErrorMsg());

                        break;
                    }
                    case ProtocolType.S.FROM_SERVER_TYPE_OF_RESPONSE$NAT:{
                        Log.d(TAG, "handleMessage: 收到打洞的数据报");
                        String remoteAddressB = pFromServer.getDataContent();
                        //进行地址ip和端口号的分解
                        Log.d(TAG, "handleMessage: remoteAddressB" + remoteAddressB);
                        String[] temp = remoteAddressB.split("/");
                        String[] address = temp[1].split(":");
                        String ip = address[0];
                        String addressPort = address[1];
                        int port = Integer.valueOf(addressPort);
                        //准备向B发送打洞包 ps：此包不一定能够成功被B接收到，但是起到了打开洞口的重要作用
                        Log.d(TAG, "handleMessage: p2p" + ip + addressPort);
                        LocalUDPDataSender.getInstance(context).sendChatP2P(ip, port);
                        Log.d(TAG, "handleMessage: 发送打洞数据报给B");
                    }
                    case ProtocolType.C.FROM_CLIENT_TYPE_OF_NAT:{
                        Log.d(TAG, "handleMessage: 收到了由客户端发过来的数据报");
                    }
                    default:
                        Log.w(TAG, "【IMCORE】收到的服务端消息类型：" + pFromServer.getType() + "，但目前该类型客户端不支持解析和处理！");
                }
            } catch (Exception e) {
                Log.w(TAG, "【IMCORE】处理消息的过程中发生了错误.", e);
            }
        }

        private void sendReceivedBack(final Protocol pFromServer) {
            if (pFromServer.getFp() != null) {
                new LocalUDPDataSender.SendCommonDataAsync(
                        context
                        , ProtocolFactory.createReceivedBack(
                        pFromServer.getTo()
                        , pFromServer.getFrom()
                        , pFromServer.getFp())) {
                    @Override
                    protected void onPostExecute(Integer code) {
                        if (ClientCoreSDK.DEBUG)
                            Log.d(TAG, "【IMCORE】【QoS】向" + pFromServer.getFrom() + "发送" + pFromServer.getFp() + "包的应答包成功,from=" + pFromServer.getTo() + "！");
                    }
                }.execute();
            } else {
                Log.w(TAG, "【IMCORE】【QoS】收到" + pFromServer.getFrom() + "发过来需要QoS的包，但它的指纹码却为null！无法发应答包！");
            }
        }
    }
}
