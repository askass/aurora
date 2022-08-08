package tech.aurorafin.aurora.dbRoom;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import tech.aurorafin.aurora.PlanData;

import java.util.Calendar;

@Entity(tableName = "operation_table")
public class Operation {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long categoryId;
    public int day;
    public int month;
    public int year;
    public int dayWeek;
    public int yearToWeek;
    public int dateCode;/*YYYYMMDD*/
    public int totalMonthFrom;/*YYYYMMDD*/
    public int totalMonthTo;/*YYYYMMDD*/
    public long value;
    public String description;

    public Operation(long categoryId, int day, int month, int year, int dayWeek,
                     int yearToWeek, int dateCode, int totalMonthFrom, int totalMonthTo, long value, String description) {
        this.categoryId = categoryId;
        this.day = day;
        this.month = month;
        this.year = year;
        this.dayWeek = dayWeek;
        this.yearToWeek = yearToWeek;
        this.dateCode = dateCode;
        this.totalMonthFrom = totalMonthFrom;
        this.totalMonthTo = totalMonthTo;
        this.value = value;
        this.description = description;
    }
    @Ignore
    public Operation(long categoryId, int day, int month, int year, long value, String description){
        this.categoryId = categoryId;
        this.day = day;
        this.month = month;
        this.year = year;
        this.value = value;
        this.description = description;
        this.dateCode = PlanData.getDayCode(year, month, day);

        Calendar init = Calendar.getInstance();
        //init.setFirstDayOfWeek(Calendar.SUNDAY);
        init.set(Calendar.YEAR,year);
        init.set(Calendar.MONTH,month);
        init.set(Calendar.DAY_OF_MONTH,day);

        this.dayWeek = init.get(Calendar.DAY_OF_WEEK);

        Calendar cal = (Calendar)init.clone();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        this.totalMonthFrom = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        this.totalMonthTo = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

    }

    public void setId(long id) {
        this.id = id;
    }



}
