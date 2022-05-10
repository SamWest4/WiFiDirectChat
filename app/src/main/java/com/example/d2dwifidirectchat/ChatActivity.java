package com.example.d2dwifidirectchat;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

public class ChatActivity extends AppCompatActivity {

    TextView statusText, protocolText;
    RecyclerView messagesView;
    EditText sendText;
    ImageButton sendButton;
    ServerClient serverClient;
    LinearLayout messageLayout;
    Button disconnectButton;
    String tag;

    ArrayList<MessagePair> messages = new ArrayList<MessagePair>();
    ChatMessagesAdapter adapter;

    Boolean isHost;
    String hostAddress;

    Activity thisAct;
    Context thisContext;

    AuthService authService;
    String deviceName;
    Boolean isSecured;
    ArrayList<String> authStrings;
    Integer authStep;

    SecretKey sharedKey;


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
        if(isHost) tag = "CHAT-HOST";
        else tag = "CHAT-CLIENT";

        authService = new AuthService(serverClient,isHost,authStrings, myInterface, thisContext);
        serverClient.setMessagesChangedListener(authService.incomingMessagesListener);
        authService.startAuth();

    }

    public interface finishedInterface {
        String completed(SecretKey k);
    }
    public finishedInterface myInterface = (k) -> {

        Log.d("Auth", "AUTH SERVICE DONE!");
        sharedKey = k;


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
        serverClient.setSecured(true, sharedKey);
        sendButton.setEnabled(true);
        protocolText.setText("SECURED");
        messageLayout.setVisibility(View.VISIBLE);
        protocolText.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        return "Done";
    };




    private void setUpServer(){
        //Phone is host
        if(isHost){
            statusText.setText("Host");
            serverClient = new ServerClient(messages, authStrings);
        }
        //Phone is client
        else{
            statusText.setText("Client");
            serverClient = new ServerClient(hostAddress, messages, authStrings);
        }



        serverClient.start();

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnect();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                String messageToSend = sendText.getText().toString();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if( messageToSend != null) {
                            String escapedMessage = messageToSend.replace("\'","\\'");

                            try {

                                MessagePair mPair =  new MessagePair(deviceName, escapedMessage);
                                messages.add(new MessagePair("Me", escapedMessage));
                                String encryptedMessage = protocolUtils.encrypt(mPair.toString().getBytes(StandardCharsets.UTF_8), sharedKey);
                                Log.d(tag+"-SENT", encryptedMessage);
                                serverClient.write(encryptedMessage.getBytes(StandardCharsets.UTF_8));
                            } catch (Exception e) {
                                Log.d("Encryption", "Error encrypting message!");
                                e.printStackTrace();
                            }


                        }
                    }
                });

                sendText.setText("");
                //Hides the keyboard
//                InputMethodManager imm = (InputMethodManager) thisAct.getSystemService(Activity.INPUT_METHOD_SERVICE);
//                View v = thisAct.getCurrentFocus();
//                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            }
        });


    }
    private void initComponents() {
        statusText = findViewById(R.id.status_text);
        protocolText = findViewById(R.id.protocol_text);
        messagesView = findViewById(R.id.messages_view);
        sendText = findViewById(R.id.message_text);
        sendButton = findViewById(R.id.send_button);
        sendButton.setEnabled(false);
        messageLayout = findViewById(R.id.linearLayoutMessages);
        disconnectButton = findViewById(R.id.disconnect_button);

        messageLayout.setVisibility(View.INVISIBLE);

        adapter = new ChatMessagesAdapter(messages);
        messagesView.setAdapter(adapter);
        messagesView.setLayoutManager(new LinearLayoutManager(this));

        isSecured = false;
        deviceName = Settings.Global.getString(getContentResolver(), "device_name");
        authStrings = new ArrayList<String>();
        authStep = 0;



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





    private void disconnect() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {

                WifiP2pManager manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
                WifiP2pManager.Channel channel = manager.initialize(thisContext, getMainLooper(), null);


                if (channel != null) {

                    if(ActivityCompat.checkSelfPermission(thisContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        //permission check
                    }
                    manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                        @Override
                        public void onGroupInfoAvailable(WifiP2pGroup group) {
                            if (group != null) {
                                manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                                    @Override
                                    public void onSuccess() {
                                        Log.d("p2p-disconnect", "removeGroup onSuccess -");
                                        triggerRebirth(thisContext);
                                    }

                                    @Override
                                    public void onFailure(int reason) {
                                        Log.d("p2p-disconnect", "removeGroup onFailure -" + reason);
                                        triggerRebirth(thisContext);

                                    }
                                });
                            }
                            else{
                                triggerRebirth(thisContext);
                            }
                        }
                    });
                }
                else{
                    Log.d("DISCONNECT","channel is null");
                }
            }
        });

    }


}