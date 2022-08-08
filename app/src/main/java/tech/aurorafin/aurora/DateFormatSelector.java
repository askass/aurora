package tech.aurorafin.aurora;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class DateFormatSelector extends LinearLayout implements View.OnClickListener {


    Context mContext;
    String[] dateFormats;
    TextView dateExample;
    public int Dp300;
    int todayDateCode;
    RadioButton radioButtons[];

    private int selectedFormatCode;
    public int formatCode;

    public DateFormatSelector(Context context) {
        super(context);
        mContext = context;

        float density = context.getResources().getDisplayMetrics().density;
        int Dp10 = (int)(10f * density + 0.5f);
        int Dp20 = (int)(20f * density + 0.5f);
        int Dp35 = (int)(35f * density + 0.5f);
        int Dp40 = (int)(40f * density + 0.5f);
        int Dp270 = (int)(270f * density + 0.5f);
        Dp300 = (int)(300f * density + 0.5f);

        setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setOrientation(VERTICAL);


        ScrollView sw = new ScrollView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Dp270);
        lp.setMargins(0,Dp10,0,0);
        sw.setLayoutParams(lp);
        sw.setVerticalFadingEdgeEnabled(false);
        sw.setVerticalScrollBarEnabled(true);
        addView(sw);

        RadioGroup rg = new RadioGroup(context);
        rg.setOrientation(VERTICAL);
        rg.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        rg.setPadding(Dp20,0,Dp20,Dp10);
        sw.addView(rg);

        dateFormats = context.getResources().getStringArray(R.array.date_formats);
        int greyTxtColor = ContextCompat.getColor(context,  R.color.grey_txt_color);
        radioButtons = new RadioButton[dateFormats.length];
        for(int i = 0; i <dateFormats.length; i++){
            RadioButton rb = getFormatRadioBtn(context, dateFormats[i], greyTxtColor, Dp40);
            rb.setOnClickListener(this);
            radioButtons[i] = rb;
            rg.addView(rb);
        }

        dateExample = new TextView(context);
        lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Dp35);
        lp.setMargins(Dp20,0,Dp20,0);
        dateExample.setLayoutParams(lp);
        dateExample.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_transparent_border_top_light));
        dateExample.setGravity(Gravity.BOTTOM);
        int blueRow = ContextCompat.getColor(context,  R.color.blue_row);
        dateExample.setTextColor(blueRow);
        dateExample.setPadding(Dp10, 0 ,0 , 0);
        dateExample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        dateExample.setText("");
        addView(dateExample);

        Calendar init = Calendar.getInstance();
        int day = init.get(Calendar.DAY_OF_MONTH);
        int month = init.get(Calendar.MONTH);
        int year = init.get(Calendar.YEAR);

        todayDateCode = PlanData.getDayCode(year,month, day);
        updateRbCheck();
    }

    public void resetRbCheck(){
        updateDateExample(selectedFormatCode);
        setCurrentFormatRbChecked(selectedFormatCode);
    }

    public void updateRbCheck(){
        formatCode = DateFormater.getDateFormatKey(mContext);
        selectedFormatCode = formatCode;
        updateDateExample(selectedFormatCode);
        setCurrentFormatRbChecked(selectedFormatCode);
    }

    private void setCurrentFormatRbChecked(int formatCode) {
        for(int i = 0; i < radioButtons.length; i++){
            if(i == formatCode){
                radioButtons[i].setChecked(true);
            }
        }
    }


    private RadioButton getFormatRadioBtn(Context context, String text, int color, int height){
        RadioButton rb = new RadioButton(context);
        rb.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
        rb.setGravity(Gravity.CENTER_VERTICAL);
        rb.setSaveEnabled(false);
        rb.setTextColor(color);
        rb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        rb.setText(text);
        return rb;
    }

    @Override
    public void onClick(View view) {
        for(int i = 0; i < radioButtons.length; i++){
            if(radioButtons[i].isChecked()){
                updateDateExample(i);
                break;
            }
        }
    }

    private void updateDateExample(int dateFormatKey) {
        formatCode = dateFormatKey;
        dateExample.setText(DateFormater.getDateFromDateCode(todayDateCode, formatCode));
    }
}
