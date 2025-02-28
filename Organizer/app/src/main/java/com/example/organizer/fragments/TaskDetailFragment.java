package com.example.organizer.fragments;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.organizer.R;
public class TaskDetailFragment extends Fragment {
    private static final String ARG_TASK_NAME = "task_name";
    private static final String ARG_TASK_DESCRIPTION = "task_description";
    public static TaskDetailFragment newInstance(String name, String description) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TASK_NAME, name);
        args.putString(ARG_TASK_DESCRIPTION, description);
        fragment.setArguments(args);
        return fragment;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_task_detail, container, false);
        TextView taskNameTextView = rootView.findViewById(R.id.taskNameTextView);
        TextView taskDescriptionTextView = rootView.findViewById(R.id.taskDescriptionTextView);
        if (getArguments() != null) {
            String taskName = getArguments().getString(ARG_TASK_NAME);
            String taskDescription = getArguments().getString(ARG_TASK_DESCRIPTION);
            taskNameTextView.setText(taskName);
            taskDescriptionTextView.setText(taskDescription);
        }
        return rootView;
    }
}