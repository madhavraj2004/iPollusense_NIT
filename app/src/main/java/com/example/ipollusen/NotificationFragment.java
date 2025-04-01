package com.example.ipollusen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private RecyclerView notificationList;
    private TextView notificationBadge;
    private List<String> notifications;
    private NotificationAdapter notificationAdapter;

    // This receiver will be used to catch the local broadcast from the service
    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check for the custom broadcast
            if ("com.example.ipollusen.NEW_NOTIFICATION".equals(intent.getAction())) {
                String notificationMessage = intent.getStringExtra("notification_message");
                if (notificationMessage != null) {
                    addNotification(notificationMessage);
                }
            }
        }
    };

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_notification, container, false);

        notificationList = rootView.findViewById(R.id.notificationList);
        notificationBadge = rootView.findViewById(R.id.notificationBadge);

        notifications = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notifications);

        notificationList.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationList.setAdapter(notificationAdapter);



        updateBadge();

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register the receiver to listen for notifications from the service
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(notificationReceiver,
                new IntentFilter("com.example.ipollusen.NEW_NOTIFICATION"));
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unregister the receiver to avoid memory leaks
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(notificationReceiver);
    }

    // Adds a new notification to the list and updates the UI
    public void addNotification(String notificationMessage) {
        notifications.add(notificationMessage);
        notificationAdapter.notifyDataSetChanged();
        updateBadge();
    }

    // Update the badge to show the number of new notifications
    private void updateBadge() {
        int newNotificationsCount = notifications.size();
        if (newNotificationsCount > 0) {
            notificationBadge.setText(String.valueOf(newNotificationsCount));
            notificationBadge.setVisibility(View.VISIBLE);
        } else {
            notificationBadge.setVisibility(View.GONE);
        }
    }

    // Adapter for displaying notifications in RecyclerView
    private static class NotificationAdapter extends RecyclerView.Adapter<NotificationViewHolder> {

        private final List<String> notifications;

        public NotificationAdapter(List<String> notifications) {
            this.notifications = notifications;
        }

        @Override
        public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(NotificationViewHolder holder, int position) {
            String notification = notifications.get(position);
            holder.notificationText.setText(notification);
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }
    }

    // ViewHolder for individual notification items
    private static class NotificationViewHolder extends RecyclerView.ViewHolder {

        TextView notificationText;

        public NotificationViewHolder(View itemView) {
            super(itemView);
            notificationText = itemView.findViewById(R.id.notification_content);
        }
    }
}
