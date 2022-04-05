package com.example.d2dwifidirectchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    Activity act;

    TextView connectionStatus, messageText;
    Button scanButton;
    RecyclerView devicesView;
    EditText typeMessage;
    ImageButton sendButton;


    WifiP2pManager manager;
    WifiP2pManager.Channel channel;

    BroadcastReceiver receiver;
    IntentFilter intentFilter;

    ArrayList<WifiP2pDevice> availableDevices = new ArrayList<WifiP2pDevice>();
    AvailableDevicesAdapter adapter;

    //Socket socket;
    //ServerClass serverClass;
    //ClientClass clientClass;
    ServerClient serverClient;
    Boolean isHost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initComponents();
        addListeners();


    }

    private void addListeners() {
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    Log.d("p2p","No Permissions");
                    String[] perms = {
                            "android.permission.ACCESS_FINE_LOCATION",
                    };
                    ActivityCompat.requestPermissions(act, perms,99);

                    return;
                }
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override

                    public void onSuccess() {
                        Log.d("P2P","Discovery started");
                        connectionStatus.setText("Discovery started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery failed to start");
                    }
                });
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                String messageToSend = typeMessage.getText().toString();
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if( messageToSend != null && isHost){
                            serverClient.write(messageToSend.getBytes(StandardCharsets.UTF_8));
                        } else if( messageToSend != null && !isHost){
                            serverClient.write(messageToSend.getBytes(StandardCharsets.UTF_8));
                        }
                    }
                });
            }
        });
    }

    private void initComponents() {
        connectionStatus = findViewById(R.id.connection_status);
        messageText = findViewById(R.id.message_view);
        scanButton = findViewById(R.id.scan_button);
        devicesView = findViewById(R.id.devices_view);
        typeMessage = findViewById(R.id.edit_message_text);
        sendButton = findViewById(R.id.image_button);

        act = this;

        adapter = new AvailableDevicesAdapter(availableDevices);
        devicesView.setAdapter(adapter);
        devicesView.setLayoutManager(new LinearLayoutManager(this));
        adapter.setOnItemClickListener(new AvailableDevicesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                WifiP2pDevice device = availableDevices.get(position);
                String name = device.deviceName;
                //TODO: connect to phone
                connectToPhone(device);
                Toast.makeText(act, name + " was clicked!", Toast.LENGTH_SHORT).show();
            }
        });

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }




    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            Log.d("P2P","Peers available!");
            if(!wifiP2pDeviceList.equals(availableDevices)){
                availableDevices.clear();
                availableDevices.addAll(wifiP2pDeviceList.getDeviceList());

                adapter.notifyDataSetChanged();

                if(availableDevices.size() == 0){
                    connectionStatus.setText("No devices found");
                    return;
                }


            }
        }
    };


    //Suppress permission check because permissions already checked by this point
    @SuppressLint("MissingPermission")
    protected void connectToPhone(WifiP2pDevice targetDevice){
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = targetDevice.deviceAddress;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                connectionStatus.setText(String.format("Request sent to %s!",targetDevice.deviceName));
            }

            @Override
            public void onFailure(int i) {
                connectionStatus.setText("Failed to send request");
                Toast.makeText(act,"Failed to request!", Toast.LENGTH_LONG).show();
            }
        });
    }

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            //Phone is host
            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                connectionStatus.setText("Host");
                isHost = true;
                serverClient = new ServerClient(messageText);
                serverClient.start();
            }
            //Phone is client
            else if(wifiP2pInfo.groupFormed){
                connectionStatus.setText("Client");
                isHost = false;
                serverClient = new ServerClient(groupOwnerAddress, messageText);
                serverClient.start();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver,intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


}