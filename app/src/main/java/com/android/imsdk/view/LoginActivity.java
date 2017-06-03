package com.android.imsdk.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.imsdk.R;

import net.client.im.core.LocalUDPDataSender;

import java.util.Observer;

/**
 * Created by kiddo on 17-6-3.
 */

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText editServerIp = null;
    private EditText editServerPort = null;

    private EditText editLoginName = null;
    private EditText editLoginPsw = null;
    private Button btnLogin = null;
    private TextView viewVersion = null;
    /** 登陆进度提示 */
    private OnLoginProgress onLoginProgress = null;
    /** 收到服务端的登陆完成反馈时要通知的观察者（因登陆是异步实现，本观察者将由
     *  ChatBaseEvent 事件的处理者在收到服务端的登陆反馈后通知之） */
    private Observer onLoginSucessObserver = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    /**
     * 真正的登陆信息发送实现方法。
     */
    private void doLoginImpl()
    {
        // * 立即显示登陆处理进度提示（并将同时启动超时检查线程）
        onLoginProgress.showProgressing(true);
        // * 设置好服务端反馈的登陆结果观察者（当客户端收到服务端反馈过来的登陆消息时将被通知）
        IMClientManager.getInstance(this).getBaseEventListener()
                .setLoginOkForLaunchObserver(onLoginSucessObserver);

        // 异步提交登陆名和密码
        new LocalUDPDataSender.SendLoginDataAsync(
                LoginActivity.this
                , editLoginName.getText().toString().trim()
                , editLoginPsw.getText().toString().trim())
        {
            /**
             * 登陆信息发送完成后将调用本方法（注意：此处仅是登陆信息发送完成
             * ，真正的登陆结果要在异步回调中处理哦）。
             *
             * @param code 数据发送返回码，0 表示数据成功发出，否则是错误码
             */
            @Override
            protected void fireAfterSendLogin(int code)
            {
                if(code == 0)
                {
                    //
                    Toast.makeText(getApplicationContext(), "数据发送成功！", Toast.LENGTH_SHORT).show();
                    Log.d(MainActivity.class.getSimpleName(), "登陆信息已成功发出！");
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "数据发送失败。错误码是："+code+"！", Toast.LENGTH_SHORT).show();

                    // * 登陆信息没有成功发出时当然无条件取消显示登陆进度条
                    onLoginProgress.showProgressing(false);
                }
            }
        }.execute();
    }

    /**
     * 获取APP版本信息.
     */
    private String getProgrammVersion()
    {
        PackageInfo info;
        try
        {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
//			versionCode = info.versionCode;
//			versionName = info.versionName;
            return info.versionName;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.w(TAG, "读程序版本信息时出错,"+e.getMessage(),e);
            return "N/A";
        }
    }

    private boolean CheckNetworkState()
    {
        boolean flag = false;
        ConnectivityManager manager = (ConnectivityManager)getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if(manager.getActiveNetworkInfo() != null)
        {
            flag = manager.getActiveNetworkInfo().isAvailable();
        }
        if(!flag)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle("Network not avaliable");//
            builder.setMessage("Current network is not avaliable, set it?");//
            builder.setPositiveButton("Setting", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); //直接进入手机中的wifi网络设置界面
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.create();
            builder.show();
        }

        return flag;
    }

    /**
     * 登陆进度提示和超时检测封装实现类.
     */
    private class OnLoginProgress
    {
        /** 登陆的超时时间定义 */
        private final static int RETRY_DELAY = 6000;

        private Handler handler = null;
        private Runnable runnable = null;
        // 重试时要通知的观察者
        private Observer retryObsrver = null;

        private ProgressDialog progressDialogForPairing = null;
        private Activity parentActivity = null;

        public OnLoginProgress(Activity parentActivity)
        {
            this.parentActivity = parentActivity;
            init();
        }

        private void init()
        {
            progressDialogForPairing = new ProgressDialog(parentActivity);
            progressDialogForPairing.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialogForPairing.setTitle("登陆中");
            progressDialogForPairing.setMessage("正在登陆中，请稍候。。。");
            progressDialogForPairing.setCanceledOnTouchOutside(false);

            handler = new Handler();
            runnable = new Runnable(){
                @Override
                public void run()
                {
                    onTimeout();
                }
            };
        }

        /**
         * 登陆超时后要调用的方法。
         */
        private void onTimeout()
        {
            // 本观察者中由用户选择是否重试登陆或者取消登陆重试
            new AlertDialog.Builder(LoginActivity.this)
                    .setTitle("超时了")
                    .setMessage("登陆超时，可能是网络故障或服务器无法连接，是否重试？")
                    .setPositiveButton("重试！", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog,int which)
                        {
                            // 确认要重试时（再次尝试登陆哦）
                            doLogin();
                        }
                    })
                    .setNegativeButton("取消" , new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog,int which)
                        {
                            // 不需要重试则要停止“登陆中”的进度提示哦
                            OnLoginProgress.this.showProgressing(false);
                        }
                    })
                    .show();
        }

        /**
         * 显示进度提示.
         *
         * @param show
         */
        public void showProgressing(boolean show)
        {
            // 显示进度提示的同时即启动超时提醒线程
            if(show)
            {
                showLoginProgressGUI(true);

                // 先无论如何保证利重试检测线程在启动前肯定是处于停止状态
                handler.removeCallbacks(runnable);
                // 启动
                handler.postDelayed(runnable, RETRY_DELAY);
            }
            // 关闭进度提示
            else
            {
                // 无条件停掉延迟重试任务
                handler.removeCallbacks(runnable);

                showLoginProgressGUI(false);
            }
        }

        /**
         * 进度提示时要显示或取消显示的GUI内容。
         *
         * @param show true表示显示gui内容，否则表示结速gui内容显示
         */
        private void showLoginProgressGUI(boolean show)
        {
            // 显示登陆提示信息
            if(show)
            {
                try{
                    if(parentActivity != null && !parentActivity.isFinishing())
                        progressDialogForPairing.show();
                }
                catch (WindowManager.BadTokenException e){
                    Log.e(TAG, e.getMessage(), e);
                }
            }
            // 关闭登陆提示信息
            else
            {
                // 此if语句是为了保证延迟线程里不会因Activity已被关闭而此处却要非法地执行show的情况（此判断可趁为安全的show方法哦！）
                if(parentActivity != null && !parentActivity.isFinishing())
                    progressDialogForPairing.dismiss();
            }
        }
    }
}
