package com.flexmeet.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.flexmeet.activities.DashboardActivity;
import com.flexmeet.databinding.FragmentExploreBinding;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.*;
import java.util.Locale;

public class ExploreFragment extends Fragment {
    private FragmentExploreBinding binding;
    private DatabaseReference roomsRef;
    private ValueEventListener listener;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        roomsRef = FirebaseDatabase.getInstance().getReference("public_rooms");
        binding.progressBar.setVisibility(View.VISIBLE);
        listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.liveRoomsContainer.removeAllViews();
                int count = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    String status = child.child("status").getValue(String.class);
                    if (!"live".equals(status)) continue;
                    Long createdAt = child.child("createdAt").getValue(Long.class);
                    if (createdAt != null && createdAt > 0
                            && System.currentTimeMillis() - createdAt > 6L * 60 * 60 * 1000) continue;
                    String room = child.child("room").getValue(String.class);
                    if (room == null || room.trim().isEmpty()) room = child.getKey();
                    String title = child.child("title").getValue(String.class);
                    String host = child.child("hostName").getValue(String.class);
                    addRoom(room, title == null ? "Community room" : title, host == null ? "FlexMeet host" : host);
                    count++;
                }
                binding.emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                if (binding != null) {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyState.setText("Live rooms are unavailable right now. " + error.getMessage());
                    binding.emptyState.setVisibility(View.VISIBLE);
                }
            }
        };
        roomsRef.addValueEventListener(listener);
    }

    private void addRoom(String room, String title, String host) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setRadius(24f);
        card.setCardElevation(0f);
        card.setStrokeWidth(1);
        card.setStrokeColor(Color.rgb(225, 229, 239));
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(20, 18, 20, 18);
        LinearLayout top = new LinearLayout(requireContext());
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView live = new TextView(requireContext());
        live.setText("● LIVE");
        live.setTextColor(Color.rgb(220, 56, 78));
        live.setTextSize(12f);
        live.setTypeface(null, 1);
        TextView code = new TextView(requireContext());
        code.setText(room.toUpperCase(Locale.ROOT));
        code.setTextColor(Color.rgb(103, 112, 132));
        code.setTextSize(12f);
        code.setGravity(Gravity.END);
        code.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(live);
        top.addView(code);
        content.addView(top);
        TextView name = new TextView(requireContext());
        name.setText(title);
        name.setTextColor(Color.rgb(28, 35, 51));
        name.setTextSize(19f);
        name.setTypeface(null, 1);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = 12;
        content.addView(name, titleParams);
        TextView owner = new TextView(requireContext());
        owner.setText("Hosted by " + host);
        owner.setTextColor(Color.rgb(105, 114, 132));
        owner.setTextSize(14f);
        content.addView(owner);
        Button join = new Button(requireContext());
        join.setText("Join live room");
        join.setAllCaps(false);
        join.setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) ((DashboardActivity) getActivity()).openMeeting(room);
        });
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-2, -2);
        buttonParams.topMargin = 10;
        content.addView(join, buttonParams);
        card.addView(content);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = 12;
        binding.liveRoomsContainer.addView(card, params);
    }

    @Override public void onDestroyView() {
        if (roomsRef != null && listener != null) roomsRef.removeEventListener(listener);
        binding = null;
        super.onDestroyView();
    }
}