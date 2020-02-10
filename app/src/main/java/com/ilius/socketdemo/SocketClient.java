package com.ilius.socketdemo;

import android.os.Message;
import android.util.Log;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class SocketClient implements Runnable{
    private Socket socket;
    private String host;
    private int port;
    private PrintWriter pw;
    private Message message;
    private android.os.Handler handler;

    public SocketClient(String host, int port, android.os.Handler handler){
        this.handler = handler; // MainActivity 的 handler
        this.host = host;
        this.port = port;
        this.socket = new Socket();
    }

    @Override
    public void run() {
        int resetCount = 4;
            while (--resetCount>0){ // 这里给3次重连机会(在连接条件成立下)
                try {
                    // 给个连接延时(3000后还在连接则判断连接失败！)
                    SocketAddress socketAddress = new InetSocketAddress(host,port); // 连接的地址+端口
                    socket.connect(socketAddress,3000);
                    pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8"),true);
                    resetCount = 4; // 连接成功了就再给3次重连机会
                    if (!this.readMessage()){ // 这里进入读socket消息的方法,如果出现异常则返回false
                        continue; // 这里直接进入下个循环...目前这的代码块啥用没有，但以后我需要用到。
                    }
                } catch (Exception e) { // 连接失败用handler发送消息处理。
                    Log.d("-e",e.toString());
                    socket = new Socket();
                    message = new Message();
                    message.what = -1;
                    message.obj = "尝试重写连接服务器..."+resetCount;
                    handler.sendMessage(message);
                }
            }
            message = new Message();
            message.what = -2;
            handler.sendMessage(message); // 退出程序...
    }

    /**
     * 读Socket消息...需先连接服务端
     * @return 读取消息失败则返回 false
     */
    private Boolean readMessage(){
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
            message = new Message();
            message.what=2;
            message.obj="连接成功，请设置昵称！";
            handler.sendMessage(message);  // 让安卓弹出设置聊天昵称的窗口
            while (isEnable()){ // socket为连接状态下则循环
                char messages[] = new char[1024];
                int i = br.read(messages); // 读消息（每次读1024个字符）
                if (i>-1){
                    char str0[] =new char[i];
                    System.arraycopy(messages, 0, str0, 0, i);
                    String str = new String(str0); // 转成字符串
                    message = new Message();
                    message.what = 1;
                    message.obj = str;
                    handler.sendMessage(message); // 让安卓把收到的聊天消息加入到消息栏中...
                }else {
                    break;
                }
            }
            return false;
        }catch(Exception e){
            message = new Message();
            message.what = -1;
            message.obj = "服务器已断开...正在尝试重连";
            handler.sendMessage(message);
        }
        return false;
    }

    /**
     * Socket发送消息
     * @param message 发送的内容
     * @return 发送成功为True 否则 False
     */
    public Boolean sendMessage(final String message){
        if (this.isEnable()){ // 连接状态为真，才可以发送消息
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 很骚气，这个也必须用线程,不然就报错...哎
                    // 因为发送消息也是网络请求操作，不这样搞会出现卡死。
                    pw.println(message);
                }
            }).start();
            return true;
        }
        return false;
    }

    /**
     * 返回Socket是否已连接
     * @return 如果socket已连接返回 True 否则 False
     */
    public Boolean isEnable(){
        // 下面的逻辑代码是 是否连接过...并且是否不是关闭状态，才能给出是否已连接...
        if(this.socket.isConnected()&&!this.socket.isClosed()){
            return true;
        }else {
            return false;
        }
    }

}
