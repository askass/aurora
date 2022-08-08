package tech.aurorafin.aurora;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class ScreenLocker extends FrameLayout {

    ObjectAnimator animator;

    AnimatorListenerAdapter endAnimatorListenerAdapter = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            setVisibility(View.GONE);
        }
    };

    public ScreenLocker(@NonNull Context context) {
        super(context);
    }

    public ScreenLocker(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScreenLocker(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScreenLocker(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void lockScreen(){
        if(animator != null){
            animator.removeListener(endAnimatorListenerAdapter);
            animator.cancel();
        }
        setVisibility(View.VISIBLE);
        animator =  ObjectAnimator.ofFloat(this, "Alpha",this.getAlpha(),0.3f);
        animator.setDuration(50);
        animator.start();
    }

    public void unlockScreen(){
        if(getVisibility() == View.VISIBLE){
            if(animator != null){
                animator.cancel();
            }
            animator =  ObjectAnimator.ofFloat(this, "Alpha",this.getAlpha() ,0f);
            animator.setDuration(50);
            animator.addListener(endAnimatorListenerAdapter);
            animator.start();
        }
    }




}
