package tech.aurorafin.aurora.dbRoom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    void insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM CATEGORY_TABLE WHERE aggregatorId = :aggId ORDER BY type ASC")
    Category[] getAllCategoriesOfAggregator(long aggId);

    @Query("DELETE FROM CATEGORY_TABLE WHERE id = :id")
    void deleteCategoryById(long id);

    @Query("UPDATE CATEGORY_TABLE SET last_update =:last_update WHERE Id =:id")
    void selLastCategoryUpdateTime(long id, long last_update);


    @Query("SELECT * FROM CATEGORY_TABLE ORDER BY last_update DESC LIMIT 9 ")
    List<Category> getLastUsedCategories();

    @Query("SELECT * FROM CATEGORY_TABLE ORDER BY type")
    List<Category> getSortedCategories();
}
