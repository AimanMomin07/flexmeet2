package com.flexmeet.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.flexmeet.R;
import com.flexmeet.databinding.FragmentParticipantsBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ParticipantsFragment extends Fragment {

    private FragmentParticipantsBinding binding;
    private String meetingId;
    private DatabaseReference participantsRef;
    private ParticipantAdapter adapter;
    private String hostId;

    public static ParticipantsFragment newInstance(String meetingId) {
        ParticipantsFragment fragment = new ParticipantsFragment();
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
        binding = FragmentParticipantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        participantsRef = FirebaseManager.getInstance().getFirebaseDatabase()
                .getReference("meetings").child(meetingId).child("participants");

        setupRecyclerView();
        loadMeetingHostInfo();
        loadParticipants();

        binding.closeParticipants.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        binding.muteAllBtn.setOnClickListener(v -> {
            muteAllParticipants();
        });
    }

    private void setupRecyclerView() {
        adapter = new ParticipantAdapter();
        binding.participantsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.participantsRecyclerView.setAdapter(adapter);
    }

    private void loadMeetingHostInfo() {
        FirebaseFirestore.getInstance().collection("meetings").document(meetingId)
                .get().addOnSuccessListener(documentSnapshot -> {
                    hostId = documentSnapshot.getString("hostId");
                    if (FirebaseManager.getInstance().getCurrentUserId().equals(hostId)) {
                        binding.muteAllBtn.setVisibility(View.VISIBLE);
                    }
                    adapter.setHostId(hostId);
                });
    }

    private void loadParticipants() {
        participantsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> uids = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    uids.add(child.getKey());
                }
                fetchUserDetails(uids);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load participants", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserDetails(List<String> uids) {
        List<User> participants = new ArrayList<>();
        if (uids.isEmpty()) {
            adapter.setParticipants(participants);
            return;
        }

        for (String uid : uids) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get().addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            participants.add(user);
                            if (participants.size() == uids.size()) {
                                adapter.setParticipants(participants);
                            }
                        }
                    });
        }
    }

    private void muteAllParticipants() {
        participantsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (!child.getKey().equals(hostId)) {
                        child.getRef().child("isMuted").setValue(true);
                    }
                }
                Toast.makeText(getContext(), "All participants muted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {
        private List<User> participants = new ArrayList<>();
        private String hostId;

        public void setParticipants(List<User> participants) {
            this.participants = participants;
            notifyDataSetChanged();
        }

        public void setHostId(String hostId) {
            this.hostId = hostId;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_control, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = participants.get(position);
            holder.nameTv.setText(user.getDisplayName());
            
            boolean isHost = user.getUid().equals(hostId);
            holder.roleTv.setText(isHost ? "Host" : "Participant");

            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(user.getPhotoUrl()).into(holder.imageIv);
            }

            // Host controls
            boolean currentIsHost = FirebaseManager.getInstance().getCurrentUserId().equals(hostId);
            if (currentIsHost && !isHost) {
                holder.muteBtn.setVisibility(View.VISIBLE);
                holder.removeBtn.setVisibility(View.VISIBLE);
                
                holder.muteBtn.setOnClickListener(v -> {
                    participantsRef.child(user.getUid()).child("isMuted").setValue(true);
                    Toast.makeText(getContext(), "Muted " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                });

                holder.removeBtn.setOnClickListener(v -> {
                    participantsRef.child(user.getUid()).removeValue();
                    Toast.makeText(getContext(), "Removed " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                });
            } else {
                holder.muteBtn.setVisibility(View.GONE);
                holder.removeBtn.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return participants.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ShapeableImageView imageIv;
            TextView nameTv, roleTv;
            View muteBtn, removeBtn;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageIv = itemView.findViewById(R.id.participant_image);
                nameTv = itemView.findViewById(R.id.participant_name);
                roleTv = itemView.findViewById(R.id.participant_role);
                muteBtn = itemView.findViewById(R.id.mute_btn);
                removeBtn = itemView.findViewById(R.id.remove_btn);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
