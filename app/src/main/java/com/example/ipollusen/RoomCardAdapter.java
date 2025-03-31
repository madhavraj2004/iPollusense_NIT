package com.example.ipollusen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RoomCardAdapter extends RecyclerView.Adapter<RoomCardAdapter.RoomCardViewHolder> {

    private List<RoomCardModel> roomCardList;

    public RoomCardAdapter(List<RoomCardModel> roomCardList) {
        this.roomCardList = roomCardList;
    }

    @NonNull
    @Override
    public RoomCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room_card, parent, false);
        return new RoomCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomCardViewHolder holder, int position) {
        RoomCardModel room = roomCardList.get(position);
        holder.txtRoomName.setText(room.getRoomName());
        holder.txtRoomDesc.setText(room.getRoomDesc());
        String dimensions = "Dimensions: " + room.getLength() + " x " + room.getBreadth();
        holder.txtRoomDimensions.setText(dimensions);
        holder.editNodeIds.setText(room.getNodeIds() != null ? room.getNodeIds() : "");
    }

    @Override
    public int getItemCount() {
        return roomCardList != null ? roomCardList.size() : 0;
    }

    public static class RoomCardViewHolder extends RecyclerView.ViewHolder {
        TextView txtRoomName, txtRoomDesc, txtRoomDimensions;
        // Using EditText for node IDs; change to TextView if read-only display is preferred.
        EditText editNodeIds;

        public RoomCardViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRoomName = itemView.findViewById(R.id.txtRoomName);
            txtRoomDesc = itemView.findViewById(R.id.txtRoomDesc);
            txtRoomDimensions = itemView.findViewById(R.id.txtRoomDimensions);
            editNodeIds = itemView.findViewById(R.id.shownodeIds);
        }
    }
}
