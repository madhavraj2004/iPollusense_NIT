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

    public interface OnRoomClickListener {
        void onRoomClick(int position);
        default void onEditRoom(int position) {}
        default void onDeleteRoom(int position) {}
    }

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
        RoomModel room = roomList.get(position);
        holder.roomName.setText(room.getRoomName());

        holder.itemView.setOnClickListener(v -> listener.onRoomClick(position));

        if (holder.editRoom != null) {
            holder.editRoom.setOnClickListener(v -> listener.onEditRoom(position));
        }

        if (holder.deleteRoom != null) {
            holder.deleteRoom.setOnClickListener(v -> listener.onDeleteRoom(position));
        }
    }

    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView roomName;
        ImageView editRoom, deleteRoom;

        ViewHolder(View itemView) {
            super(itemView);
            roomName = itemView.findViewById(R.id.roomName);
            editRoom = itemView.findViewById(R.id.editIcon);
            deleteRoom = itemView.findViewById(R.id.deleteIcon);
        }
    }
}
