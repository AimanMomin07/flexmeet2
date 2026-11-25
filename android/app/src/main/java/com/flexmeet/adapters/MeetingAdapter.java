package com.flexmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.flexmeet.R;
import com.flexmeet.models.Meeting;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MeetingAdapter extends RecyclerView.Adapter<MeetingAdapter.ViewHolder> {

    private List<Meeting> meetings = new ArrayList<>();
    private OnMeetingClickListener listener;
    private boolean isHistoryView = false;
    private SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("EEE, MMM d, hh:mm a", Locale.getDefault());

    public interface OnMeetingClickListener {
        void onMeetingClick(Meeting meeting);
        void onDeleteClick(Meeting meeting);
    }

    public MeetingAdapter(OnMeetingClickListener listener) {
        this.listener = listener;
    }

    public void setHistoryView(boolean historyView) {
        this.isHistoryView = historyView;
    }

    public void setMeetings(List<Meeting> newMeetings) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return meetings.size();
            }

            @Override
            public int getNewListSize() {
                return newMeetings.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return meetings.get(oldItemPosition).getMeetingId().equals(
                        newMeetings.get(newItemPosition).getMeetingId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Meeting oldM = meetings.get(oldItemPosition);
                Meeting newM = newMeetings.get(newItemPosition);
                return oldM.getTitle().equals(newM.getTitle()) &&
                        oldM.getScheduledTime() == newM.getScheduledTime();
            }
        });
        this.meetings.clear();
        this.meetings.addAll(newMeetings);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_meeting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meeting meeting = meetings.get(position);
        holder.titleTv.setText(meeting.getTitle());
        holder.timeTv.setText(dateTimeFormatter.format(new Date(meeting.getScheduledTime())));

        if (isHistoryView) {
            // HISTORY MODE: Hide buttons, show status
            holder.joinBtn.setVisibility(View.GONE);
            holder.deleteBtn.setVisibility(View.GONE);
            
            // For now, let's use the meeting status field or a default
            String status = meeting.getStatus() != null ? meeting.getStatus() : "Completed";
            holder.timeTv.setText(dateTimeFormatter.format(new Date(meeting.getScheduledTime())) + " • " + status);
        } else {
            // UPCOMING MODE: Show buttons
            holder.joinBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setVisibility(View.VISIBLE);

            holder.joinBtn.setOnClickListener(v -> {
                if (listener != null) listener.onMeetingClick(meeting);
            });

            holder.deleteBtn.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(meeting);
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (!isHistoryView && listener != null) listener.onMeetingClick(meeting);
        });
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTv, timeTv;
        View joinBtn, deleteBtn;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTv = itemView.findViewById(R.id.meeting_title);
            timeTv = itemView.findViewById(R.id.meeting_time);
            joinBtn = itemView.findViewById(R.id.join_now_btn);
            deleteBtn = itemView.findViewById(R.id.delete_btn);
        }
    }
}
