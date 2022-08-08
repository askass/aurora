package tech.aurorafin.aurora.dbRoom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface PlanTotalDao {
    @Insert
    void insert(PlanTotal planTotal);

    @Update
    void update(PlanTotal planTotal);

    @Delete
    void delete(PlanTotal planTotal);

    @Query("DELETE FROM plan_total_table WHERE categoryId =:categoryId AND year =:year")
    void deleteAllTotalsOfYearById(long categoryId, int year);


    @Query("SELECT * FROM plan_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND dateCodeFrom =:dateCodeFrom " +
            "AND dateCodeTo =:dateCodeTo " +
            "AND isMonth = 1")
    PlanTotal getPlanTotalMonthById(long categoryId, int dateCodeFrom, int dateCodeTo);


    @Query("SELECT * FROM plan_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND dateCodeFrom =:totalYearFrom " +
            "AND dateCodeTo =:totalYearTo " +
            "AND isYear = 1")
    PlanTotal getPlanTotalYearById(long categoryId, int totalYearFrom, int totalYearTo);



    @Query("SELECT SUM(value) " +
            "FROM plan_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND year = :year " +
            "AND isMonth = 1")
    long sumOfMonthsOfYearById(long categoryId, int year);


    @Query("DELETE FROM PLAN_TOTAL_TABLE WHERE categoryId = :categoryId")
    void deleteAllPlanTotalsByCategoryId(long categoryId);


    @Query("SELECT * FROM PLAN_TOTAL_TABLE WHERE categoryId = :categoryId " +
            "AND dateCodeFrom >= :dateCodeFrom " +
            "AND dateCodeTo <= :dateCodeTo " +
            "AND isMonth = 1")
    PlanTotal[] getPlanTotalMonthsOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    @Query("SELECT * FROM PLAN_TOTAL_TABLE WHERE categoryId = :categoryId " +
            "AND dateCodeFrom >= :dateCodeFrom " +
            "AND dateCodeTo <= :dateCodeTo " +
            "AND isYear = 1")
    PlanTotal[] getPlanTotalYearsOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    @Query("SELECT SUM(value) " +
            "FROM plan_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND dateCodeFrom >= :dateCodeFrom " +
            "AND dateCodeTo <= :dateCodeTo "+
            "AND isMonth = 1" )
    long getSumPlanTotalMonthsOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    /*temp queries*/

    @Query("SELECT * FROM plan_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND isMonth = 1 " +
            "ORDER BY dateCodeFrom ASC")
    PlanTotal[]getAllMonthsById(long categoryId);



}
