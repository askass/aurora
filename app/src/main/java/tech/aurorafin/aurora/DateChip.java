package tech.aurorafin.aurora;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;


public class DateChip extends FrameLayout {
    Context mContext;
    boolean checked = false;
    int dateCodeFrom;
    int dateCodeTo;
    public int index;

    public TextView name;
    int textCheckedColor;
    int textDefaultColor;
    ChipUpdates mChipUpdates;

    public interface ChipUpdates{
        void chipClicked(int dateCodeFrom, int dateCodeTo, int index);
    }


    public DateChip(@NonNull Context context, ChipUpdates chipUpdates, String label, int dateCodeFrom, int dateCodeTo, int index) {
        super(context);
        this.mContext = context;
        this.mChipUpdates = chipUpdates;
        this.dateCodeFrom = dateCodeFrom;
        this.dateCodeTo = dateCodeTo;
        this.index = index;

        float density = context.getResources().getDisplayMetrics().density;
        int Dp2d5 = (int)(2.5f * density +0.5f);
        int Dp5 = (int)(5f * density +0.5f);
        int Dp20 = (int)(20f * density +0.5f);
        int Dp32 = (int)(32f * density +0.5f);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Dp32, 1f);
        lp.setMarginStart(Dp2d5);
        lp.setMarginEnd(Dp2d5);
        setLayoutParams(lp);
        setClickable(true);
        setFocusable(true);
        setBackground(ContextCompat.getDrawable(mContext, R.drawable.chip_bkg));

        textCheckedColor = ContextCompat.getColor(context, R.color.white_txt_color);
        textDefaultColor = ContextCompat.getColor(context, R.color.blue_row);


        name = new TextView(context);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setGravity(Gravity.CENTER);
        LayoutParams lp2 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        lp2.gravity = Gravity.CENTER;
        name.setLayoutParams(lp2);
        name.setPadding(Dp5,0,Dp5,0);
        name.setTextColor(textDefaultColor);
        name.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        name.setMaxLines(1);
        name.setText(label);
        addView(name);

    }


    @Override
    public boolean performClick() {
        setChecked();
        mChipUpdates.chipClicked(dateCodeFrom, dateCodeTo, index);
        return super.performClick();
    }

    public void setChecked() {
        if(!checked) {
            this.checked = true;
            setBackground(ContextCompat.getDrawable(mContext, R.drawable.chip_bkg_checked));
            name.setTextColor(textCheckedColor);
        }
    }

    public void setDefault() {
        if(checked){
            this.checked = false;
            setBackground(ContextCompat.getDrawable(mContext, R.drawable.chip_bkg));
            name.setTextColor(textDefaultColor);
        }
    }

    public boolean isChecked(){
        return checked;
    }

    public DateChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DateChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DateChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
