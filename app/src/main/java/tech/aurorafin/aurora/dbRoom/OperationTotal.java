package tech.aurorafin.aurora.dbRoom;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "operation_total_table")
public class OperationTotal {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long categoryId;
    public boolean isYear;
    public boolean isMonth;
    public boolean isDay;
    public int dateCodeFrom;/*YYYYMMDD*/
    public int dateCodeTo;/*YYYYMMDD*/
    public int period;
    public int year;
    public long value;
    public long operations_count;

    public OperationTotal(long categoryId, boolean isYear, boolean isMonth, boolean isDay, int dateCodeFrom, int dateCodeTo, int period, int year, long value, long operations_count) {
        this.categoryId = categoryId;
        this.isYear = isYear;
        this.isMonth = isMonth;
        this.isDay = isDay;
        this.dateCodeFrom = dateCodeFrom;
        this.dateCodeTo = dateCodeTo;
        this.period = period;
        this.year = year;
        this.value = value;
        this.operations_count = operations_count;
    }

    public void setId(long id) {
        this.id = id;
    }
}
