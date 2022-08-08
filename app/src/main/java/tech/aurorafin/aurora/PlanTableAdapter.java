package tech.aurorafin.aurora;


import android.content.Context;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.Objects;

public class PlanTableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements CtrViewHolder.CtrViewHolderInterface {

    PlanData planData;
    Context mContext;
    RecyclerView mRecyclerView;
    SpecLinearLayout recyclerViewContainer;
    MyLinearLayoutManager llm;
    PlanData.InputManager mInputManager;

    int darkRow;
    int lightRow;
    int activatedRow;
    int lightTriangle;
    int darkTriangle;
    int greyTriangle;
    int lightText;
    int darkText;
    int black;
    int darkGrey;

    int oneDp;
    int Dp42;

    boolean tableCollapsed = false;
    boolean onAnimation = false;

    RecyclerView PlanTableSubRV;
    PlanTableSubRowAdapter SubRowAdapter;
    PlanCtrViewHolder collapsedHolder;
    int collapsedHolderPos = -1;

    private AccelerateDecelerateInterpolator accelerateDecelerateInterpolator;

    public PlanTableAdapter(Context mContext, PlanData planData,
                            PlanData.InputManager inputManager,
                            SpecLinearLayout recyclerViewContainer,
                            MyLinearLayoutManager llm) {

        this.mContext = mContext;
        this.planData = planData;
        //this.ctrRowClickListener = ctrRowClickListener;
        this.llm = llm;
        mInputManager = inputManager;
        this.recyclerViewContainer = recyclerViewContainer;

        darkRow = ContextCompat.getColor(mContext, R.color.grey_row);
        lightRow = ContextCompat.getColor(mContext, R.color.light_row);

        activatedRow = ContextCompat.getColor(mContext, R.color.blue_row);
        lightTriangle = ContextCompat.getColor(mContext, R.color.white_txt_color);
        darkTriangle = ContextCompat.getColor(mContext, R.color.table_txt_color);
        greyTriangle = ContextCompat.getColor(mContext, R.color.grey_line);
        lightText = ContextCompat.getColor(mContext, R.color.white_txt_color);
        darkText = ContextCompat.getColor(mContext, R.color.table_txt_color);
        black = ContextCompat.getColor(mContext, R.color.black);
        darkGrey = ContextCompat.getColor(mContext, R.color.dark_grey);

        PlanTableSubRV = new RecyclerView(mContext);
        PlanTableSubRV.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(mContext);
        PlanTableSubRV.setLayoutManager(lm);
        SubRowAdapter = new PlanTableSubRowAdapter(mContext, planData, inputManager, lm);
        PlanTableSubRV.setAdapter(SubRowAdapter);
        RecyclerView.LayoutParams layoutParams= new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, 0);
        PlanTableSubRV.setLayoutParams(layoutParams);
        PlanTableSubRV.setMotionEventSplittingEnabled(false);
        ((SimpleItemAnimator) Objects.requireNonNull(PlanTableSubRV.getItemAnimator())).setSupportsChangeAnimations(false);
        accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();

        float density = mContext.getResources().getDisplayMetrics().density;
        oneDp = (int)(density);
        Dp42 = (int)(42f * density +0.5f);

    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;

    }

    @Override
    public int getItemViewType(int position) {
        boolean btr = planData.getRowType(position);
        if(btr)
            return 0;
        else
            return 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =null;
        RecyclerView.ViewHolder viewHolder = null;
        if(viewType==0)
        {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.blue_text_row,parent,false);
            viewHolder = new BtrViewHolder(view);
        }
        else
        {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chameleon_text_row,parent,false);
            viewHolder= new PlanCtrViewHolder(view, mContext, this,
                    greyTriangle, activatedRow, lightTriangle, lightText, darkText,
                    accelerateDecelerateInterpolator,
                    llm, mRecyclerView, recyclerViewContainer, PlanTableSubRV);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder.getItemViewType()== 0)
        {
            BtrViewHolder btrViewHolder = (BtrViewHolder) holder;
            btrViewHolder.label.setText(planData.getBtrLabel(position));
            btrViewHolder.number.setText(planData.getBtrValue());
        }
        else {
            PlanCtrViewHolder ctrViewHolder = (PlanCtrViewHolder) holder;
            ctrViewHolder.label.setText(planData.getTotalLabel(position));
            ctrViewHolder.number.setText(planData.getTotalValue(position));
            ctrViewHolder.setLayout();
        }
    }

    @Override
    public int getItemCount() {
        return planData.getTotalsSize();
    }

    // ------------------------Handle input mode ---------------------------------------------------
    public void minimize(){
        if(collapsedHolder != null){
            collapsedHolder.programmaticSqueezeWithoutAnimation();
        }
    }

    public void minimizeWithAnimation(){
        if(collapsedHolder != null){
            collapsedHolder.clickToCollapseSqueeze();
        }
    }

    public void maximize(int position){
        if(position!=0 && position<planData.getTotalsSize()){
            PlanCtrViewHolder tempHolder = (PlanCtrViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
            if(tempHolder!=null){
                tempHolder.programmaticCollapseWithoutAnimation();
            }
        }
    }

    public void rebindAdapters(boolean withSub){
        int p = -1;
        if(collapsedHolder != null){
            p = collapsedHolder.getAdapterPosition();
        }
        for (int i = 0; i < planData.getTotalsSize(); i++ ){
            if(i != p){
                notifyItemChanged(i);
            }else {
                collapsedHolder.number.setText(planData.getTotalValue(collapsedHolder.getAdapterPosition()));
            }
        }
        if(withSub){
            SubRowAdapter.notifyDataSetChanged();
        }
    }

    public void rebindSubRVAdapter(){
        SubRowAdapter.notifyDataSetChanged();
    }

    public void updateSubRvHeight(int winHeight){
        int newHeight = Math.round(winHeight - Dp42 + oneDp);
        PlanTableSubRV.getLayoutParams().height = newHeight;
        SubRowAdapter.updateScrollPosition(newHeight);
    }

    public void deactivateInputMode(){
        SubRowAdapter.deactivateInputMode();
    }
    public void deactivateInputModeWithHandler(){
        SubRowAdapter.deactivateInputModeWithHandler();
    }

    public void updateTotals(){
        if(collapsedHolder != null){
            collapsedHolder.number.setText(planData.getTotalValue(collapsedHolder.getAdapterPosition()));
        }
        notifyItemChanged(0);
        SubRowAdapter.updateActiveInputRow();
    }

    public void lockCollapsedRow(){
        onAnimation = true;
        if(collapsedHolder != null){
            collapsedHolder.ctr_triangle.setColorFilter(darkGrey);
        }
    }

    public void unlockCollapsedRow(){
        onAnimation = false;
        if(collapsedHolder != null){
            collapsedHolder.ctr_triangle.setColorFilter(lightTriangle);
        }
    }




    // ---------------------------------------------------------------------------------------------

    // Blue Type Row(Btr) View Holder
    public static class BtrViewHolder extends RecyclerView.ViewHolder {
        TextView label, number;
        public BtrViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.btr_text_lbl);
            number = itemView.findViewById(R.id.btr_value_lbl);
        }
    }


    // Chameleon Type Row (Ctr) View Holder

    public class PlanCtrViewHolder extends CtrViewHolder{

        public PlanCtrViewHolder(@NonNull View itemView, Context context, CtrViewHolderInterface ctrViewHolderInterface,
                                 int greyTriangle, int activatedRow, int lightTriangle, int lightText, int darkText,
                                 AccelerateDecelerateInterpolator accelerateDecelerateInterpolator,
                                 MyLinearLayoutManager llm, RecyclerView recyclerView, LinearLayout recyclerViewContainer, RecyclerView tableSubRV) {
            super(itemView, context, ctrViewHolderInterface,
                    greyTriangle, activatedRow, lightTriangle, lightText, darkText,
                    accelerateDecelerateInterpolator,
                    llm, recyclerView, recyclerViewContainer, tableSubRV);
        }

        @Override
        public void clickToCollapseSqueeze() {
            if(SubRowAdapter.ActiveRowPos == -1){
                if (!onAnimation &&(super.rowCollapseContainer.getTop()>=0)) {
                    PlanTableSubRV.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    if (!tableCollapsed) {
                        SubRowAdapter.canClick = false;
                        setAnimationConstsToExpand();
                        super.rowCollapseContainer.addView(PlanTableSubRV);
                        if (PlanTableSubRV.getHeight() == 0) {
                            PlanTableSubRV.getLayoutParams().height = Math.round(planTableHeight_from - crtContainerHeight_from + super.oneDp);
                        }
                        PlanTableSubRV.scrollToPosition(0);
                        SubRowAdapter.setDayMap(getAdapterPosition());
                        SubRowAdapter.notifyDataSetChanged();
                        activateCtr();
                        expandSqueezeTable(70);
                        tableCollapsed = true;
                    } else {
                        SubRowAdapter.canClick = false;
                        planData.dropDaySelection();
                        setAnimationConstsToSqueeze();
                        deactivateCtr();
                        expandSqueezeTable(70);
                        tableCollapsed = false;
                    }
                }
            }
        }

        @Override
        public void animationEnd() {
            PlanTableSubRV.setLayerType(View.LAYER_TYPE_NONE, null);
            onAnimation = false;
            mRecyclerView.getLayoutParams().height = mRecyclerView.getHeight();
            ((RecyclerView.MarginLayoutParams)mRecyclerView.getLayoutParams()).topMargin = mRecyclerView.getTop();

            if(super.squeeze){
                rowCollapseContainer.setBackground(null);
                rowCollapseContainer.removeView(PlanTableSubRV);
                llm.setScrollEnabled(true);
                super.chameleonTextRow.setEnabled(true);
            }else {
                SubRowAdapter.canClick = true;
            }
        }

        @Override
        public void programmaticSqueezeWithoutAnimation() {
            if(collapsedHolderPos == getAdapterPosition()){
                SubRowAdapter.canClick = false;
                planData.dropDaySelection();
                setAnimationConstsToSqueeze();
                deactivateCtr();
                super.setFractionTableAnimation(1);
                animationEnd();
                tableCollapsed = false;
            }
        }

        @Override
        public void programmaticCollapseWithoutAnimation() {
             if (!tableCollapsed) {
                SubRowAdapter.canClick = false;
                setAnimationConstsToExpand();
                rowCollapseContainer.addView(PlanTableSubRV);
                if (PlanTableSubRV.getHeight() == 0) {
                    PlanTableSubRV.getLayoutParams().height = Math.round(super.planTableHeight_from - super.crtContainerHeight_from + oneDp);
                }
                PlanTableSubRV.scrollToPosition(0);
                SubRowAdapter.setDayMap(getAdapterPosition());
                SubRowAdapter.notifyDataSetChanged();
                activateCtr();
                setFractionTableAnimation(1);
                animationEnd();
                tableCollapsed = true;
            }
        }
    }


    /*CtrViewHolderInterface*/
    @Override
    public boolean isTotalSelected(int position) {
        return planData.isTotalSelected(position);
    }

    @Override
    public int getCollapsedHolderPos() {
        return collapsedHolderPos;
    }

    @Override
    public boolean isLocalTotalSelection() {
        return planData.localTotalsSelection;
    }

    @Override
    public void setRowSelected(int position, boolean total){
        planData.setRowSelected(position, total);
    }

    @Override
    public void setRowUnselected(int position, boolean total){
        planData.setRowUnselected(position, total);
    }

    @Override
    public void setCollapsedHolder(CtrViewHolder ctrViewHolder){
        collapsedHolder =(PlanCtrViewHolder)ctrViewHolder;
    }
    @Override
    public void setCollapsedHolderPos(int position, boolean backBtnActionEnabled){
        collapsedHolderPos = position;
        mInputManager.backButtonCollapse(backBtnActionEnabled);
    }

    @Override
    public void setOnAnimation(boolean b){
        onAnimation = b;
    }

    @Override
    public void notifyAdapterItemChanged(int position) {
        notifyItemChanged(position);
    }



}



