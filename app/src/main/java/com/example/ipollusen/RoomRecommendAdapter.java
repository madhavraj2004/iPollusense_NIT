package com.example.ipollusen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RoomRecommendAdapter extends RecyclerView.Adapter<RoomRecommendAdapter.RoomViewHolder> {

    private List<RoomRecommendModel> roomList;
    private OnPlayClickListener playClickListener;
    private OnChatClickListener chatClickListener;
    private OnRoomClickListener roomClickListener;  // NEWLY ADDED


    // Callback interface for play button clicks.
    public interface OnPlayClickListener {
        void onPlayClick(RoomRecommendModel room, ImageButton playButton, ImageButton chatButton);
    }

    // Callback interface for chat bubble clicks.
    public interface OnChatClickListener {
        void onChatClick(RoomRecommendModel room);
    }

    // Callback interface for room card click
    public interface OnRoomClickListener {
        void onRoomClick(int position);
    }

    public RoomRecommendAdapter(List<RoomRecommendModel> roomList,
                                OnPlayClickListener playClickListener,
                                OnChatClickListener chatClickListener,
                                OnRoomClickListener roomClickListener) {
        this.roomList = roomList;
        this.playClickListener = playClickListener;
        this.chatClickListener = chatClickListener;
        this.roomClickListener = roomClickListener;
    }


    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommend, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomRecommendModel room = roomList.get(position);
        holder.roomName.setText(room.getRoomName());

        holder.btnPlayPause.setSelected(false);
        holder.btnChatBubble.setVisibility(View.GONE);

        holder.btnPlayPause.setOnClickListener(v -> {
            if (playClickListener != null) {
                playClickListener.onPlayClick(room, holder.btnPlayPause, holder.btnChatBubble);
            }
        });

        holder.btnChatBubble.setOnClickListener(v -> {
            if (chatClickListener != null) {
                chatClickListener.onChatClick(room);
            }
        });

        // Handle Room Card Click
        holder.itemView.setOnClickListener(v -> {
            if (roomClickListener != null) {
                roomClickListener.onRoomClick(holder.getAdapterPosition());
            }
        });



        // Show or Hide Dot
        if (room.isHasNewRecommendation()) {
            holder.newRecDot.setVisibility(View.VISIBLE);
        } else {
            holder.newRecDot.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }


    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView roomName;
        ImageButton btnPlayPause;
        ImageButton btnChatBubble;
        TextView newRecDot;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            roomName = itemView.findViewById(R.id.roomName);
            btnPlayPause = itemView.findViewById(R.id.btnPlayPause);
            btnChatBubble = itemView.findViewById(R.id.btnChatBubble);
            newRecDot = itemView.findViewById(R.id.newRecDot);
        }
    }
}
