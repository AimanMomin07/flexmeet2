package com.flexmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.flexmeet.R;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.Message;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<Message> messages = new ArrayList<>();
    private String currentUserId;
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatAdapter() {
        this.currentUserId = FirebaseManager.getInstance().getCurrentUserId();
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        if (currentUserId != null && currentUserId.equals(messages.get(position).getSenderId())) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        String time = timeFormatter.format(new Date(message.getTimestamp()));

        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).messageTv.setText(message.getText());
            ((SentViewHolder) holder).timestampTv.setText(time);
        } else {
            ((ReceivedViewHolder) holder).userNameTv.setText(message.getSenderName());
            ((ReceivedViewHolder) holder).messageTv.setText(message.getText());
            ((ReceivedViewHolder) holder).timestampTv.setText(time);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView messageTv, timestampTv;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.text_message);
            timestampTv = itemView.findViewById(R.id.text_timestamp);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTv, messageTv, timestampTv;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTv = itemView.findViewById(R.id.text_user_name);
            messageTv = itemView.findViewById(R.id.text_message);
            timestampTv = itemView.findViewById(R.id.text_timestamp);
        }
    }
}
