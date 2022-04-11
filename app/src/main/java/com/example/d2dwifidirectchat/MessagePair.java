package com.example.d2dwifidirectchat;

import android.util.Log;

import org.json.JSONObject;

public class MessagePair {
    public String sender;
    public String message;

    //constructor
    public MessagePair(String _sender, String _message) {
        sender = _sender;
        message = _message;
    }

    public MessagePair(String messagePairString){
        try {

            if(messagePairString.startsWith("MessagePair")){
                String[] split = messagePairString.split("MessagePair");
                Log.d("MessagePairString", messagePairString);
                JSONObject myObj = new JSONObject(split[1]);
                Log.d("JSON", myObj.toString());
                sender = myObj.getString("sender");
                message = myObj.getString("message");
            }
            else{
                Log.d("p2p", "Error is message format");
                sender = "ERROR";
                message = "ERROR";
            }
        }
        catch(Exception e){
            Log.d("p2p", "Error receiving message");
            Log.d("p2p", messagePairString);
            sender = "ERROR";
            message = "ERROR";
        }
    }

    @Override
    public String toString() {
        return "MessagePair{" +
                "sender:'" + sender + '\'' +
                ", message:'" + message + '\'' +
                '}';
    }



}
