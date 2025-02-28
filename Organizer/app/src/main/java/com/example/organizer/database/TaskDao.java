package com.example.organizer.database;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.organizer.models.Task;
import java.util.List;
@Dao
public interface TaskDao {
    @Insert
    void insert(Task task);
    @Update
    void updateTask(Task task);
    @Delete
    void deleteTask(Task task);
    @Query("SELECT * FROM task_tb")
    List<Task> getAllTasks();
    @Query("DELETE FROM task_tb WHERE nameCT = :categoryName")
    void deleteTasksByCategoryId(String categoryName);
    @Query("SELECT * FROM task_tb WHERE nameCT = :categoryName")
    List<Task> getTasksByCategory(String categoryName);
}