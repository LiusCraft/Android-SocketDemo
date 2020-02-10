package com.ilius.socketdemo;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetSocketAddress;

public class MainActivity extends AppCompatActivity {

    private Button btn_send;
    private EditText et_message;
    private TextView tv_message;
    private ScrollView sv_lt;
    private String nickName;
    private SocketClient socketClient;

    /**
     * 处理Socket消息
     */
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: // 聊天消息
                    tv_message.append(msg.obj.toString());
                    int offset=tv_message.getLineCount()*tv_message.getLineHeight();//判断textview文本的高度
                    if (offset > sv_lt.getHeight()) {
                        sv_lt.scrollTo(0,offset - sv_lt.getHeight());//如果文本的高度大于ScrollView,就自动滑动
                    }
                    break;
                case 2: // 连接服务端成功，设置聊天昵称
                    setNickName();
                    break;
                case -1: // socket处理出现异常(断开连接等异常...)
                    Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;
                case -2:
                    finish();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setViewModule(); // 设置视图的组件
        setListener(); // 设置设置监听事件
        // 给socket信息与启动socket线程
        socketClient = new SocketClient("cn-zj-dx-3.sakurafrp.com",36638, handler);
        Thread thread = new Thread(socketClient);
        thread.start();
    }

    private void setViewModule(){
        btn_send = findViewById(R.id.btn_send);
        et_message = findViewById(R.id.et_message);
        tv_message = findViewById(R.id.tv_message);
        sv_lt = findViewById(R.id.sv_lt);
    }

    private void setListener() {
        OnClick onClick = new OnClick();
        btn_send.setOnClickListener(onClick);
    }

    private class OnClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_send:
                    if (socketClient.isEnable()){
                        socketClient.sendMessage(et_message.getText().toString());
                        et_message.setText("");
                    }else{
                        Toast.makeText(MainActivity.this,"未连接到服务器...",Toast.LENGTH_LONG).show();
                    }
            }
        }
    }

    private void setNickName() {
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_setname, null);
        AlertDialog.Builder setName = new AlertDialog.Builder(MainActivity.this);
        final AlertDialog dialog = setName.create();
        dialog.setView(view);
        dialog.show();
        dialog.setCancelable(false);
        Button btn_start = view.findViewById(R.id.btn_start);
        Button btn_clean = view.findViewById(R.id.btn_clean);
        final EditText et_nickname = view.findViewById(R.id.et_nickname);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nickName = et_nickname.getText().toString();
                if ("".equals(nickName)) {
                    Toast.makeText(MainActivity.this, "请给自己一个昵称啊~", Toast.LENGTH_LONG).show();
                    return;
                }
                socketClient.sendMessage(nickName);
                Toast.makeText(MainActivity.this, "您好!" + nickName, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });
        btn_clean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
