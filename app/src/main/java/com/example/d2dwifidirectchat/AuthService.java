package com.example.d2dwifidirectchat;

import android.util.Log;

import com.example.d2dwifidirectchat.ServerClient.messagesChangedListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthService {

    public Integer authStep;
    ArrayList<String> authStrings;

    ServerClient serverClient;
    Boolean isHost;
    String tag;
    ChatActivity.finishedInterface authDone;

    public AuthService(ServerClient _serverClient, Boolean _isHost, ArrayList<String> _authStrings, ChatActivity.finishedInterface _authDone){
        authStep = 0;
        authStrings = _authStrings;
        serverClient = _serverClient;
        isHost = _isHost;
        tag = "AUTH-" + isHost.toString();
        authDone=_authDone;
    }


    public void startAuth(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(beginAuth);
    }


    Runnable beginAuth = new Runnable() {
        @Override
        public void run() {
            while(serverClient.outStream == null){

            }
            if(isHost){
                //Thread.sleep(1000);
                Log.d(tag,"Sending first message");
                serverClient.writeProtocol("Hello".getBytes(StandardCharsets.UTF_8));
                authStep++;
            }
            else{

            }
        }
    };

    public void authComplete(){
        authDone.completed("All done!");
    }

    public messagesChangedListener incomingMessagesListener = new messagesChangedListener(){
        @Override
        public void onMessagesChangedListener() {

            Log.d(tag, "current step = " + authStep.toString());
            Log.d(tag, authStrings.toString());
            String incoming = authStrings.get(authStrings.size() - 1);

            //Protocol steps for client initiating protocol
            if(!isHost){
                switch (authStep){
                    case 0:
                        if(incoming.equals("Hello")){
                            Log.d(tag,"Message 1 received as expected");
                            serverClient.writeProtocol("Hello back".getBytes(StandardCharsets.UTF_8));
                            authStep++;
                        }
                        else Log.d(tag,"Step 0 not as expected");
                        break;
                    case 1:
                        if(incoming.equals("Hello done")){
                            Log.d(tag,"protocol done!");
                            authStep++;

                            //TODO: Raise event for auth complete
                            authComplete();
                        }
                        else Log.d(tag,"Step 1 not as expected");
                        break;
                    default:
                        Log.d(tag,"default");

                }
            }
            //Protocol steps for host of protocol
            else{
                switch (authStep){
                    case 1:
                        if(incoming.equals("Hello back")){
                            Log.d(tag,"Response to message 1 received as expected");
                            serverClient.writeProtocol("Hello done".getBytes(StandardCharsets.UTF_8));
                            authStep++;

                            authComplete();
                        }
                        else Log.d(tag, "Step 1 not as expected");

                        break;
                    default:
                        Log.d(tag,"default");
                }
            }
        }
    };


}
