package com.flexmeet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.flexmeet.activities.PrivateChatActivity;
import com.flexmeet.adapters.ContactsAdapter;
import com.flexmeet.databinding.FragmentContactsBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.User;
import com.google.firebase.database.*;
import java.util.*;

public class ContactsFragment extends Fragment implements ContactsAdapter.OnUserClickListener {
    private FragmentContactsBinding binding;
    private ContactsAdapter adapter;
    private final Map<String, User> people = new LinkedHashMap<>();
    private DatabaseReference root;
    private ValueEventListener contactsListener, requestsListener;
    private String myId;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        adapter = new ContactsAdapter(this);
        binding.contactsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.contactsRecyclerView.setAdapter(adapter);
        myId = FirebaseManager.getInstance().getCurrentUserId();
        root = FirebaseManager.getInstance().getFirebaseDatabase().getReference();
        if (myId == null) {
            binding.emptyText.setText("Sign in to find and add friends.");
            binding.emptyText.setVisibility(View.VISIBLE);
            return;
        }
        listenForContacts();
        listenForRequests();
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) { searchMember(s.toString()); }
            @Override public void afterTextChanged(Editable e) {}
        });
    }

    private void searchMember(String raw) {
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() < 3) { render(); return; }
        binding.progressBar.setVisibility(View.VISIBLE);
        root.child("users").orderByChild("email").equalTo(email).get()
                .addOnCompleteListener(task -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    if (!task.isSuccessful() || task.getResult() == null) return;
                    for (DataSnapshot child : task.getResult().getChildren()) {
                        User user = child.getValue(User.class);
                        if (user == null || myId.equals(user.getUid())) continue;
                        root.child("contacts").child(myId).child(user.getUid()).get().addOnSuccessListener(contact -> {
                            if (binding == null) return;
                            if (contact.exists()) user.setStatus("accepted");
                            else root.child("friend_requests").child(myId).child(user.getUid()).get().addOnSuccessListener(req -> {
                                if (binding == null) return;
                                user.setStatus(req.exists() ? "requested" : "search");
                                people.put(user.getUid(), user);
                                render();
                            });
                            if (contact.exists()) { people.put(user.getUid(), user); render(); }
                        });
                    }
                });
    }

    private void listenForContacts() {
        DatabaseReference ref = root.child("contacts").child(myId);
        contactsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user != null) { user.setStatus("accepted"); people.put(user.getUid(), user); }
                }
                render();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { showError(error); }
        };
        ref.addValueEventListener(contactsListener);
    }

    private void listenForRequests() {
        DatabaseReference ref = root.child("friend_requests").child(myId);
        requestsListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String state = child.child("requestStatus").getValue(String.class);
                    if (!"pending".equals(state)) continue;
                    User user = child.getValue(User.class);
                    if (user != null) { user.setStatus("incoming"); people.put(user.getUid(), user); }
                }
                render();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { showError(error); }
        };
        ref.addValueEventListener(requestsListener);
    }

    private void render() {
        if (binding == null) return;
        String q = binding.searchInput.getText() == null ? "" : binding.searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<User> rows = new ArrayList<>();
        for (User user : people.values()) {
            if ("incoming".equals(user.getStatus()) || "accepted".equals(user.getStatus())
                    || (!q.isEmpty() && user.getEmail() != null && user.getEmail().toLowerCase(Locale.ROOT).contains(q))) rows.add(user);
        }
        adapter.setUsers(rows);
        binding.emptyText.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        if (rows.isEmpty()) binding.emptyText.setText(q.isEmpty() ? "No friends yet. Search by email to send a request." : "No matching members. Ask them to create a FlexMeet account first.");
    }

    @Override public void onUserClick(User user) {
        String state = user.getStatus();
        DatabaseReference pairRequest = root.child("friend_requests").child(myId).child(user.getUid());
        if ("incoming".equals(state)) {
            Map<String, Object> accepted = new HashMap<>();
            accepted.put("requestStatus", "accepted");
            accepted.put("updatedAt", ServerValue.TIMESTAMP);
            pairRequest.updateChildren(accepted);
            user.setStatus("accepted");
            Map<String, Object> contact = profileMap(user);
            contact.put("acceptedAt", ServerValue.TIMESTAMP);
            root.child("contacts").child(myId).child(user.getUid()).setValue(contact);
            Map<String, Object> mine = currentProfile();
            mine.put("acceptedAt", ServerValue.TIMESTAMP);
            root.child("contacts").child(user.getUid()).child(myId).setValue(mine);
            Toast.makeText(requireContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();
        } else if ("accepted".equals(state)) {
            Intent chat = new Intent(requireContext(), PrivateChatActivity.class);
            chat.putExtra("user", user);
            startActivity(chat);
        } else if (!"requested".equals(state)) {
            Map<String, Object> request = currentProfile();
            request.put("requestStatus", "pending");
            request.put("createdAt", ServerValue.TIMESTAMP);
            root.child("friend_requests").child(user.getUid()).child(myId).setValue(request)
                    .addOnSuccessListener(unused -> { user.setStatus("requested"); people.put(user.getUid(), user); render(); Toast.makeText(requireContext(), "Friend request sent", Toast.LENGTH_SHORT).show(); })
                    .addOnFailureListener(error -> Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private Map<String,Object> currentProfile() {
        User me = FirebaseManager.getInstance().getCurrentRealtimeUser();
        return profileMap(me);
    }
    private Map<String,Object> profileMap(User user) {
        Map<String,Object> map = new HashMap<>();
        if (user != null) { map.put("uid", user.getUid()); map.put("email", user.getEmail()); map.put("displayName", user.getDisplayName()); map.put("photoUrl", user.getPhotoUrl()); }
        return map;
    }
    private void showError(DatabaseError e) { if (getContext() != null) Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show(); }

    @Override public void onDestroyView() {
        if (root != null && myId != null) {
            if (contactsListener != null) root.child("contacts").child(myId).removeEventListener(contactsListener);
            if (requestsListener != null) root.child("friend_requests").child(myId).removeEventListener(requestsListener);
        }
        binding = null;
        super.onDestroyView();
    }
}