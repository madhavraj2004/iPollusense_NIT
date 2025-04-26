package com.example.ipollusen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.ViewHolder> {
    private final List<RoomModel> roomList;
    private final OnRoomClickListener listener;

    // Listener interface for handling room actions
    public interface OnRoomClickListener {
        void onRoomClick(int position);      // Triggered when a room is clicked
        void onEditRoom(int position);      // Triggered when edit button is clicked
        void onDeleteRoom(int position);    // Triggered when delete button is clicked
    }

    // Constructor
    public RoomAdapter(List<RoomModel> roomList, OnRoomClickListener listener) {
        this.roomList = roomList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the current room
        RoomModel room = roomList.get(position);

        // Bind room data to UI components
        holder.roomName.setText(room.getRoomName());

        // Set click listeners
        holder.itemView.setOnClickListener(v -> listener.onRoomClick(position));
        holder.editRoom.setOnClickListener(v -> listener.onEditRoom(position));
        holder.deleteRoom.setOnClickListener(v -> listener.onDeleteRoom(position));
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    // ViewHolder class to hold room item views
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView roomName;
        final ImageView editRoom, deleteRoom;

        ViewHolder(View itemView) {
            super(itemView);
            roomName = itemView.findViewById(R.id.roomName);
            editRoom = itemView.findViewById(R.id.editIcon);
            deleteRoom = itemView.findViewById(R.id.deleteIcon);
        }
    }
}