# Android-SocketDemo
>安卓socket通信测试项目 - 欢迎各位来对帮助我改进此项目

本项目已实现多人在线聊天！
程序自带掉线重连（有3次机会重连），如果重连失败则退出程序。
打开程序连接到服务端后会弹出让你设置聊天昵称的对话框。

>私人聊天功能：（比较简陋）
@对方昵称 聊天内容 即可跟对方私聊(昵称跟聊天内容中间需要空格空开)

>服务端：
服务端代码我就直接贴下面吧：
```java
package org.liuscraft.s;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author ilius
 */
public class Server extends Thread{
    private ServerSocket server;
    private HashMap<String, PrintWriter> outAll = new HashMap<>();

    /**
     * 默认申请19730端口
     */
    public Server() {
        try {
            this.server = new ServerSocket(19730);

        } catch (Exception e) {
            System.err.println("申请端口出现错误..."+e.getLocalizedMessage());
        }
    }

    /**
     * 创建服务,端口设置为port参数值
     * @param port 端口号
     */
    public Server(int port) {
        try {
            this.server = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("申请端口出现错误..."+e.getLocalizedMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = this.server.accept();
                new Thread(new ClientHandle(socket)).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加输出流到Server.outAll变量
     * @param nickName 用户名
     * @param pw 输出流
     * @return 添加成功返回true否则返回false
     */
    private synchronized boolean addOut(String nickName,PrintWriter pw){
        if (nickName!=null&&!this.outAll.containsKey(nickName)){
            this.outAll.put(nickName, pw);
            return true;
        }else{
            return false;
        }
    }
    private synchronized boolean removeOut(String nickName){
        if (this.outAll.remove(nickName)!=null) {
            return true;
        }
        return false;
    }

    private void sendMessageAll(String message){
        Set<java.util.Map.Entry<String, PrintWriter>> entrySet = this.outAll.entrySet();
        for (Map.Entry<String, PrintWriter> e : entrySet){
            e.getValue().println(message);
        }
    }

    private boolean sendMessageTo(String who, String message){
        PrintWriter pw1 = this.outAll.get(who); // 接收者
        String name = "";
        try{
            name = message.substring(1,message.indexOf(" ")); // 发送者
        }catch (Exception e){
            pw1.println("发送失败,私聊格式是:@对方昵称 要说的话");
            return false;
        }

        PrintWriter pw2 = this.outAll.get(name);
        if (pw1!=null){
            pw2.println(who+" 对你说:"+message);
            pw1.println("你对 "+name+" 说:"+message);
            return true;
        }
        return false;
    }

    class ClientHandle implements Runnable{
        private Socket socket;
        private String nickName;
        private String host;
        public ClientHandle(Socket socket){
            this.socket = socket;
            this.host = socket.getInetAddress().getHostAddress();
        }

        @Override
        public void run() {
            PrintWriter pw;
            try {
                InputStreamReader isr = new InputStreamReader(this.socket.getInputStream(),"utf-8");
                BufferedReader br = new BufferedReader(isr);
                OutputStreamWriter wr = new OutputStreamWriter(this.socket.getOutputStream(),"utf-8");
                 pw = new PrintWriter(wr,true);
                 this.nickName = br.readLine();
                 System.out.println(this.nickName+" 加入");
                 sendMessageAll(this.nickName+" 加入到聊天室!");
                 if (addOut(this.nickName,pw)){
                     while (true){
                         char message[] = new char[1024];
                         int i = br.read(message);
                         char str0[] =new char[i];
                         if (i==-1){
                             break;
                         }
                         System.arraycopy(message, 0, str0, 0, i);
                         String str = new String(str0);
                         System.out.println(str);
                         if(str.startsWith("@")){
                             sendMessageTo(nickName,str);
                         }else{
                            sendMessageAll(this.nickName + " 说:" + str);
                         }
                     }
                 }
            } catch (Exception e) {

            }finally {
                try {
                    socket.close();
                    removeOut(this.nickName);
                    System.out.println(this.nickName+" 离开了");
                    sendMessageAll(this.nickName+" 离开了聊天室");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```
