package tech.aurorafin.aurora;


import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class LongFormater {

    public static long One = 1L;
    public static long Thousand = 1000L;
    public static long Million  = 1000000L;
    public static long Billion  = 1000000000L;
    public static long Trillion = 1000000000000L;

    public static int noDecimal = 0;
    public static int oneDecimal = 1;
    public static int twoDecimal = 2;



    public static String formatLong(long number, long DIVIDER, int DECIMAL){
        if(number != 0){
            StringBuilder newString = new StringBuilder();
            double dub = number;
            dub = dub/(DIVIDER*100);
            BigDecimal bd = BigDecimal.valueOf(dub);
            bd = bd.setScale(DECIMAL, RoundingMode.HALF_UP);
            String str =  bd.toString();
            int p = 0;
            int addition = 0;
            if(DECIMAL != noDecimal){
                addition = DECIMAL +1;
            }
            for (int i = str.length()-1; i >= 0; i--) {
                char ch = str.charAt(i);
                if (ch != '-' && p > addition && (p-addition) % 3 == 0){
                    newString.append(" ");
                }
                newString.append(ch);
                p++;
            }
            return newString.reverse().toString();
        }else {
            return "-";
        }
    }

    public static long getDivider(long max, long min){
        long mMin = min < 0 ? min*-1: min;
        long absMax = Math.max(max, mMin);

        if( absMax < 10000000L){
            return One;
        }else if(absMax < 10000000000L){
            return Thousand;
        }else if(absMax < 10000000000000L){
            return Million;
        }else if(absMax < 10000000000000000L){
            return Billion;
        } else {
            return Trillion;
        }
    }

    public static int getDecimals(long divider, long max, long min){
        long mMin = min < 0 ? min*-1: min;
        long absMax = Math.max(max, mMin);
        if(absMax/100 < 100*divider){
            return twoDecimal;
        }else if(absMax/100 < 1000*divider){
            return oneDecimal;
        }else {
            return noDecimal;
        }
    }

    public static String getPercentDelta(long delta, long plan, long fact, boolean withPlus){
        if(plan!=0){
            if((delta == 0 || fact == 0)&&!withPlus){
                return "-";
            }else {
                double temp = (double)delta / (double)Math.abs(plan);
                temp = temp*100;
                if(temp > 999){
                    return ">999%";
                }else if(temp < -999) {
                    return "<999%";
                }else {
                    BigDecimal bd = BigDecimal.valueOf(temp);
                    bd = bd.setScale(0, RoundingMode.HALF_UP);
                    if(withPlus && delta > 0){
                        return "+" + bd.toString()+"%";
                    }else {
                        return bd.toString()+"%";
                    }

                }
            }
        }else {
            return "n/a";
        }
    }

    public static float getChartFloat(long divider, long value){
        float tempVal = (float) value;
        return tempVal/(divider*100);
    }

    public static double getCsvString(long value){
        //Ghjcnjq ghtj,hfpjdfntkm wtkjuj xbckf c cnhjrjdjt


        double tempVal = (double) value;
        return tempVal/(100);
    }

    public static class MyValueFormatter extends ValueFormatter {

        private DecimalFormat mFormat;

        public MyValueFormatter () {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            symbols.setGroupingSeparator(' ');
            mFormat = new DecimalFormat("###,###,##0", symbols); // use one decimal
        }

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            return  mFormat.format(value);
        }

    }
}
