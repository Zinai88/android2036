package com.example.organizer.models;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.Exclude;
@Entity(tableName = "category_tb")
public class Category {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    @Exclude
    private String firebaseId;
    private String name;
    private String color;
    private String userId;
    public Category() {}
    public Category(String name, String color, String userId) {
        this.name = name;
        this.color = color;
        this.userId = userId;
        this.firebaseId = "";
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getFirebaseId() {
        return firebaseId;
    }
    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getColor() {
        return color;
    }
    public void setColor(String color) {
        this.color = color;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
}