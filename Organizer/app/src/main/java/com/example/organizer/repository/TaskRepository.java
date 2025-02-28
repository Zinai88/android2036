package com.example.organizer.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.organizer.database.AppDatabase;
import com.example.organizer.database.TaskDao;
import com.example.organizer.models.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private final TaskDao taskDao;
    private final ExecutorService executorService;
    private final FirebaseFirestore db;
    private final AppDatabase appDatabase;

    public TaskRepository(Context context) {
        appDatabase = AppDatabase.getInstance(context);
        AppDatabase database = AppDatabase.getInstance(context);
        taskDao = database.taskDao();
        executorService = Executors.newSingleThreadExecutor();
        db = FirebaseFirestore.getInstance();
    }

    // Додавання завдання до бази даних
    public void insert(Task task) {
        new Thread(() -> appDatabase.taskDao().insert(task)).start();  // Виконання в окремому потоці
    }

    // Завантаження всіх завдань з Room
    public LiveData<List<Task>> getAllTasks() {
        return (LiveData<List<Task>>) taskDao.getAllTasks();
    }

    // Оновлення завдання
    public void updateTask(Task task) {
        executorService.execute(() -> {
            taskDao.updateTask(task);
            db.collection("tasks").document(String.valueOf(task.getId()))
                    .set(task)
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Task оновлено"))
                    .addOnFailureListener(e -> Log.e("Firebase", "Помилка оновлення", e));
        });
    }

    // Видалення завдання
    public void deleteTask(Task task) {
        executorService.execute(() -> {
            taskDao.deleteTask(task);
            db.collection("tasks").document(String.valueOf(task.getId()))
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Task видалено"))
                    .addOnFailureListener(e -> Log.e("Firebase", "Помилка видалення", e));
        });
    }
}
