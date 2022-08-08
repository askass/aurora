package tech.aurorafin.aurora.dbRoom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface OperationTotalDao {
    @Insert
    void insert(OperationTotal operationTotal);

    @Update
    void update(OperationTotal operationTotal);

    @Delete
    void delete(OperationTotal operationTotal);

    @Query("DELETE FROM operation_total_table WHERE categoryId =:categoryId AND year =:year")
    void deleteAllTotalsOfYearById(long categoryId, int year);


    @Query("SELECT * FROM operation_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND dateCodeFrom =:totalDayFrom " +
            "AND dateCodeTo =:totalDayTo " +
            "AND isDay = 1")
    OperationTotal getOperationTotalDayById(long categoryId, int totalDayFrom, int totalDayTo);

    @Query("SELECT * FROM operation_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND dateCodeFrom =:totalMonthFrom " +
            "AND dateCodeTo =:totalMonthTo " +
            "AND isMonth = 1")
    OperationTotal getOperationTotalMonthById(long categoryId, int totalMonthFrom, int totalMonthTo);



    @Query("SELECT SUM(value) " +
            "FROM operation_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND year = :year " +
            "AND isDay = 1")
    long sumOfDaysOfYearById(long categoryId, int year);

    @Query("SELECT SUM(value) " +
            "FROM operation_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND year = :year " +
            "AND isMonth = 1")
    long sumOfMonthsOfYearById(long categoryId, int year);


    @Query("SELECT * FROM operation_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND year =:year " +
            "AND isMonth = 1 " +
            "ORDER BY dateCodeFrom ASC")
    OperationTotal[] getOperationMonthTotalsOfCategoryByYear(long categoryId, int year);

    @Query("SELECT * FROM operation_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND year =:year " +
            "AND isDay = 1 " +
            "ORDER BY dateCodeFrom ASC")
    OperationTotal[] getOperationDayTotalsOfCategoryByYear(long categoryId, int year);

    @Query("SELECT * FROM operation_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND dateCodeFrom =:yearFrom " +
            "AND dateCodeTo =:yearTo " +
            "AND isYear = 1")
    OperationTotal getOperationTotalYearById(long categoryId, int yearFrom, int yearTo);

    @Query("DELETE FROM operation_total_table WHERE categoryId = :categoryId")
    void deleteAllOperationTotalsByCategoryId(long categoryId);



    @Query("SELECT * FROM operation_total_table WHERE categoryId = :categoryId " +
            "AND dateCodeFrom >= :dateCodeFrom " +
            "AND dateCodeTo <= :dateCodeTo " +
            "AND isDay = 1")
    OperationTotal[] getOperationDayTotalsOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    @Query("SELECT * FROM operation_total_table WHERE categoryId = :categoryId " +
            "AND dateCodeFrom >= :dateCodeFrom " +
            "AND dateCodeTo <= :dateCodeTo " +
            "AND isMonth = 1")
    OperationTotal[] getOperationMonthTotalsOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    @Query("SELECT * FROM operation_total_table WHERE categoryId = :categoryId " +
            "AND dateCodeFrom >= :dateCodeFrom " +
            "AND dateCodeTo <= :dateCodeTo " +
            "AND isYear = 1")
    OperationTotal[] getOperationYearTotalsOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    @Query("SELECT SUM(value) " +
            "FROM operation_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND dateCodeFrom >= :dateCodeFrom " +
            "AND dateCodeTo <= :dateCodeTo "+
            "AND isDay = 1" )
    long getSumOperationTotalDaysOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    @Query("SELECT SUM(value) " +
            "FROM operation_total_table " +
            "WHERE categoryId = :categoryId " +
            "AND dateCodeFrom >= :dateCodeFrom " +
            "AND dateCodeTo <= :dateCodeTo "+
            "AND isMonth = 1" )
    long getSumOperationTotalMonthsOfCategoryByDateCodeRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    @Query("SELECT MAX(year) " +
            "FROM operation_total_table " +
            "WHERE isYear = 1" )
    int getMaxYear();

    @Query("SELECT Min(year) " +
            "FROM operation_total_table " +
            "WHERE isYear = 1" )
    int getMinYear();

    @Query("SELECT SUM(value) " +
            "FROM operation_total_table " +
            "WHERE categoryId = :categoryId AND isYear = 1 AND year <= :year")
    long getCategorySumForBalance(long categoryId, int year);
}
