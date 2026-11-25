package com.flexmeet.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.flexmeet.activities.DashboardActivity;
import com.flexmeet.activities.VideoCallActivity;
import com.flexmeet.adapters.MeetingAdapter;
import com.flexmeet.databinding.FragmentHomeBinding;
import com.flexmeet.firebase.FirebaseManager;
import com.flexmeet.models.Meeting;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements MeetingAdapter.OnMeetingClickListener {

    private FragmentHomeBinding binding;
    private MeetingAdapter adapter;
    private List<Meeting> meetingList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupWelcomeText();
        setupButtons();
        setupRecyclerView();
        loadUpcomingMeetings();
    }

    private void setupWelcomeText() {
        String userEmail = FirebaseManager.getInstance().getCurrentUserEmail();
        if (userEmail != null) {
            String firstName = userEmail.split("@")[0];
            binding.welcomeText.setText("Hello, " + firstName + "!");
        }
    }

    private void setupButtons() {
        binding.startMeetingBtn.setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).startNewMeeting();
            }
        });

        binding.joinMeetingBtn.setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).showJoinDialog();
            }
        });

        binding.scheduleMeetingBtn.setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).scheduleMeeting();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new MeetingAdapter(this);
        binding.meetingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.meetingsRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onMeetingClick(Meeting meeting) {
        if (getActivity() instanceof DashboardActivity) {
            ((DashboardActivity) getActivity()).openMeeting(meeting.getMeetingId());
        }
    }

    @Override
    public void onDeleteClick(Meeting meeting) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Meeting")
                .setMessage("Are you sure you want to permanently delete this meeting?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    performDelete(meeting);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(Meeting meeting) {
        FirebaseFirestore.getInstance().collection("meetings").document(meeting.getMeetingId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Meeting deleted permanently", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUpcomingMeetings() {
        String currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) return;

        long currentTime = System.currentTimeMillis();

        FirebaseFirestore.getInstance().collection("meetings")
                .whereEqualTo("hostId", currentUserId)
                .whereGreaterThan("scheduledTime", currentTime) // ONLY FUTURE MEETINGS
                .orderBy("scheduledTime", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (binding == null) return;

                    if (error != null) {
                        return;
                    }

                    meetingList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Meeting meeting = doc.toObject(Meeting.class);
                            if (meeting != null) {
                                meeting.setMeetingId(doc.getId());
                                meetingList.add(meeting);
                            }
                        }
                    }

                    if (meetingList.isEmpty()) {
                        binding.emptyStateLayout.setVisibility(View.VISIBLE);
                        binding.meetingsRecyclerView.setVisibility(View.GONE);
                    } else {
                        binding.emptyStateLayout.setVisibility(View.GONE);
                        binding.meetingsRecyclerView.setVisibility(View.VISIBLE);
                        adapter.setMeetings(new ArrayList<>(meetingList));
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
