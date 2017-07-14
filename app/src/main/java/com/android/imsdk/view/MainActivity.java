package com.android.imsdk.view;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.imsdk.R;
import com.android.imsdk.core.IMClientManager;

import net.client.im.ClientCoreSDK;
import net.client.im.core.LocalUDPDataSender;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kiddo on 17-6-3.
 */

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private Button btnLogout = null;

    private EditText editId = null;
    private EditText editContent = null;
    private TextView viewMyid = null;
    private Button btnSend = null;
    private Button btnOnline = null;

    private ListView chatInfoListView;
    private MyAdapter chatInfoListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initListeners();
        initOthers();
    }

    /**
     * 捕获back键
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        doLogout();
        // 退出程序
        doExit();
    }

    @Override
    protected void onDestroy() {
        // 释放IM占用资源
        IMClientManager.getInstance(this).release();
        super.onDestroy();
    }

    private void initViews() {
        btnLogout = (Button) this.findViewById(R.id.logout_btn);
        btnOnline = (Button) this.findViewById(R.id.online);
        btnSend = (Button) this.findViewById(R.id.send_btn);
        editId = (EditText) this.findViewById(R.id.id_editText);
        editContent = (EditText) this.findViewById(R.id.content_editText);
        viewMyid = (TextView) this.findViewById(R.id.myid_view);

        chatInfoListView = (ListView) this.findViewById(R.id.demo_main_activity_layout_listView);
        chatInfoListAdapter = new MyAdapter(this);
        chatInfoListView.setAdapter(chatInfoListAdapter);

        this.setTitle("MobileIMSDK Demo");
    }

    private void initListeners() {
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 退出登陆
                doLogout();
                // 退出程序
                doExit();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSendMessage();
            }
        });

        btnOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnline();
            }
        });
    }

    private void getOnline(){
        //LocalUDPDataSender.getInstance(this).sendOnline();
    }

    private void doSendMessage() {
        String msg = editContent.getText().toString().trim();
        if (msg.length() > 0) {
            int friendId = Integer.parseInt(editId.getText().toString().trim());
            showIMInfo_black("我对" + friendId + "说：" + msg);

            // 发送消息（Android系统要求必须要在独立的线程中发送哦）
            new LocalUDPDataSender.SendCommonDataAsync(MainActivity.this, msg, friendId, true) {
                @Override
                protected void onPostExecute(Integer code) {
                    if (code == 0)
                        Log.d(MainActivity.class.getSimpleName(), "2数据已成功发出！");
                    else
                        Toast.makeText(getApplicationContext(), "数据发送失败。错误码是：" + code + "！", Toast.LENGTH_SHORT).show();
                }
            }.execute();
        } else
            Log.e(MainActivity.class.getSimpleName(), "txt2.len=" + (msg.length()));
    }

    private void initOthers() {
        // Refresh userId to show
        refreshMyid();

        // Set MainGUI instance refrence to listeners
        IMClientManager.getInstance(this).getTransDataListener().setMainGUI(this);
        IMClientManager.getInstance(this).getBaseEventListener().setMainGUI(this);
        IMClientManager.getInstance(this).getMessageQoSListener().setMainGUI(this);
    }


    public void refreshMyid() {
        int myid = ClientCoreSDK.getInstance().getCurrentUserId();
        this.viewMyid.setText(myid == -1 ? "连接断开" : "" + myid);
    }

    private void doLogout() {
        // 发出退出登陆请求包（Android系统要求必须要在独立的线程中发送哦）
        new AsyncTask<Object, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Object... params) {
                int code = -1;
                try {
                    code = LocalUDPDataSender.getInstance(MainActivity.this).sendLoginOut();
                } catch (Exception e) {
                    Log.w(TAG, e);
                }

                return code;
            }

            @Override
            protected void onPostExecute(Integer code) {
                refreshMyid();
                if (code == 0)
                    Log.d(MainActivity.class.getSimpleName(), "注销登陆请求已完成！");
                else
                    Toast.makeText(getApplicationContext(), "注销登陆请求发送失败。错误码是：" + code + "！", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void doExit() {
        finish();
        System.exit(0);
    }

    //--------------------------------------------------------------- 各种信息输出方法 START
    public void showIMInfo_black(String txt) {
        chatInfoListAdapter.addItem(txt, ChatInfoColorType.black);
    }

    public void showIMInfo_blue(String txt) {
        chatInfoListAdapter.addItem(txt, ChatInfoColorType.blue);
    }

    public void showIMInfo_brightred(String txt) {
        chatInfoListAdapter.addItem(txt, ChatInfoColorType.brightred);
    }

    public void showIMInfo_red(String txt) {
        chatInfoListAdapter.addItem(txt, ChatInfoColorType.red);
    }

    public void showIMInfo_green(String txt) {
        chatInfoListAdapter.addItem(txt, ChatInfoColorType.green);
    }

    /**
     * 各种显示列表Adapter实现类。
     */
    public class MyAdapter extends BaseAdapter {
        private List<Map<String, Object>> mData;
        private LayoutInflater mInflater;
        private SimpleDateFormat hhmmDataFormat = new SimpleDateFormat("HH:mm:ss");

        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
            mData = new ArrayList<Map<String, Object>>();
        }

        public void addItem(String content, ChatInfoColorType color) {
            Map<String, Object> it = new HashMap<String, Object>();
            it.put("__content__", "[" + hhmmDataFormat.format(new Date()) + "]" + content);
            it.put("__color__", color);
            mData.add(it);
            this.notifyDataSetChanged();
            chatInfoListView.setSelection(this.getCount());
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.demo_main_activity_list_item_layout, null);
                holder.content = (TextView) convertView.findViewById(R.id.demo_main_activity_list_item_layout_tvcontent);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.content.setText((String) mData.get(position).get("__content__"));
            ChatInfoColorType colorType = (ChatInfoColorType) mData.get(position).get("__color__");
            switch (colorType) {
                case blue:
                    holder.content.setTextColor(Color.rgb(0, 0, 255));
                    break;
                case brightred:
                    holder.content.setTextColor(Color.rgb(255, 0, 255));
                    break;
                case red:
                    holder.content.setTextColor(Color.rgb(255, 0, 0));
                    break;
                case green:
                    holder.content.setTextColor(Color.rgb(0, 128, 0));
                    break;
                case black:
                default:
                    holder.content.setTextColor(Color.rgb(0, 0, 0));
                    break;
            }

            return convertView;
        }

        public final class ViewHolder {
            public TextView content;
        }
    }

    /**
     * 信息颜色常量定义。
     */
    public enum ChatInfoColorType {
        black,
        blue,
        brightred,
        red,
        green,
    }

}
