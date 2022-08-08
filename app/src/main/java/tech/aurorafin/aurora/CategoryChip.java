package tech.aurorafin.aurora;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;


public class CategoryChip extends FrameLayout {
    Context mContext;
    boolean checked = false;
    public long categoryId;

    public TextView type, name;
    ProgressBar pb;
    int textCheckedColor;
    int textDefaultColor;
    int lockedColor;
    ChipUpdates mChipUpdates;


    public CategoryChip(@NonNull Context context, ChipUpdates chipUpdates, long categoryId, String namet, String typet) {
        super(context);
        this.mContext = context;
        this.mChipUpdates = chipUpdates;
        this.categoryId = categoryId;

        float density = context.getResources().getDisplayMetrics().density;
        int Dp2 = (int)(2f * density +0.5f);
        int Dp5 = (int)(5f * density +0.5f);
        int Dp20 = (int)(20f * density +0.5f);
        int Dp32 = (int)(32f * density +0.5f);
        int Dp100 = (int)(100f * density +0.5f);

        FrameLayout.MarginLayoutParams mlp = new MarginLayoutParams(LayoutParams.WRAP_CONTENT, Dp32);
        
        mlp.topMargin = Dp5;
        mlp.rightMargin = Dp5;
        setLayoutParams(mlp);
        setMinimumWidth(Dp100);
        setClickable(true);
        setFocusable(true);
        setBackground(ContextCompat.getDrawable(mContext, R.drawable.chip_bkg));

        textCheckedColor = ContextCompat.getColor(context, R.color.white_txt_color);
        textDefaultColor = ContextCompat.getColor(context, R.color.blue_row);
        lockedColor = ContextCompat.getColor(mContext, R.color.grey_line);

        type = new TextView(context);
        type.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        type.setGravity(Gravity.CENTER);
        LayoutParams lp1 = new LayoutParams(Dp20, Dp20);
        type.setLayoutParams(lp1);
        type.setTextColor(textDefaultColor);
        type.setText(typet);
        addView(type);

        name = new TextView(context);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setGravity(Gravity.CENTER);
        LayoutParams lp2 = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        lp2.gravity = Gravity.CENTER;
        name.setLayoutParams(lp2);
        name.setPadding(Dp20,0,Dp20,0);
        name.setTextColor(textDefaultColor);
        name.setText(namet);
        addView(name);

        pb = new ProgressBar(context);
        LayoutParams lp3 = new LayoutParams(Dp20, Dp20);
        pb.setLayoutParams(lp3);
        pb.setPadding(Dp2, Dp2, Dp2, Dp2);
        pb.setVisibility(View.GONE);
        addView(pb);
    }


    @Override
    public boolean performClick() {
        setChecked();
        mChipUpdates.chipClicked(categoryId);
        return super.performClick();
    }

    public void setChecked() {
        if(!checked) {
            this.checked = true;
            setBackground(ContextCompat.getDrawable(mContext, R.drawable.chip_bkg_checked));
            type.setTextColor(textCheckedColor);
            name.setTextColor(textCheckedColor);
        }
    }

    public void setDefault() {
        if(checked){
            this.checked = false;
            setBackground(ContextCompat.getDrawable(mContext, R.drawable.chip_bkg));
            type.setTextColor(textDefaultColor);
            name.setTextColor(textDefaultColor);
        }
    }

    public void lockChip(){
        this.setEnabled(false);
        if(checked){
           setDefault();
        }
        name.setTextColor(lockedColor);
        type.setVisibility(View.GONE);
        pb.setVisibility(View.VISIBLE);

    }

    public void unLockChip(){
        this.setEnabled(true);
        if(checked){
            setDefault();
        }
        name.setTextColor(textDefaultColor);
        type.setVisibility(View.VISIBLE);
        pb.setVisibility(View.GONE);

    }

    public interface ChipUpdates{
        void chipClicked(long categoryId);
    }

    public CategoryChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CategoryChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public CategoryChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
