package tech.aurorafin.aurora;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatCheckBox;


public class SpecCheckBox extends AppCompatCheckBox {

    public boolean multiSelect;

    public SpecCheckBox(Context context) {
        super(context);
    }

    public SpecCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpecCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public boolean performClick() {

        if(!multiSelect && isChecked()){
            return false;
        }else {
            return super.performClick();
        }
    }
}
