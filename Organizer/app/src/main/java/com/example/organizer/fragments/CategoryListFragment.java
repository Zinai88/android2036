package com.example.organizer.fragments;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.example.organizer.R;
import com.example.organizer.adapters.CategoryAdapter;
import com.example.organizer.database.AppDatabase;
import com.example.organizer.database.CategoryDao;
import com.example.organizer.database.TaskDao;
import com.example.organizer.models.Category;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
public class CategoryListFragment extends Fragment {
    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private List<Category> categoryList = new ArrayList<>();
    private CategoryDao categoryDao;
    TaskDao taskDao;
    public CategoryListFragment() {
        // Порожній конструктор
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_category_list, container, false);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        AppDatabase db = AppDatabase.getInstance(getContext());
        categoryDao = db.categoryDao();
        recyclerView = rootView.findViewById(R.id.categoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapter = new CategoryAdapter(getContext(), categoryList, true, category -> deleteCategory(category));
        recyclerView.setAdapter(categoryAdapter);
        loadCategoriesForUser();
        return rootView;
    }
    private void loadCategoriesForUser() {
        if(isInternetAvailable()){
            String currentUserUid = auth.getCurrentUser().getUid();
            firestore.collection("categories")
                    .whereEqualTo("userId", currentUserUid)  // Запит за користувачем
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Category> categories = new ArrayList<>();
                        for (var document : queryDocumentSnapshots) {
                            Category category = document.toObject(Category.class);
                            categories.add(category);
                        }
                        Log.d("CategoryListFragment", "Loaded categories from Firestore: " + categories.size());
                        getActivity().runOnUiThread(() -> {
                            categoryList.clear();
                            categoryList.addAll(categories);
                            categoryAdapter.notifyDataSetChanged();
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Помилка завантаження з Firestore", Toast.LENGTH_SHORT).show());
        }
        else{
            String currentUserUid = auth.getCurrentUser().getUid();
            new Thread(() -> {
                List<Category> categories = categoryDao.getCategoriesByUser(currentUserUid);
                Log.d("CategoryListFragment", "Loaded categories: " + categories.size());
                getActivity().runOnUiThread(() -> {
                    categoryList.clear();
                    categoryList.addAll(categories);
                    categoryAdapter.notifyDataSetChanged();
                    Log.d("CategoryListFragment", "Categories updated in adapter");
                });
            }).start();
        }
    }
    private void deleteCategory(Category category) {
        if(isInternetAvailable()){
            firestore.collection("tasks")
                    .whereEqualTo("nameCT", category.getName().toString())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            firestore.collection("tasks").document(document.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {})
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Помилка видалення завдання", Toast.LENGTH_SHORT).show();
                                    });
                        }
                        firestore.collection("categories").document(category.getFirebaseId().toString())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Категорію видалено FS", Toast.LENGTH_SHORT).show();
                                    loadCategoriesForUser();
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Помилка видалення категорії", Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Помилка отримання завдань", Toast.LENGTH_SHORT).show());
        }
        else{
            new Thread(() -> {
                AppDatabase db = Room.databaseBuilder(getContext(), AppDatabase.class, "organizer_db").build();
                TaskDao taskDao = db.taskDao();
                CategoryDao categoryDao = db.categoryDao();

                if (taskDao != null && categoryDao != null) {
                    taskDao.deleteTasksByCategoryId(category.getName());
                    categoryDao.deleteCategory(category);
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Категорію видалено Room", Toast.LENGTH_SHORT).show();
                        loadCategoriesForUser();
                    });
                } else {
                    Log.e("CategoryListFragment", "taskDao or categoryDao is null");
                }
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