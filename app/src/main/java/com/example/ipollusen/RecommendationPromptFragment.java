package com.example.ipollusen;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class RecommendationPromptFragment extends DialogFragment {
    private static final String ARG_ROOM_ID = "room_id";
    private static final String ARG_ROOM_NAME = "room_name";
    private static final String ARG_ROOM_DESC = "room_desc";

    public static RecommendationPromptFragment newInstance(String roomId, String roomName, String roomDesc) {
        RecommendationPromptFragment fragment = new RecommendationPromptFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        args.putString(ARG_ROOM_NAME, roomName);
        args.putString(ARG_ROOM_DESC, roomDesc);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recommendation_prompt, container, false);

        TextView title = view.findViewById(R.id.roomName);
        TextView desc = view.findViewById(R.id.roomDesc);
        Button closeButton = view.findViewById(R.id.closeButton);

        if (getArguments() != null) {
            title.setText(getArguments().getString(ARG_ROOM_NAME));
            desc.setText(getArguments().getString(ARG_ROOM_DESC));
        }

        closeButton.setOnClickListener(v -> dismiss());

        return view;
    }
}
