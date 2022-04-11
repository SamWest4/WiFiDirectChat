package com.example.d2dwifidirectchat;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatMessagesAdapter extends RecyclerView.Adapter<ChatMessagesAdapter.ViewHolder>{

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView messageSender;
        public TextView message;

        public ViewHolder(final View itemView) {
            super(itemView);

            messageSender = (TextView) itemView.findViewById(R.id.sender_name);
            message = (TextView) itemView.findViewById(R.id.sender_message);
        }
    }



    private ArrayList<MessagePair> messages;


    //Constructor
    public ChatMessagesAdapter(ArrayList<MessagePair> _messages){
        messages = _messages;
    }

    @Override
    public ChatMessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View messagesView = inflater.inflate(R.layout.message, parent, false);

        ViewHolder viewHolder = new ViewHolder(messagesView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ChatMessagesAdapter.ViewHolder holder, int position) {
        MessagePair message = messages.get(position);

        TextView senderView = holder.messageSender;
        senderView.setText(message.sender);


        TextView messageView = holder.message;
        messageView.setText(message.message);

        if(message.sender == "Me"){
            senderView.setGravity(Gravity.RIGHT);
            messageView.setGravity(Gravity.RIGHT);
        }
        else{
            senderView.setGravity(Gravity.LEFT);
            messageView.setGravity(Gravity.LEFT);
        }
    }

    @Override
    public int getItemCount() {
        if(messages != null){
            return messages.size();
        }
        else{
            return 0;
        }

    }
}
