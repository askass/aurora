package tech.aurorafin.aurora.dbRoom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface PlanDao {
    @Insert
    long insert(Plan plan);

    @Update
    void update(Plan plan);

    @Delete
    void delete(Plan plan);

    @Query("SELECT * FROM PLAN_TABLE WHERE categoryId = :categoryId AND year =:year")
    Plan[] getPlanOfCategoryByYear(long categoryId, int year);

    @Query("SELECT * FROM PLAN_TABLE WHERE categoryId = :categoryId AND year =:year ORDER BY dateCode ASC")
    Plan[] getSortedPlanOfCategoryByYear(long categoryId, int year);

    @Query("DELETE FROM PLAN_TABLE WHERE id = :id")
    void deletePlanById(long id);

    @Query("DELETE FROM PLAN_TABLE WHERE categoryId = :categoryId")
    void deleteAllPlanByCategoryId(long categoryId);

    @Query("SELECT SUM(value) " +
            "FROM PLAN_TABLE " +
            "WHERE categoryId = :categoryId " +
            "AND year = :year ")
    long sumOfDaysOfYearById(long categoryId, int year);


    @Query("SELECT * FROM PLAN_TABLE WHERE categoryId = :categoryId " +
            "AND dateCode >= :dateCodeFrom " +
            "AND dateCode <= :dateCodeTo")
    Plan[] getPlanOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    @Query("SELECT SUM(value) " +
            "FROM PLAN_TABLE " +
            "WHERE categoryId = :categoryId " +
            "AND dateCode >= :dateCodeFrom " +
            "AND dateCode <= :dateCodeTo")
    long getSumPlanOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);
}
