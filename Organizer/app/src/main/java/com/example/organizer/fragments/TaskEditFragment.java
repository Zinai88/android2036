package com.example.organizer.fragments;
import android.app.DatePickerDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.organizer.R;
import com.example.organizer.database.AppDatabase;
import com.example.organizer.models.Category;
import com.example.organizer.models.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;
public class TaskEditFragment extends Fragment {
    private EditText titleEditText, descriptionEditText, dueDateEditText;
    private Spinner categorySpinner;
    private Button saveButton, deleteButton;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String taskId, taskIdFS;
    private List<Category> categoryList;
    private List<String> categoryNames;
    private String selectedCategoryId;
    private final Calendar calendar = Calendar.getInstance();
    public static TaskEditFragment newInstance(Task task) {
        TaskEditFragment fragment = new TaskEditFragment();
        Bundle args = new Bundle();
        args.putInt("taskId", task.getId());
        args.putString("taskIdFS", task.getFirebaseId());
        args.putString("title", task.getTitle());
        args.putString("description", task.getDescription());
        args.putString("dueDate", task.getDueDate());
        args.putString("nameCT", task.getNameCT());
        fragment.setArguments(args);
        return fragment;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_edit, container, false);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        titleEditText = view.findViewById(R.id.editTaskTitle);
        descriptionEditText = view.findViewById(R.id.editTaskDescription);
        dueDateEditText = view.findViewById(R.id.editTaskDueDate);
        categorySpinner = view.findViewById(R.id.editCategorySpinner);
        saveButton = view.findViewById(R.id.saveTaskButton);
        deleteButton = view.findViewById(R.id.deleteTaskButton);
        if (getArguments() != null) {
            taskId = String.valueOf(getArguments().getInt("taskId"));
            taskIdFS = getArguments().getString("taskIdFS");
            titleEditText.setText(getArguments().getString("title"));
            descriptionEditText.setText(getArguments().getString("description"));
            dueDateEditText.setText(getArguments().getString("dueDate"));
            selectedCategoryId = getArguments().getString("nameCT");
        }
        taskId = String.valueOf(getArguments().getInt("taskId", -1));
        if (taskId.equals("-1")) {
            Toast.makeText(getContext(), "Помилка отримання ID завдання", Toast.LENGTH_SHORT).show();
            return view;
        }
        dueDateEditText.setOnClickListener(v -> showDatePicker());
        if(isInternetAvailable()){
            loadCategories();
        }
        else loadCategoriesFromRoom();
        saveButton.setOnClickListener(v -> updateTask());
        deleteButton.setOnClickListener(v -> deleteTask());
        return view;
    }
    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    dueDateEditText.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }
    private void loadCategories(){
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("categories")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    categoryList = new ArrayList<>();
                    categoryNames = new ArrayList<>();
                    int selectedIndex = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Category category = document.toObject(Category.class);
                        category.setFirebaseId(document.getId());
                        categoryList.add(category);
                        categoryNames.add(category.getName());
                        if (category.getFirebaseId().equals(selectedCategoryId)) {
                            selectedIndex = categoryList.size() - 1;
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, categoryNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(adapter);
                    categorySpinner.setSelection(selectedIndex);
                });
    }
    private void loadCategoriesFromRoom() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(getContext());
            categoryList = db.categoryDao().getAllCategories();
            categoryNames = new ArrayList<>();
            int selectedIndex = 0;
            for (int i = 0; i < categoryList.size(); i++) {
                categoryNames.add(categoryList.get(i).getName());
                if (String.valueOf(categoryList.get(i).getId()).equals(selectedCategoryId)) {
                    selectedIndex = i;
                }
            }
            int finalSelectedIndex = selectedIndex;
            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(adapter);
                categorySpinner.setSelection(finalSelectedIndex);
            });
        }).start();
    }
    private void updateTask() {
        String newTitle = titleEditText.getText().toString().trim();
        String newDescription = descriptionEditText.getText().toString().trim();
        String newDueDate = dueDateEditText.getText().toString().trim();
        int categoryIndex = categorySpinner.getSelectedItemPosition();
        String newNameCT = categoryList.get(categoryIndex).getName();
        if (newTitle.isEmpty() || newDescription.isEmpty() || newDueDate.isEmpty()) {
            Toast.makeText(getContext(), "Заповніть всі поля", Toast.LENGTH_SHORT).show();
            return;
        }
        if(isInternetAvailable()){
            Map<String, Object> updatedTask = new HashMap<>();
            updatedTask.put("title", newTitle);
            updatedTask.put("description", newDescription);
            updatedTask.put("dueDate", newDueDate);
            updatedTask.put("nameCT", newNameCT);
            if (taskIdFS == null || taskIdFS.isEmpty()) {
                Toast.makeText(getContext(), "Invalid task ID", Toast.LENGTH_SHORT).show();
                return;
            }
            firestore.collection("tasks").document(taskIdFS)
                    .update(updatedTask)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Завдання оновлено", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Помилка оновлення", Toast.LENGTH_SHORT).show());
            Log.d("TaskEditFragment", "taskId: " + taskIdFS);
        }
        else {
            if (taskId == null || taskId.isEmpty()) {
                Toast.makeText(getContext(), "Помилка: немає ID завдання", Toast.LENGTH_SHORT).show();
                return;
            }
            int taskIdInt;
            try {
                taskIdInt = Integer.parseInt(taskId);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некоректний ID завдання", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(getContext(), "ID завдання: " + taskIdInt, Toast.LENGTH_SHORT).show();
            Task task = new Task();
            task.setId(taskIdInt);
            task.setTitle(newTitle);
            task.setDescription(newDescription);
            task.setDueDate(newDueDate);
            task.setCategoryId(categoryList.get(categoryIndex).getId());
            task.setNameCT(categoryNames.get(categoryIndex));
            task.setUserId(auth.getCurrentUser().getUid());
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(getContext());
                db.taskDao().updateTask(task);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Завдання оновлено", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
            }).start();
        }
    }
    private void deleteTask() {
        if(isInternetAvailable()){
            firestore.collection("tasks").document(taskIdFS)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Завдання видалено", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Помилка видалення", Toast.LENGTH_SHORT).show());
        }
        else{
            if (taskId == null || taskId.isEmpty()) {
                Toast.makeText(getContext(), "Помилка: немає ID завдання", Toast.LENGTH_SHORT).show();
                return;
            }
            int taskIdInt;
            try {
                taskIdInt = Integer.parseInt(taskId);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некоректний ID завдання", Toast.LENGTH_SHORT).show();
                return;
            }
            Task task = new Task();
            task.setId(taskIdInt);
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(getContext());
                db.taskDao().deleteTask(task);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Завдання видалено", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
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