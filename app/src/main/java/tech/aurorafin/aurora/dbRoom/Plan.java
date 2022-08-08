package tech.aurorafin.aurora.dbRoom;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "plan_table")
public class Plan {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long categoryId;
    public int day;
    public int month;
    public int year;
    public int dateCode;/*YYYYMMDD*/
    public int totalMonthFrom;/*YYYYMMDD*/
    public int totalMonthTo;/*YYYYMMDD*/
    public long value;

    public Plan(long categoryId, int day, int month, int year, int dateCode,
                int totalMonthFrom, int totalMonthTo, long value) {
        this.categoryId = categoryId;
        this.day = day;
        this.month = month;
        this.year = year;
        this.dateCode = dateCode;
        this.totalMonthFrom = totalMonthFrom;
        this.totalMonthTo = totalMonthTo;
        this.value = value;
    }

    public void setId(long id) {
        this.id = id;
    }
}
