package com.flexmeet.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.flexmeet.adapters.ChatAdapter;
import com.flexmeet.databinding.ActivityPrivateChatBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.Message;
import com.flexmeet.models.User;
import com.google.firebase.database.*;
import java.util.UUID;

public class PrivateChatActivity extends AppCompatActivity {
    private ActivityPrivateChatBinding binding;
    private User otherUser;
    private String chatId, myId;
    private ChatAdapter adapter;
    private DatabaseReference chatRef, friendshipRef;
    private ChildEventListener messagesListener;
    private ValueEventListener friendshipListener;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityPrivateChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        otherUser = (User) getIntent().getSerializableExtra("user");
        myId = FirebaseManager.getInstance().getCurrentUserId();
        if (otherUser == null || myId == null || otherUser.getUid() == null) { finish(); return; }
        binding.userNameText.setText(otherUser.getDisplayName() == null ? "Friend" : otherUser.getDisplayName());
        binding.userStatusText.setText(otherUser.getEmail() == null ? "FlexMeet contact" : otherUser.getEmail());
        binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter();
        binding.chatRecyclerView.setAdapter(adapter);
        binding.backBtn.setOnClickListener(v -> finish());
        binding.sendBtn.setOnClickListener(v -> sendMessage());

        friendshipRef = FirebaseManager.getInstance().getFirebaseDatabase().getReference("contacts").child(myId).child(otherUser.getUid());
        friendshipListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    binding.messageInput.setEnabled(false);
                    binding.sendBtn.setEnabled(false);
                    binding.messageInput.setHint("Accept a friend request to chat");
                    Toast.makeText(PrivateChatActivity.this, "Chat is available after the friend request is accepted.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                binding.messageInput.setEnabled(true);
                binding.sendBtn.setEnabled(true);
                openAcceptedChat();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { Toast.makeText(PrivateChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show(); finish(); }
        };
        friendshipRef.addListenerForSingleValueEvent(friendshipListener);
    }

    private void openAcceptedChat() {
        if (chatRef != null) return;
        String otherId = otherUser.getUid();
        String firstId = myId.compareTo(otherId) < 0 ? myId : otherId;
        String secondId = myId.compareTo(otherId) < 0 ? otherId : myId;
        chatId = firstId + "_" + secondId;
        chatRef = FirebaseManager.getInstance().getFirebaseDatabase().getReference("private_chats")
                .child(firstId).child(secondId).child("messages");
        messagesListener = new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previous) {
                Message m = snapshot.getValue(Message.class);
                if (m != null) { adapter.addMessage(m); binding.chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1); }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) { Toast.makeText(PrivateChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show(); }
        };
        chatRef.addChildEventListener(messagesListener);
    }

    private void sendMessage() {
        String text = binding.messageInput.getText() == null ? "" : binding.messageInput.getText().toString().trim();
        if (text.isEmpty() || chatRef == null) return;
        String myName = FirebaseManager.getInstance().getCurrentUser() == null ? null : FirebaseManager.getInstance().getCurrentUser().getDisplayName();
        if (myName == null || myName.trim().isEmpty()) myName = FirebaseManager.getInstance().getCurrentUserEmail().split("@")[0];
        Message message = new Message(UUID.randomUUID().toString(), myId, myName, chatId, text);
        chatRef.push().setValue(message).addOnFailureListener(e -> Toast.makeText(this, "Message failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        binding.messageInput.setText("");
    }

    @Override protected void onDestroy() {
        if (chatRef != null && messagesListener != null) chatRef.removeEventListener(messagesListener);
        if (friendshipRef != null && friendshipListener != null) friendshipRef.removeEventListener(friendshipListener);
        super.onDestroy();
    }
}