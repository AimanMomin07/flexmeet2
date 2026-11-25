package com.flexmeet.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.flexmeet.adapters.ChatAdapter;
import com.flexmeet.databinding.FragmentChatBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.Message;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import java.util.UUID;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private String meetingId;
    private ChatAdapter adapter;
    private DatabaseReference chatRef;

    public static ChatFragment newInstance(String meetingId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("meeting_id", meetingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            meetingId = getArguments().getString("meeting_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatRef = FirebaseManager.getInstance().getFirebaseDatabase()
                .getReference("meetings").child(meetingId).child("messages");

        setupRecyclerView();
        setupChatListener();

        binding.sendMessageBtn.setOnClickListener(v -> sendMessage());
        binding.closeChat.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter();
        binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.chatRecyclerView.setAdapter(adapter);
    }

    private void setupChatListener() {
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    adapter.addMessage(message);
                    binding.chatRecyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Chat sync failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = binding.messageInput.getText().toString().trim();
        if (text.isEmpty()) return;

        String userId = FirebaseManager.getInstance().getCurrentUserId();
        String userName = FirebaseManager.getInstance().getCurrentUser().getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = FirebaseManager.getInstance().getCurrentUserEmail().split("@")[0];
        }

        Message message = new Message(
                UUID.randomUUID().toString(),
                userId,
                userName,
                meetingId,
                text
        );

        chatRef.push().setValue(message);
        binding.messageInput.setText("");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
