package com.example.d2dwifidirectchat;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
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
    public InputStream inStream;
    public OutputStream outStream;

    ArrayList<MessagePair> messages;
    ArrayList<String> authStrings;

    Boolean secured;

    //Constructor for server
    public ServerClient(ArrayList<MessagePair> _messages, ArrayList<String> _authStrings){
        isServer = true;
        messages = _messages;
        secured = false;
        authStrings = _authStrings;

    }

    //Constructor for client
    public ServerClient(String hostAdd, ArrayList<MessagePair> _messages, ArrayList<String> _authStrings){
        isServer = false;
        hostAddress = hostAdd;
        socket = new Socket();
        messages = _messages;
        secured = false;
        authStrings = _authStrings;

    }

    public void setSecured(Boolean _secured){
        secured = _secured;
    }

    public interface messagesChangedListener {
        public void onMessagesChangedListener();
    }

    private messagesChangedListener messagesChanged;

    public void setMessagesChangedListener(messagesChangedListener mcl){
        this.messagesChanged = mcl;
    }

    public void writeProtocol(byte[] bytes){
        if(outStream != null){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try  {
                        outStream.write(bytes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
        else{
            Log.d("p2p", "Out stream is null");
        }
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
                    Thread.sleep(600);

                    try {
                        socket.connect(new InetSocketAddress(hostAddress, 8888), 1500);
                    } catch (ConnectException e) {
                        Log.d("p2p", "Failed to open socket, trying again");
                        Log.d("p2p",e.toString());
                        Thread.sleep(700);
                        socket.connect(new InetSocketAddress(hostAddress, 8888), 1500);
                    }
                }catch (InterruptedException e){
                    Log.d("p2p", "Thread sleeps error");
                }


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
                int bytes = 0;

                while(socket != null){
                    try {
                        if(inStream != null){
                            bytes = inStream.read(buffer);
                        } else bytes = 0;
                        if(bytes > 0 ){
                            int finalBytes = bytes;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    String bufferMessage = new String(buffer, 0, finalBytes);


                                    if(!secured){
                                        authStrings.add(bufferMessage);
                                    }
                                    else{
                                        MessagePair incomingMessage = new MessagePair(bufferMessage);
                                        messages.add(incomingMessage);
                                    }
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
