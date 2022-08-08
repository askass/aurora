package tech.aurorafin.aurora;

import android.content.Context;
import android.content.SharedPreferences;

public class DateFormater {

    public final static int YYYYMMDD = 0;
    public final static int YYYYDDMM = 1;
    public final static int MMDDYYYY = 2;
    public final static int DDMMYYYY = 3;
    public final static int YYMMDD = 4;
    public final static int YYDDMM = 5;
    public final static int MMDDYY = 6;
    public final static int DDMMYY = 7;

    private static final String PREF_FILE_KEY = "dateFormatPrefs";
    public static final String DATE_FORMAT_KEY = "DATE_FORMAT_KEY";

    public static void updateDateFormatKey(int dateFormat, Context context) {
        SharedPreferences settings =
                context.getSharedPreferences(PREF_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(DATE_FORMAT_KEY, dateFormat);
        editor.apply();
    }

    public static int getDateFormatKey(Context context){
        SharedPreferences sharedPref =context.getSharedPreferences(
                PREF_FILE_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(DATE_FORMAT_KEY, 0);
    }




    public static String getDateFromDateCode(int intDayCode, int formatCode){
        int year = intDayCode / 10000;
        String month = getDatePart((intDayCode % 10000) / 100);
        String day = getDatePart(intDayCode % 100);
        switch (formatCode){
            case YYYYMMDD:
                return year + "/" + month + "/" + day;
            case YYYYDDMM:
                return year + "/" + day + "/" + month;
            case MMDDYYYY:
                return month + "/" + day + "/" + year;
            case DDMMYYYY:
                return day + "/" + month + "/" + year;
            case YYMMDD:
                return year % 100 + "/" + month + "/" + day;
            case YYDDMM:
                return year % 100 + "/" + day + "/" + month;
            case MMDDYY:
                return month + "/" + day + "/" + year % 100;
            case DDMMYY:
                return day + "/" + month + "/" + year% 100;
            default:
                return year + "/" + month + "/" + day;
        }
    }

    private static String getDatePart(int dp){
        if(dp<10){
            return "0" + dp;
        }else{
            return String.valueOf(dp);
        }
    }
}


