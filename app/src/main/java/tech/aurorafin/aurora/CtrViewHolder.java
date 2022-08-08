package tech.aurorafin.aurora;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class CtrViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
    Context mContext;
    CtrViewHolderInterface mCtrViewHolderInterface;

    /*colors*/
    int greyTriangle;
    int activatedRow;
    int lightTriangle;
    int lightText;
    int darkText;

    public int oneDp;
    public boolean squeeze;

    /*Animation consts*/
    float planTableTopMargin_from;
    float planTableTopMargin_to;
    public float planTableHeight_from;
    float planTableHeight_to;
    float planTableHeightToAdd_from;
    float planTableHeightToAdd_to;
    public float crtContainerHeight_from;
    float crtContainerHeight_to;
    float ctrTriangleRotation_from;
    float ctrTriangleRotation_to;
    float srwAlpha_from;
    float srwAlpha_to;
    int animTime;

    AccelerateDecelerateInterpolator accelerateDecelerateInterpolator;

    /*Parents*/
    MyLinearLayoutManager llm;
    RecyclerView mRecyclerView;
    LinearLayout recyclerViewContainer;
    RecyclerView tableSubRV;

    public TextView label;
    public TextView number;
    public LinearLayout chameleonTextRow;
    public ImageView ctr_triangle;
    public LinearLayout rowCollapseContainer;


    public interface CtrViewHolderInterface{
        boolean isTotalSelected(int position);
        int getCollapsedHolderPos();
        void setCollapsedHolderPos(int position, boolean backBtnAction);
        void setCollapsedHolder(CtrViewHolder ctrViewHolder);
        boolean isLocalTotalSelection();
        void setRowSelected(int position, boolean total);
        void setRowUnselected(int position, boolean total);
        void setOnAnimation(boolean onAnimation);
        void notifyAdapterItemChanged(int position);
    }

    public CtrViewHolder(@NonNull View itemView, Context context, CtrViewHolderInterface ctrViewHolderInterface,
                         int greyTriangle, int activatedRow, int lightTriangle, int lightText, int darkText,
                         AccelerateDecelerateInterpolator accelerateDecelerateInterpolator,
                         MyLinearLayoutManager llm, RecyclerView recyclerView, LinearLayout recyclerViewContainer, RecyclerView tableSubRV) {
        super(itemView);

        mContext = context;
        mCtrViewHolderInterface = ctrViewHolderInterface;
        float density = mContext.getResources().getDisplayMetrics().density;
        oneDp = (int)(density);

        /*colors*/
        this.greyTriangle = greyTriangle;
        this.activatedRow = activatedRow;
        this.lightTriangle = lightTriangle;
        this.lightText = lightText;
        this.darkText = darkText;

        this.accelerateDecelerateInterpolator = accelerateDecelerateInterpolator;

        /*Parents*/

        this.llm = llm;
        this.mRecyclerView = recyclerView;
        this.recyclerViewContainer = recyclerViewContainer;
        this.tableSubRV = tableSubRV;


        chameleonTextRow = itemView.findViewById(R.id.chameleonTextRow);
        ctr_triangle = itemView.findViewById(R.id.ctr_triangle);
        rowCollapseContainer = itemView.findViewById(R.id.rowCollapseContainer);
        label = itemView.findViewById(R.id.ctr_text_lbl);
        number = itemView.findViewById(R.id.ctr_value_lbl);
        chameleonTextRow.setOnClickListener(this);
        chameleonTextRow.setOnLongClickListener(this);
    }

    public void setLayout(){
        int p = getAdapterPosition();

        if(mCtrViewHolderInterface.isTotalSelected(p)){
            setSelectedLayout();
        }else if( p == mCtrViewHolderInterface.getCollapsedHolderPos()){
            ctr_triangle.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.plan_icon_triangle_bm_dark));
            setCollapsedLayout();
        } else {
            ctr_triangle.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.plan_icon_triangle_bm_dark));
            if(mCtrViewHolderInterface.isLocalTotalSelection()){
                ctr_triangle.setColorFilter(greyTriangle);
            }else {
                ctr_triangle.setColorFilter(null);
            }
            if ( p % 2 == 0){
                chameleonTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple_gr));
            }else {
                chameleonTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple_wt));
            }
        }
    }

    public void setSelectedLayout(){
        ctr_triangle.setColorFilter(null);
        ctr_triangle.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.plan_icon_ok_circle));
        chameleonTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_selected_row_gr));
    }

    @Override
    public boolean onLongClick(View view) {
        int p = getAdapterPosition();
        if (mCtrViewHolderInterface.getCollapsedHolderPos() == -1){
            if(mCtrViewHolderInterface.isTotalSelected(p)){
                return false;
            }else{
                setSelectedLayout();
                mCtrViewHolderInterface.setRowSelected(p, true);
                return true;
            }
        }else{
            return false;
        }
    }

    @Override
    public void onClick(View view) {
        if(mCtrViewHolderInterface.isLocalTotalSelection()){
            int p = getAdapterPosition();
            if(mCtrViewHolderInterface.isTotalSelected(p)){
                mCtrViewHolderInterface.setRowUnselected(p, true);
                mCtrViewHolderInterface.notifyAdapterItemChanged(p);
            }else{
                setSelectedLayout();
                mCtrViewHolderInterface.setRowSelected(p, true);
            }
        }else{
            clickToCollapseSqueeze();
        }
    }



    public void activateCtr(){
        llm.setScrollEnabled(false);
        setCollapsedLayout();
        mCtrViewHolderInterface.setCollapsedHolder(this);
        mCtrViewHolderInterface.setCollapsedHolderPos(this.getAdapterPosition(), true);
    }

    public void setCollapsedLayout(){
        chameleonTextRow.setBackgroundColor(activatedRow);
        rowCollapseContainer.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_border_bottom));
        ctr_triangle.setColorFilter(lightTriangle);
        label.setTextColor(lightText);
        number.setTextColor(lightText);
    }

    public void deactivateCtr(){
        chameleonTextRow.setEnabled(false);
        ctr_triangle.setColorFilter(null);
        label.setTextColor(darkText);
        number.setTextColor(darkText);
        mCtrViewHolderInterface.setCollapsedHolder(null);
        mCtrViewHolderInterface.setCollapsedHolderPos(-1, false);
        setLayout();
    }

    public void setAnimationConstsToExpand(){
        squeeze = false;
        planTableTopMargin_from = 0;
        planTableTopMargin_to = - rowCollapseContainer.getTop();
        planTableHeight_from = mRecyclerView.getHeight();
        planTableHeight_to = planTableHeight_from - planTableTopMargin_to;
        crtContainerHeight_from = rowCollapseContainer.getHeight();
        crtContainerHeight_to = planTableHeight_from + oneDp;// + finalAdjustment;
        planTableHeightToAdd_from = planTableHeight_from;
        planTableHeightToAdd_to = mRecyclerView.getBottom() + mRecyclerView.getBottom() - rowCollapseContainer.getBottom()+ oneDp;// + finalAdjustment;
        ctrTriangleRotation_from = 0;
        ctrTriangleRotation_to = 90;
        float spcKoef = -planTableTopMargin_to/planTableHeight_from;
        srwAlpha_from = 0.1f-0.4f*spcKoef;
        srwAlpha_to = 1;
        animTime = (int)(415 +75*spcKoef);//-50
    }

    public void setAnimationConstsToSqueeze(){
        squeeze = true;
        planTableTopMargin_from = mRecyclerView.getTop();//((ViewGroup.MarginLayoutParams)mRecyclerView.getLayoutParams()).topMargin;
        planTableTopMargin_to = 0;
        planTableHeight_from = mRecyclerView.getHeight();
        planTableHeight_to = recyclerViewContainer.getHeight();
        crtContainerHeight_to = chameleonTextRow.getHeight();
        crtContainerHeight_from = rowCollapseContainer.getHeight();
        planTableHeightToAdd_from = mRecyclerView.getBottom();
        planTableHeightToAdd_to = recyclerViewContainer.getHeight();
        ctrTriangleRotation_from = 90;
        ctrTriangleRotation_to = 0;
        float spcKoef = -planTableTopMargin_from/crtContainerHeight_from;
        srwAlpha_from = 1;
        srwAlpha_to = 0.1f-0.3f*(spcKoef);
        animTime = (int)(395 + 75*spcKoef);//-50
    }

    public void setFractionTableAnimation(float share){
        int top = Math.round(planTableTopMargin_from+(planTableTopMargin_to - planTableTopMargin_from)*share);
        int bot = Math.round(planTableHeightToAdd_from + (planTableHeightToAdd_to - planTableHeightToAdd_from)*share);
        rowCollapseContainer.getLayoutParams().height = Math.round(crtContainerHeight_from + (crtContainerHeight_to - crtContainerHeight_from)*share);
        mRecyclerView.layout(mRecyclerView.getLeft(), top, mRecyclerView.getRight(), bot);
        tableSubRV.setAlpha(srwAlpha_from + (srwAlpha_to-srwAlpha_from)*share);
        ctr_triangle.setRotation(ctrTriangleRotation_from + (ctrTriangleRotation_to - ctrTriangleRotation_from)*share);
     /*       mRecyclerView.measure(
                    View.MeasureSpec.makeMeasureSpec(mRecyclerView.getMeasuredWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(mRecyclerView.getHeight(), View.MeasureSpec.EXACTLY));*/
    }

    public void expandSqueezeTable(long delay){
        ValueAnimator animator = ValueAnimator.ofFloat(0f,1f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float)animation.getAnimatedValue();
                setFractionTableAnimation(value);
                mRecyclerView.postInvalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animationEnd();
            }
        });
        animator.setInterpolator(accelerateDecelerateInterpolator);
        animator.setDuration(animTime);
        animator.setStartDelay(delay);
        mCtrViewHolderInterface.setOnAnimation(true);
        animator.start();
    }

    public void clickToCollapseSqueeze(){
        /*Has to be Overridden!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
           /* if(SubRowAdapter.ActiveRowPos == -1){
                if (!onAnimation &&(rowCollapseContainer.getTop()>=0)) {
                    PlanTableSubRV.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    if (!tableCollapsed) {
                        SubRowAdapter.canClick = false;
                        setAnimationConstsToExpand();
                        rowCollapseContainer.addView(PlanTableSubRV);
                        if (PlanTableSubRV.getHeight() == 0) {
                            PlanTableSubRV.getLayoutParams().height = Math.round(planTableHeight_from - crtContainerHeight_from+oneDp);
                        }
                        PlanTableSubRV.scrollToPosition(0);
                        SubRowAdapter.setDayMap(getAdapterPosition());
                        SubRowAdapter.notifyDataSetChanged();
                        activateCtr();
                        expandSqueezeTable();
                        tableCollapsed = true;
                    } else {
                        SubRowAdapter.canClick = false;
                        planData.dropDaySelection();
                        setAnimationConstsToSqueeze();
                        deactivateCtr();
                        expandSqueezeTable();
                        tableCollapsed = false;
                    }
                }
            }*/
    }

    public void animationEnd(){
        /*OVERRIDE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

            /*tableSubRV.setLayerType(View.LAYER_TYPE_NONE, null);
            onAnimation = false;
            mRecyclerView.getLayoutParams().height = mRecyclerView.getHeight();
            ((RecyclerView.MarginLayoutParams)mRecyclerView.getLayoutParams()).topMargin = mRecyclerView.getTop();

            if(squeeze){
                rowCollapseContainer.setBackground(null);
                rowCollapseContainer.removeView(tableSubRV);
                llm.setScrollEnabled(true);
                chameleonTextRow.setEnabled(true);
            }else {
                SubRowAdapter.canClick = true;
            }*/
    }

    public void programmaticSqueezeWithoutAnimation(){
        /*Override*/

            /*if(collapsedHolderPos == getAdapterPosition()){
                SubRowAdapter.canClick = false;
                planData.dropDaySelection();
                setAnimationConstsToSqueeze();
                deactivateCtr();
                setFractionTableAnimation(1);
                animationEnd();
                tableCollapsed = false;
            }*/
    }

    public void programmaticCollapseWithoutAnimation() {
        /*Override*/

            /*if (!tableCollapsed) {
                SubRowAdapter.canClick = false;
                setAnimationConstsToExpand();
                rowCollapseContainer.addView(PlanTableSubRV);
                if (PlanTableSubRV.getHeight() == 0) {
                    PlanTableSubRV.getLayoutParams().height = Math.round(planTableHeight_from - crtContainerHeight_from + oneDp);
                }
                PlanTableSubRV.scrollToPosition(0);
                SubRowAdapter.setDayMap(getAdapterPosition());
                SubRowAdapter.notifyDataSetChanged();
                activateCtr();
                setFractionTableAnimation(1);
                animationEnd();
                tableCollapsed = true;
            }*/
    }

}


