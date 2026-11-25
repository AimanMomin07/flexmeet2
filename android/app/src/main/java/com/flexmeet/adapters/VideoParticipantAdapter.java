package com.flexmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.flexmeet.R;
import com.google.android.material.imageview.ShapeableImageView;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class VideoParticipantAdapter extends RecyclerView.Adapter<VideoParticipantAdapter.ViewHolder> {

    private final EglBase.Context eglContext;
    private final List<Participant> participants = new ArrayList<>();

    public VideoParticipantAdapter(EglBase.Context eglContext) {
        this.eglContext = eglContext;
        setHasStableIds(true);
    }

    public void setParticipants(List<Participant> values) {
        participants.clear();
        participants.addAll(values);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return participants.get(position).id.hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_participant, parent, false);
        return new ViewHolder(view, eglContext);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Participant participant = participants.get(position);
        holder.initializeRenderer();
        if (holder.participant != null && holder.participant.videoTrack != null
                && holder.participant.videoTrack != participant.videoTrack) {
            holder.participant.videoTrack.removeSink(holder.videoView);
        }
        holder.participant = participant;
        holder.nameTv.setText(participant.name);

        if (participant.videoTrack != null) {
            participant.videoTrack.removeSink(holder.videoView);
            holder.videoView.setVisibility(View.VISIBLE);
            holder.avatarIv.setVisibility(View.GONE);
            participant.videoTrack.addSink(holder.videoView);
        } else {
            holder.videoView.setVisibility(View.GONE);
            holder.avatarIv.setVisibility(View.VISIBLE);
            if (participant.photoUrl != null && !participant.photoUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(participant.photoUrl).into(holder.avatarIv);
            } else {
                holder.avatarIv.setImageResource(android.R.drawable.ic_menu_myplaces);
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        if (holder.participant != null && holder.participant.videoTrack != null) {
            holder.participant.videoTrack.removeSink(holder.videoView);
        }
        holder.participant = null;
        holder.videoView.release();
        holder.initialized = false;
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final SurfaceViewRenderer videoView;
        final ShapeableImageView avatarIv;
        final TextView nameTv;
        final EglBase.Context eglContext;
        Participant participant;
        boolean initialized;

        void initializeRenderer() {
            if (!initialized) {
                videoView.init(eglContext, null);
                videoView.setEnableHardwareScaler(true);
                initialized = true;
            }
        }

        ViewHolder(@NonNull View itemView, EglBase.Context eglContext) {
            super(itemView);
            videoView = itemView.findViewById(R.id.video_view);
            avatarIv = itemView.findViewById(R.id.participant_avatar);
            nameTv = itemView.findViewById(R.id.participant_name);
            this.eglContext = eglContext;
            initializeRenderer();
        }
    }

    public static class Participant {
        public final String id;
        public final String name;
        public final String photoUrl;
        public final VideoTrack videoTrack;

        public Participant(String id, String name, String photoUrl, VideoTrack videoTrack) {
            this.id = id;
            this.name = name;
            this.photoUrl = photoUrl;
            this.videoTrack = videoTrack;
        }
    }
}
