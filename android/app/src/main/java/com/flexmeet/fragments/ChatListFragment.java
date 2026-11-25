package com.flexmeet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.flexmeet.activities.PrivateChatActivity;
import com.flexmeet.adapters.ContactsAdapter;
import com.flexmeet.databinding.FragmentChatListBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.User;
import com.google.firebase.database.*;
import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment implements ContactsAdapter.OnUserClickListener {
    private FragmentChatListBinding binding;
    private ContactsAdapter adapter;
    private DatabaseReference contactsRef;
    private ValueEventListener listener;
    private final List<User> friends = new ArrayList<>();

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentChatListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        adapter = new ContactsAdapter(this);
        binding.chatListRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.chatListRecyclerView.setAdapter(adapter);
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        if (uid == null) { empty("Sign in to chat with your friends."); return; }
        contactsRef = FirebaseManager.getInstance().getFirebaseDatabase().getReference("contacts").child(uid);
        listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                friends.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user != null) { user.setStatus("accepted"); friends.add(user); }
                }
                adapter.setUsers(friends);
                empty(friends.isEmpty() ? "Add a friend and accept each other's request to start chatting." : null);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { empty("Could not load friends: " + error.getMessage()); }
        };
        contactsRef.addValueEventListener(listener);
    }
    private void empty(String message) {
        if (binding == null) return;
        boolean show = message != null;
        binding.noChatsLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.chatListRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show) binding.noChatsText.setText(message);
    }
    @Override public void onUserClick(User user) {
        Intent chat = new Intent(requireContext(), PrivateChatActivity.class);
        chat.putExtra("user", user);
        startActivity(chat);
    }
    @Override public void onDestroyView() {
        if (contactsRef != null && listener != null) contactsRef.removeEventListener(listener);
        binding = null;
        super.onDestroyView();
    }
}