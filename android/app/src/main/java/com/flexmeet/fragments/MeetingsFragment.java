package com.flexmeet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.flexmeet.activities.VideoCallActivity;
import com.flexmeet.adapters.MeetingAdapter;
import com.flexmeet.databinding.FragmentMeetingsBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.Meeting;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class MeetingsFragment extends Fragment implements MeetingAdapter.OnMeetingClickListener {

    private FragmentMeetingsBinding binding;
    private MeetingAdapter adapter;
    private List<Meeting> meetingList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMeetingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        loadMeetings();

        binding.swipeRefresh.setOnRefreshListener(this::loadMeetings);
    }

    private void setupRecyclerView() {
        adapter = new MeetingAdapter(this);
        adapter.setHistoryView(true); // Set to history mode to hide buttons
        binding.meetingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.meetingsRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onMeetingClick(Meeting meeting) {
        // History mode usually disables joining, but we can allow viewing details if needed
    }

    @Override
    public void onDeleteClick(Meeting meeting) {
        // Buttons hidden in history mode
    }

    private void loadMeetings() {
        binding.swipeRefresh.setRefreshing(true);
        String currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            binding.swipeRefresh.setRefreshing(false);
            return;
        }

        long currentTime = System.currentTimeMillis();

        FirebaseFirestore.getInstance().collection("meetings")
                .whereEqualTo("hostId", currentUserId)
                .whereLessThan("scheduledTime", currentTime) // Past meetings only
                .orderBy("scheduledTime", Query.Direction.DESCENDING) // Newest first
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    meetingList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Meeting meeting = doc.toObject(Meeting.class);
                        if (meeting != null) {
                            meeting.setMeetingId(doc.getId());
                            meetingList.add(meeting);
                        }
                    }

                    if (meetingList.isEmpty()) {
                        binding.emptyState.setVisibility(View.VISIBLE);
                        binding.meetingsRecyclerView.setVisibility(View.GONE);
                    } else {
                        binding.emptyState.setVisibility(View.GONE);
                        binding.meetingsRecyclerView.setVisibility(View.VISIBLE);
                        adapter.setMeetings(new ArrayList<>(meetingList));
                    }
                    binding.swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Failed to load meetings", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
