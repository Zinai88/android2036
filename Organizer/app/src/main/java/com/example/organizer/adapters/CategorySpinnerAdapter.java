package com.example.organizer.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.organizer.models.Category;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class CategorySpinnerAdapter extends ArrayAdapter<Category> {
    private List<Category> categories;

    public CategorySpinnerAdapter(@NonNull Context context, List<Category> categories) {
        super(context, android.R.layout.simple_spinner_item, categories);
        this.categories = categories;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Nullable
    @Override
    public Category getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView textView = (TextView) super.getView(position, convertView, parent);
        setCategoryStyle(textView, position);
        return textView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
        setCategoryStyle(textView, position);
        return textView;
    }

    private void setCategoryStyle(TextView textView, int position) {
        Category category = categories.get(position);
        textView.setText(category.getName());

        // Получаем цвет категории
        String color = category.getColor();
        if (color != null && !color.isEmpty()) {
            try {
                textView.setBackgroundColor(Color.parseColor(color));
            } catch (IllegalArgumentException e) {
                textView.setBackgroundColor(Color.WHITE);
            }
        } else {
            textView.setBackgroundColor(Color.WHITE);
        }

        // Улучшаем контрастность текста
        textView.setTextColor(Color.BLACK);
        textView.setPadding(16, 8, 16, 8);
        textView.setTextSize(18);
    }
}