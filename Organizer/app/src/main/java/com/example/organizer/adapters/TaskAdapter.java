package com.example.organizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.organizer.R;
import com.example.organizer.models.Category;
import com.example.organizer.models.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnEditClickListener editClickListener;
    private OnItemClickListener itemClickListener;
    private List<Category> categories = new ArrayList<>();

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public TaskAdapter(ArrayList<Task> taskList) {
        this.taskList = taskList;
    }

    public interface OnEditClickListener {
        void onEdit(Task task);
    }

    public interface OnItemClickListener {
        void onItemClick(Task task);
    }

    public TaskAdapter(List<Task> taskList, OnEditClickListener editClickListener, OnItemClickListener itemClickListener) {
        this.taskList = taskList;
        this.editClickListener = editClickListener;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.titleTextView.setText(task.getTitle());

        String dueDate = task.getDueDate() != null ? task.getDueDate().toString() : "Невизначено";
        holder.dueDateTextView.setText(dueDate);

        // Обработка нажатия на кнопку редактирования
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(task);
            }
        });

        holder.editButton.setOnClickListener(v -> {
            if (editClickListener != null) {
                editClickListener.onEdit(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public void setTaskList(List<Task> tasks) {
        if (tasks != null) {
            taskList = new ArrayList<>(tasks);
            notifyDataSetChanged();
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView dueDateTextView;
        ImageButton editButton;

        public TaskViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.taskTitleTextView);
            dueDateTextView = itemView.findViewById(R.id.taskDueDateTextView);
            editButton = itemView.findViewById(R.id.editTaskButton);
        }
    }
}