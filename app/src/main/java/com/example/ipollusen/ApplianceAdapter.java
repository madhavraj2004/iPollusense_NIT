package com.example.ipollusen;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ApplianceAdapter extends RecyclerView.Adapter<ApplianceAdapter.ApplianceViewHolder> {
    private final List<Appliance> applianceList;

    public ApplianceAdapter(List<Appliance> applianceList) {
        this.applianceList = applianceList;
    }

    @NonNull
    @Override
    public ApplianceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appliance, parent, false);
        return new ApplianceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplianceViewHolder holder, int position) {
        Appliance appliance = applianceList.get(position);
        holder.applianceName.setText(appliance.getName());
        holder.applianceState.setText(appliance.getState());
        holder.applianceStatus.setText(appliance.isOn() ? "On" : "Off");
        holder.applianceSmart.setText(appliance.isSmart() ? "Yes" : "No");
    }

    @Override
    public int getItemCount() {
        return applianceList.size();
    }

    static class ApplianceViewHolder extends RecyclerView.ViewHolder {
        TextView applianceName, applianceState, applianceStatus, applianceSmart;

        public ApplianceViewHolder(@NonNull View itemView) {
            super(itemView);
            applianceName = itemView.findViewById(R.id.applianceName);
            applianceState = itemView.findViewById(R.id.applianceState);
            applianceStatus = itemView.findViewById(R.id.applianceStatus);
            applianceSmart = itemView.findViewById(R.id.applianceSmart);
        }
    }
}
