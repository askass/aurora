package tech.aurorafin.aurora;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import android.widget.LinearLayout;

public class SpecLinearLayout extends LinearLayout {

    boolean needUpdate = false;
    PlanData.InputManager mInputManager;

    public SpecLinearLayout(Context context) {
        super(context);
    }

    public SpecLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpecLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SpecLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    public void setNeedUpdate(boolean needUpdate) {
        this.needUpdate = needUpdate;
    }

    public void setmInputManager(PlanData.InputManager mInputLayoutManager) {
        this.mInputManager = mInputLayoutManager;
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(changed & needUpdate) {
              mInputManager.updateSubRvHeight(getHeight());
        }
        super.onLayout(changed, l, t, r, b);
    }
}
