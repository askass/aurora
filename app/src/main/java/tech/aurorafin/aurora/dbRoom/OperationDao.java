package tech.aurorafin.aurora.dbRoom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface OperationDao {
    @Insert
    long insert(Operation operation);

    @Update
    void update(Operation operation);

    @Delete
    void delete(Operation operation);

    @Query("DELETE FROM OPERATION_TABLE WHERE id = :id")
    void deleteOperationById(long id);


    @Query("SELECT * FROM OPERATION_TABLE WHERE categoryId = :categoryId AND year =:year ORDER BY dateCode ASC")
    Operation[] getSortedOperationsOfCategoryByYear(long categoryId, int year);

    @Query("SELECT * FROM OPERATION_TABLE WHERE id = :id")
    Operation getOperationById(long id);

    @Query("SELECT * FROM OPERATION_TABLE ORDER BY id DESC LIMIT 6 ")
    List<Operation> getLastOperations();

    @Query("UPDATE OPERATION_TABLE SET value =:value WHERE Id =:id")
    void setOperationValueById(long id, long value);

    @Query("SELECT * FROM OPERATION_TABLE WHERE categoryId = :categoryId AND dateCode >=:dateCodeFrom AND dateCode <=:dateCodeTo ORDER BY dateCode ASC")
    List<Operation> getSortedOperationsOfCategoryByRange(long categoryId, int dateCodeFrom, int dateCodeTo);

    @Query("DELETE FROM OPERATION_TABLE WHERE categoryId = :categoryId")
    void deleteAllOperationsByCategoryId(long categoryId);

    @Query("SELECT * FROM OPERATION_TABLE WHERE categoryId = :categoryId AND dateCode >=:dateCodeFrom AND dateCode <=:dateCodeTo")
    Operation[] getOperationsOfCategoryByRange(long categoryId, int dateCodeFrom, int dateCodeTo);


}
