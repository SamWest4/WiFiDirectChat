package com.example.d2dwifidirectchat;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    TextView statusText;
    RecyclerView messagesView;
    EditText sendText;
    ImageButton sendButton;
    ServerClient serverClient;
    //Button disconnectButton;

    ArrayList<MessagePair> messages = new ArrayList<MessagePair>();
    ChatMessagesAdapter adapter;

    Boolean isHost;
    String hostAddress;

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;

    Activity thisAct;
    Context thisContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();
        isHost = intent.getBooleanExtra("isHost", false);
        hostAddress = intent.getStringExtra("hostAddress");
        thisAct = this;
        thisContext = getApplicationContext();


        initComponents();
        setUpServer();
    }

    public static void triggerRebirth(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }

        Runtime.getRuntime().exit(0);
    }

//    private void disconnect() {
//        serverClient.write("[<[disconnect]>]".getBytes(StandardCharsets.UTF_8));
//        if (manager != null && channel != null) {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//                //permission check
//            }
//            manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
//                @Override
//                public void onGroupInfoAvailable(WifiP2pGroup group) {
//                    if (group != null && manager != null && channel != null) {
//                        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
//
//                            @Override
//                            public void onSuccess() {
//                                Log.d("p2p-disconnect", "removeGroup onSuccess -");
//                                triggerRebirth(thisContext);
//                            }
//
//                            @Override
//                            public void onFailure(int reason) {
//                                Log.d("p2p-disconnect", "removeGroup onFailure -" + reason);
//                                triggerRebirth(thisContext);
//
//                            }
//                        });
//                    }
//                }
//            });
//        }
//    }

    private void setUpServer(){
        //Phone is host
        Log.d("p2p", isHost.toString());
        Log.d("p2p", hostAddress);
        if(isHost){
            statusText.setText("Host");
            serverClient = new ServerClient(messages);
        }
        //Phone is client
        else{
            statusText.setText("Client");
            serverClient = new ServerClient(hostAddress, messages);
        }
        serverClient.setMessagesChangedListener(new ServerClient.messagesChangedListener() {
            @Override
            public void onMessagesChangedListener() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        messagesView.smoothScrollToPosition(adapter.getItemCount());
                    }
                });
            }
        });
        serverClient.start();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                String messageToSend = sendText.getText().toString();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if( messageToSend != null) {
                            messages.add(new MessagePair("Me", messageToSend));
                            serverClient.write(messageToSend.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                });

                sendText.setText("");
                //Hides the keyboard
                InputMethodManager imm = (InputMethodManager) thisAct.getSystemService(Activity.INPUT_METHOD_SERVICE);
                View v = thisAct.getCurrentFocus();
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            }
        });

//        disconnectButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                disconnect();
//            }
//        });
    }
    private void initComponents() {
        statusText = findViewById(R.id.status_text);
        messagesView = findViewById(R.id.messages_view);
        sendText = findViewById(R.id.message_text);
        sendButton = findViewById(R.id.send_button);
        //disconnectButton = findViewById(R.id.disconnect_button);

        adapter = new ChatMessagesAdapter(messages);
        messagesView.setAdapter(adapter);
        messagesView.setLayoutManager(new LinearLayoutManager(this));

        //manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        //channel = manager.initialize(this, getMainLooper(), null);



    }





}