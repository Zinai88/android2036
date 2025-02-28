package com.example.organizer.fragments;
import android.app.DatePickerDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.example.organizer.R;
import com.example.organizer.models.Category;
import com.example.organizer.models.Task;
import com.example.organizer.repository.CategoryRepository;
import com.example.organizer.repository.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;
public class TaskCreateFragment extends Fragment {
    private EditText titleEditText;
    private EditText descriptionEditText;
    private EditText dueDateEditText;
    private Spinner categorySpinner;
    private TaskRepository taskRepository;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Category> categoryList;
    private List<String> categoryNames;
    public TaskCreateFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_task_create, container, false);
        titleEditText = rootView.findViewById(R.id.titleEditText);
        descriptionEditText = rootView.findViewById(R.id.descriptionEditText);
        dueDateEditText = rootView.findViewById(R.id.dueDateEditText);
        categorySpinner = rootView.findViewById(R.id.categorySpinner);
        taskRepository = new TaskRepository(getContext());
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if(isInternetAvailable()){
            loadCategories();
        }
        else loadCategoriesFromRoom();
        rootView.findViewById(R.id.saveButton).setOnClickListener(v -> saveTask());
        return rootView;
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText dueDateEditText = view.findViewById(R.id.dueDateEditText);
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dueDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(
                        getContext(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                calendar.set(Calendar.YEAR, year);
                                calendar.set(Calendar.MONTH, month);
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                dueDateEditText.setText(dateFormat.format(calendar.getTime()));
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                datePicker.show();
            }
        });
    }
    private void loadCategories() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("categories")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList = new ArrayList<>();
                    categoryNames = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Category category = document.toObject(Category.class);
                        categoryList.add(category);
                        categoryNames.add(category.getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, categoryNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Не вдалося завантажити категорії", Toast.LENGTH_SHORT).show();
                });
    }
    private void loadCategoriesFromRoom() {
        String userId = auth.getCurrentUser().getUid();
        CategoryRepository categoryRepository = new CategoryRepository(getActivity().getApplication());
        categoryRepository.getCategoriesByUserId(userId, new CategoryRepository.Callback<List<Category>>() {
            @Override
            public void onResult(List<Category> categories) {
                categoryList = new ArrayList<>();
                categoryNames = new ArrayList<>();

                for (Category category : categories) {
                    categoryList.add(category);
                    categoryNames.add(category.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(adapter);
            }
        });
    }
    private void saveTask() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String dueDate = dueDateEditText.getText().toString().trim();
        int categoryIndex = categorySpinner.getSelectedItemPosition();
        String userId = auth.getCurrentUser().getUid();
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description) || TextUtils.isEmpty(dueDate) || categoryIndex == -1) {
            Toast.makeText(getContext(), "Заповніть всі поля", Toast.LENGTH_SHORT).show();
            return;
        }
        if (categoryIndex == -1 || categoryList.isEmpty()) {
            Toast.makeText(getContext(), "Будь ласка, виберіть категорію", Toast.LENGTH_SHORT).show();
            return;
        }
        Category selectedCategory = categoryList.get(categoryIndex);
        Task task = new Task(title, description, dueDate, "Заплановано", userId);
        task.setNameCT(selectedCategory.getName());
        if (isInternetAvailable()) {
            saveTaskToFirebase(task);
        } else {
            saveTaskToRoom(task);
        }
    }
    private void saveTaskToFirebase(Task task) {
        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    String taskId = documentReference.getId();
                    db.collection("tasks").document(taskId)
                            .update("firebaseId", taskId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Завдання збережено", Toast.LENGTH_SHORT).show();
                                getActivity().onBackPressed();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Помилка оновлення ID", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Помилка збереження", Toast.LENGTH_SHORT).show();
                });
    }
    private void saveTaskToRoom(Task task) {
        new Thread(() -> {
            taskRepository.insert(task);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Завдання збережено локально", Toast.LENGTH_SHORT).show();
                    }
                    if (isAdded()) {
                        getParentFragmentManager().popBackStack();
                    }
                });
            }
        }).start();
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