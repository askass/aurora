package tech.aurorafin.aurora.dbRoom;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "plan_total_table")
public class PlanTotal {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long categoryId;
    public boolean isYear;
    public boolean isMonth;
    public int dateCodeFrom;/*YYYYMMDD*/
    public int dateCodeTo;/*YYYYMMDD*/
    public int period;
    public int year;
    public long value;

    public PlanTotal(long categoryId, boolean isYear, boolean isMonth, int dateCodeFrom, int dateCodeTo, int period, int year, long value) {
        this.categoryId = categoryId;
        this.isYear = isYear;
        this.isMonth = isMonth;
        this.dateCodeFrom = dateCodeFrom;
        this.dateCodeTo = dateCodeTo;
        this.period = period;
        this.year = year;
        this.value = value;
    }

    public void setId(long id) {
        this.id = id;
    }
}
