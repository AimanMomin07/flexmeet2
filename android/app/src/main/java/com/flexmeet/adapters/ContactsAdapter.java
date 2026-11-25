package com.flexmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.flexmeet.R;
import com.flexmeet.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
    private final List<User> users = new ArrayList<>();
    private final OnUserClickListener listener;

    public interface OnUserClickListener { void onUserClick(User user); }

    public ContactsAdapter(OnUserClickListener listener) { this.listener = listener; }

    public void setUsers(List<User> items) {
        users.clear();
        if (items != null) users.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.name.setText(user.getDisplayName() == null || user.getDisplayName().isEmpty() ? "FlexMeet member" : user.getDisplayName());
        holder.email.setText(user.getEmail() == null ? "" : user.getEmail());
        String state = user.getStatus() == null ? "search" : user.getStatus();
        if ("incoming".equals(state)) holder.action.setText("Accept");
        else if ("requested".equals(state)) holder.action.setText("Requested");
        else if ("accepted".equals(state)) holder.action.setText("Message");
        else holder.action.setText("Add");
        holder.action.setEnabled(!"requested".equals(state));
        holder.action.setOnClickListener(v -> { if (listener != null) listener.onUserClick(user); });
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onUserClick(user); });
    }

    @Override public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView avatar;
        final TextView name, email;
        final MaterialButton action;
        ViewHolder(@NonNull View item) {
            super(item);
            avatar = item.findViewById(R.id.user_image);
            name = item.findViewById(R.id.user_name);
            email = item.findViewById(R.id.user_email);
            action = item.findViewById(R.id.chat_btn);
        }
    }
}