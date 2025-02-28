package com.example.organizer.fragments;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.organizer.R;
import com.example.organizer.database.AppDatabase;
import com.example.organizer.models.Category;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import yuku.ambilwarna.*;
public class CategoryCreateFragment extends Fragment {
    private EditText nameEditText;
    private Button colorButton;
    private String colorEditText;
    public CategoryCreateFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_category_create, container, false);
        colorButton = rootView.findViewById(R.id.colorPickButton);
        nameEditText = rootView.findViewById(R.id.nameEditText);
        colorEditText = "#g";
        rootView.findViewById(R.id.saveCategoryButton).setOnClickListener(v -> saveCategory());

        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openColorPicker();
            }
        });
        return rootView;
    }
    public void openColorPicker(){
        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(getActivity(), 0, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                colorEditText = String.format("#%06X", (0xFFFFFF & color));
                colorButton.setBackgroundColor(color);
            }
        });
        ambilWarnaDialog.show();
    }
    private void saveCategory() {
        String name = nameEditText.getText().toString().trim();
        String color = colorEditText;
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(color)) {
            Toast.makeText(getContext(), "Заповніть всі поля", Toast.LENGTH_SHORT).show();
            return;
        }
        Category category = new Category(name, color, FirebaseAuth.getInstance().getCurrentUser().getUid());
        if (isInternetAvailable()) {
            saveCategoryToFirebase(category);
        } else {
            saveCategoryToRoom(category);
        }
    }
    private void saveCategoryToFirebase(Category category) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query query = db.collection("categories")
                .whereEqualTo("userId", category.getUserId())
                .whereEqualTo("name", category.getName());
        query.get().addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                Toast.makeText(getContext(), "Категорія з таким ім'ям вже існує", Toast.LENGTH_SHORT).show();
            } else {
                db.collection("categories")
                        .add(category)
                        .addOnSuccessListener(documentReference -> {
                            category.setFirebaseId(documentReference.getId());
                            db.collection("categories").document(category.getFirebaseId()).set(category);
                            getActivity().onBackPressed();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Помилка збереження категорії", Toast.LENGTH_SHORT).show();
                        });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Помилка перевірки категорії", Toast.LENGTH_SHORT).show();
        });
    }
    private void saveCategoryToRoom(Category category) {
        AppDatabase database = AppDatabase.getInstance(getContext());
        new Thread(() -> {
            database.categoryDao().insert(category);
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Категорію збережено локально", Toast.LENGTH_SHORT).show();
                getActivity().onBackPressed();
            });
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
