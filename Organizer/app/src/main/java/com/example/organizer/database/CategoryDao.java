package com.example.organizer.database;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import com.example.organizer.models.Category;
import java.util.List;
@Dao
public interface CategoryDao {
    @Insert
    void insert(Category category);
    @Update
    void update(Category category);
    @Delete
    void delete(Category category);
    @Query("SELECT * FROM category_tb")
    List<Category> getAllCategories();
    @Query("SELECT * FROM category_tb WHERE userId = :userId")
    List<Category> getCategoriesByUser(String userId);
    @Delete
    void deleteCategory(Category category);
}
