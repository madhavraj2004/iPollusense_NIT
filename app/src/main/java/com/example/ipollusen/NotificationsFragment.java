package com.example.ipollusen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView notificationsRecyclerView;
    private NotificationsAdapter adapter;
    private List<NotificationItem> notificationList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // You can initialize the notification list or any other data here if needed.
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inflate the fragment layout
        notificationsRecyclerView = view.findViewById(R.id.notifications_recycler_view);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the notification list and add sample data
        notificationList = new ArrayList<>();
        notificationList.add(new NotificationItem("Sample Notification 1", "This is the content of notification 1"));
        notificationList.add(new NotificationItem("Sample Notification 2", "This is the content of notification 2"));

        // Set up the adapter
        adapter = new NotificationsAdapter(notificationList);
        notificationsRecyclerView.setAdapter(adapter);

        // Set up swipe-to-dismiss functionality
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                adapter.removeItem(position);
                // Optionally, show a toast or a message confirming deletion
            }
        });

        itemTouchHelper.attachToRecyclerView(notificationsRecyclerView);
    }

    // Method to show an error dialog
    public void showErrorDialog(String message) {
        if (getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(message)
                    .setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}
