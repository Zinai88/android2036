package com.example.organizer.fragments;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.*;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.*;
import androidx.room.Room;
import com.example.organizer.R;
import com.example.organizer.adapters.CategorySpinnerAdapter;
import com.example.organizer.adapters.TaskAdapter;
import com.example.organizer.database.AppDatabase;
import com.example.organizer.database.CategoryDao;
import com.example.organizer.database.TaskDao;
import com.example.organizer.models.Category;
import com.example.organizer.models.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends Fragment {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private Spinner categorySpinner;
    private TaskDao taskDao;
    private List<Category> categoryList = new ArrayList<>();
    private List<Category> categoriesList = new ArrayList<>();
    private CategorySpinnerAdapter categorySpinnerAdapter;

    public TaskListFragment() {
        // Порожній публічний конструктор (вимагається для фрагментів)
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_task_list, container, false);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        recyclerView = rootView.findViewById(R.id.taskRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new TaskAdapter(new ArrayList<>());
        recyclerView.setAdapter(taskAdapter);

        Button editCategoryButton = rootView.findViewById(R.id.editCategoryButton);

        taskAdapter = new TaskAdapter(new ArrayList<>(), new TaskAdapter.OnEditClickListener() {
            @Override
            public void onEdit(Task task) {
                openTaskEditFragment(task);
            }
        }, new TaskAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Task task) {
                openTaskDetailFragment(task);
            }
        });
        recyclerView.setAdapter(taskAdapter);

        categorySpinner = rootView.findViewById(R.id.categorySpinner);
        categorySpinnerAdapter = new CategorySpinnerAdapter(getContext(), categoryList);
        categorySpinner.setAdapter(categorySpinnerAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Category selectedCategory = categoryList.get(position);
                String selectedCategoryName = selectedCategory.getName();
                if(isInternetAvailable()){
                    loadTasksForUser(selectedCategoryName);
                }
                else{
                    loadTasksFromRoom(selectedCategoryName);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                if(isInternetAvailable()){
                    loadTasksForUser(null);
                }
                else{
                    loadTasksFromRoom(null);
                }
            }
        });

        // Обробка довгого натискання для відкриття створення категорії
        categorySpinner.setOnLongClickListener(v -> {
            openCategoryCreateFragment();
            return true;
        });

        // Завантажуємо категорії
        if(isInternetAvailable()){
            loadCategoriesForUser();
        }
        else loadCategoriesFromRoom();

        // Обробник кнопки створення завдання
        rootView.findViewById(R.id.createTaskButton).setOnClickListener(v -> openTaskCreateFragment());
        rootView.findViewById(R.id.editCategoryButton).setOnClickListener(v -> openCategoryListFragment());

        return rootView;
    }


    // Відкриття фрагмента для створення завдання
    private void openTaskCreateFragment() {
        TaskCreateFragment taskCreateFragment = new TaskCreateFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, taskCreateFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openCategoryListFragment() {
        CategoryListFragment categoryListFragment = new CategoryListFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, categoryListFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // Відкриття фрагмента для створення категорії
    private void openCategoryCreateFragment() {
        CategoryCreateFragment categoryCreateFragment = new CategoryCreateFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, categoryCreateFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void openTaskDetailFragment(Task task) {
        TaskDetailFragment taskDetailFragment = TaskDetailFragment.newInstance(task.getTitle(), task.getDescription());
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, taskDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // Завантаження завдань для поточного користувача з фільтрацією по категорії
    private void loadTasksForUser(String nameCT) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Користувач не авторизований", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserUid = auth.getCurrentUser().getUid();
        // Якщо категорія не вибрана, то показуємо всі завдання
        if (nameCT == null) {
            nameCT = ""; // Показати всі завдання
        }
        firestore.collection("tasks")
                .whereEqualTo("userId", currentUserUid)
                .whereEqualTo("nameCT", nameCT) // Фільтруємо завдання за категорією
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<Task> tasks = queryDocumentSnapshots.toObjects(Task.class);
                        taskAdapter.setTaskList(tasks);
                        Toast.makeText(getContext(), "Завдання завантажено FS", Toast.LENGTH_SHORT).show();
                    } else {
                        taskAdapter.setTaskList(new ArrayList<>());
                    }
                });
    }

    private void loadTasksFromRoom(String categoryName) {
        AppDatabase db = AppDatabase.getInstance(getContext());
        taskDao = db.taskDao();
        new Thread(() -> {
            List<Task> tasks;
            if (categoryName != null) {
                tasks = taskDao.getTasksByCategory(categoryName);
            } else {
                tasks = taskDao.getAllTasks();
            }
            // Оновлюємо UI з головного потоку
            requireActivity().runOnUiThread(() -> taskAdapter.setTaskList(tasks));
        }).start();
    }


    private void openTaskEditFragment(Task task) {
        TaskEditFragment taskEditFragment = TaskEditFragment.newInstance(task);
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, taskEditFragment);
        transaction.addToBackStack(null);
        transaction.commit();
   }

    // Завантаження категорій для поточного користувача
    private void loadCategoriesForUser() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId != null) {
            firestore.collection("categories")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Category> categories = queryDocumentSnapshots.toObjects(Category.class);
                        categoryList.clear();
                        categoryList.addAll(categories);
                        categorySpinnerAdapter.notifyDataSetChanged();
                        if (!categoryList.isEmpty()) {
                            categorySpinner.setSelection(0);
                            loadTasksForUser(categoryList.get(0).getName());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (e != null) {
                            Toast.makeText(getContext(), "Помилка оновлення категорій", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    });
        }
    }

    private void loadCategoriesFromRoom() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId != null) {
            new Thread(() -> {
                // Отримуємо всі категорії для користувача через Room
                AppDatabase db = Room.databaseBuilder(getContext(), AppDatabase.class, "organizer_db").build();
                CategoryDao categoryDao = db.categoryDao();
                List<Category> categories = categoryDao.getCategoriesByUser(userId);
                // Оновлюємо UI в основному потоці
                getActivity().runOnUiThread(() -> {
                    categoryList.clear();
                    categoryList.addAll(categories);
                    categorySpinnerAdapter.notifyDataSetChanged();
                    if (!categoryList.isEmpty()) {
                        categorySpinner.setSelection(0);
                        loadTasksFromRoom(categoryList.get(0).getName());
                    }
                });
            }).start();
        }
    }
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }
}