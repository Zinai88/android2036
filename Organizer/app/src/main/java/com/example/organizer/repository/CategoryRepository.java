package com.example.organizer.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.example.organizer.database.AppDatabase;
import com.example.organizer.database.CategoryDao;
import com.example.organizer.models.Category;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CategoryRepository {
    private CategoryDao categoryDao;
    private Executor executor;

    public CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
        executor = Executors.newSingleThreadExecutor(); // Окремий потік для роботи з базою даних
    }

    public void getCategoriesByUserId(String userId, Callback<List<Category>> callback) {
        executor.execute(() -> {
            List<Category> categories = categoryDao.getCategoriesByUser(userId);
            // Викликаємо callback на головному потоці
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(categories));
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
