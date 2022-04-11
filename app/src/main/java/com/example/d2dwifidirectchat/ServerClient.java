package com.example.d2dwifidirectchat;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerClient extends Thread{

    String hostAddress;
    Boolean isServer;
    Socket socket;
    ServerSocket servSocket;
    //TextView messageText;
    private InputStream inStream;
    private OutputStream outStream;

    ArrayList<MessagePair> messages;

    //Constructor for server
    public ServerClient(ArrayList<MessagePair> _messages){
        isServer = true;
        messages = _messages;
        Log.d("p2p", "here11111!");
    }

    //Constructor for client
    public ServerClient(String hostAdd, ArrayList<MessagePair> _messages){
        isServer = false;
        Log.d("p2p", "here!");
        hostAddress = hostAdd;
        socket = new Socket();
        messages = _messages;
    }

    public interface messagesChangedListener {
        public void onMessagesChangedListener();
    }

    private messagesChangedListener messagesChanged;

    public void setMessagesChangedListener(messagesChangedListener mcl){
        this.messagesChanged = mcl;
    }

    public void write(byte[] bytes){
        try {
            if(outStream != null){
                outStream.write(bytes);
                messagesChanged.onMessagesChangedListener();
            }
            else{
                Log.d("p2p", "outstream is null");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        try {
            if(isServer){
                servSocket = new ServerSocket(8888);
                socket = servSocket.accept();
            } else{
                Log.d("p2p","Client trying to open socket");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                }
                socket.connect(new InetSocketAddress(hostAddress, 8888), 500);
            }
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;

                while(socket != null){
                    try {
                        if(inStream != null){
                            bytes = inStream.read(buffer);
                        }
                        else{
                            bytes = 0;
                        }
                        if(bytes > 0 ){
                            int finalBytes = bytes;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    String bufferMessage = new String(buffer, 0, finalBytes);


                                    Log.d("P2P-message",bufferMessage);
                                    MessagePair message = new MessagePair("User", bufferMessage);
                                    messages.add(message);
                                    messagesChanged.onMessagesChangedListener();

                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
