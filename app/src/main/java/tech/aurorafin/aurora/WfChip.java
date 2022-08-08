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

public class WfChip extends FrameLayout {
    Context mContext;
    boolean checked = false;
    public TextView name;
    int textCheckedColor;
    int textDefaultColor;
    ChipUpdates mChipUpdates;
    boolean plan;

    public interface ChipUpdates{
        void chipClicked(boolean plan);
    }


    public WfChip(@NonNull Context context, ChipUpdates chipUpdates, String label, boolean plan) {
        super(context);
        this.mContext = context;
        this.mChipUpdates = chipUpdates;
        this.plan = plan;

        float density = context.getResources().getDisplayMetrics().density;
        int Dp20 = (int)(20f * density +0.5f);
        int Dp5 = (int)(5f * density +0.5f);
        int Dp10 = (int)(10f * density +0.5f);
        int Dp32 = (int)(32f * density +0.5f);
        int Dp110 = (int)(110f * density +0.5f);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Dp110, Dp32);
        //lp.setMarginStart(Dp10);
        lp.setMarginEnd(Dp20);
        lp.gravity = Gravity.CENTER_VERTICAL;
        setLayoutParams(lp);
        setClickable(true);
        setFocusable(true);
        setBackground(ContextCompat.getDrawable(mContext, R.drawable.chip_bkg));

        textCheckedColor = ContextCompat.getColor(context, R.color.white_txt_color);
        textDefaultColor = ContextCompat.getColor(context, R.color.blue_row);


        name = new TextView(context);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
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

    public boolean isChecked(){
        return checked;
    }

    public void setChecked(boolean checked){
        if(!checked){
            setDefault();
        }else {
            setChecked();
        }
    }


    @Override
    public boolean performClick() {
        toggle();
        mChipUpdates.chipClicked(plan);
        return super.performClick();
    }

    private void toggle(){
        if(checked){
            setDefault();
        }else {
            setChecked();
        }
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

    public WfChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public WfChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public WfChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
