package tech.aurorafin.aurora.dbRoom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AggregatorDao {
    @Insert
    void insert(Aggregator aggregator);

    @Update
    void update(Aggregator aggregator);

    @Delete
    void delete(Aggregator aggregator);

    @Query("SELECT * FROM AGGREGATOR_TABLE ORDER BY name ASC")
    List<Aggregator> getAllAggregators();

    @Query("DELETE FROM AGGREGATOR_TABLE WHERE id = :id")
    void deleteAggregatorById(long id);

}
