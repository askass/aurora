package tech.aurorafin.aurora;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;

public class InputFilterMinMax implements InputFilter {

    private double min, max;

    public InputFilterMinMax(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public InputFilterMinMax(String min, String max) {
        this.min = Float.parseFloat(min);
        this.max = Float.parseFloat(max);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            String stringInput;
            //Log.d("MyTag", "dest = " + dest.toString() +" source = "+  source.toString());
            //Log.d("MyTag", "start = " + dstart +" end = "+  dend);
            stringInput = dest.toString().substring(0, dstart) + source.toString() + dest.toString().substring(dstart, dest.toString().length());
            //Log.d("MyTag", "stringInput = " + stringInput);
            double value;
            if (stringInput.length() == 1 && stringInput.charAt(0) == '-') {
                value = -1;
            }else if (stringInput.length() == 1 && (stringInput.charAt(0) == '.'||stringInput.charAt(0) == ',')){
                value = 0;
            }else if (stringInput.length() == 2 && stringInput.charAt(0) == '-'&& (stringInput.charAt(1) == '.'||stringInput.charAt(1) == ',')){
                value = 0;
            }else {
                value = Float.parseFloat(stringInput);
            }

            //Log.d("MyTag", "isInRange = "  + isInRange(min, max, value));

            if (isInRange(min, max, value))
                return null;
        } catch (Exception nfe) {
            //Log.d("MyTag", "Exception");
        }
        return "";
    }

    private boolean isInRange(double min, double max, double input) {
        return (input < max) && (input > min);
    }
}