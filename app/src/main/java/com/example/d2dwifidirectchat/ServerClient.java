package com.example.d2dwifidirectchat;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerClient extends Thread{

    String hostAddress;
    Boolean isServer;
    Socket socket;
    ServerSocket servSocket;
    TextView messageText;
    private InputStream inStream;
    private OutputStream outStream;

    public ServerClient(InetAddress hostAdd, TextView messagesBox){
        isServer = false;
        hostAddress = hostAdd.getHostAddress();
        socket = new Socket();
        messageText =messagesBox;
    }

    //If isHost
    public ServerClient(TextView messagesBox){
        isServer = true;
        messageText = messagesBox;
    }

    public void write(byte[] bytes){
        try {
            outStream.write(bytes);
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
                socket.connect(new InetSocketAddress(hostAddress, 8888), 30000);
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
                        bytes = inStream.read(buffer);
                        if(bytes > 0 ){
                            int finalBytes = bytes;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    String bufferMessage = new String(buffer, 0, finalBytes);

                                    //TODO: make proper chat interface here
                                    messageText.setText(bufferMessage);

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
