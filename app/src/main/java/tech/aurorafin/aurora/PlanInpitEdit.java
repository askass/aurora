package tech.aurorafin.aurora;

import android.content.Context;
import android.text.TextWatcher;
import android.util.AttributeSet;



public class PlanInpitEdit extends androidx.appcompat.widget.AppCompatEditText implements TextWatcher {

    int planDayToUpdate = -1;

    PlanData.InputManager mInputManager;

    boolean needUpdate = false;

    public PlanInpitEdit(Context context) {
        super(context);

    }

    public PlanInpitEdit(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public PlanInpitEdit(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    public void setPlanDayToUpdate(int planDayToUpdate) {
        this.planDayToUpdate = planDayToUpdate;
        if (planDayToUpdate == -1){
            this.needUpdate = false;
        }
    }

    public void setNeedUpdate(boolean needUpdate) {
        this.needUpdate = needUpdate;
    }

    public void setmInputManager(PlanData.InputManager mInputManager) {
        this.mInputManager = mInputManager;
    }




    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
       // Log.d("MyTag", " beforeTextChanged :: " + charSequence.toString());
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        if(planDayToUpdate != -1 & needUpdate) {
                   mInputManager.setPlanDayString(planDayToUpdate, charSequence.toString());
        }
    }

    @Override
    public void afterTextChanged(android.text.Editable editable) {
       // Log.d("MyTag", " afterTextChanged :: " + editable.toString());
    }
}
