package tech.aurorafin.aurora;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.Objects;

public class OperTableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements CtrViewHolder.CtrViewHolderInterface {

    Context mContext;
    OperData mOperData;
    OperData.OperDataCallback mOperDataCallback;

    /*Colors*/
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

    AccelerateDecelerateInterpolator accelerateDecelerateInterpolator;

    /*Parents*/
    MyLinearLayoutManager mLlm;
    RecyclerView mRecyclerView;
    LinearLayout mRecyclerViewContainer;
    RecyclerView mOperTableSubRV;
    OperTableSubRowAdapter operTableSubRowAdapter;
    LinearLayoutManager subLlm;

    /**/
    private boolean onAnimation;
    private boolean tableCollapsed = false;
    OperCtrViewHolder collapsedHolder;
    OperCtrViewHolder pendingCollapseHolder;
    int collapsedHolderPos = -1;
    Handler mHandler;
    Runnable screenLockerRunnable;


    public OperTableAdapter(Context context, OperData operData, OperData.OperDataCallback operDataCallback, MyLinearLayoutManager llm, RecyclerView recyclerView,
                            LinearLayout recyclerViewContainer){

        mContext = context;
        mOperData = operData;
        mOperDataCallback = operDataCallback;
        mHandler = new Handler(Looper.getMainLooper());
        screenLockerRunnable = new Runnable() {
            @Override
            public void run() {
                mOperDataCallback.lockScreenWhileLoadingOperations();
            }
        };

        /*Colors*/
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

        accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();

        /*Parents*/
        mLlm = llm;
        mRecyclerView = recyclerView;
        mRecyclerViewContainer = recyclerViewContainer;

        mOperTableSubRV = new RecyclerView(mContext);
        mOperTableSubRV.setHasFixedSize(true);
        subLlm = new LinearLayoutManager(mContext);
        mOperTableSubRV.setLayoutManager(subLlm);
        operTableSubRowAdapter = new OperTableSubRowAdapter(mContext, mOperData, mOperDataCallback);
        mOperTableSubRV.setAdapter(operTableSubRowAdapter);
        RecyclerView.LayoutParams layoutParams= new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, 0);
        mOperTableSubRV.setLayoutParams(layoutParams);
        mOperTableSubRV.setMotionEventSplittingEnabled(false);
        ((SimpleItemAnimator) Objects.requireNonNull(mOperTableSubRV.getItemAnimator())).setSupportsChangeAnimations(false);



    }



    @Override
    public int getItemViewType(int position) {
        boolean btr = mOperData.getRowType(position);
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
            viewHolder = new PlanTableAdapter.BtrViewHolder(view);
        }
        else
        {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chameleon_text_row,parent,false);
            viewHolder= new OperCtrViewHolder(view, mContext, this,
                    greyTriangle, activatedRow, lightTriangle, lightText, darkText,
                    accelerateDecelerateInterpolator,
                    mLlm, mRecyclerView, mRecyclerViewContainer, mOperTableSubRV);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder.getItemViewType()== 0)
        {
            PlanTableAdapter.BtrViewHolder btrViewHolder = (PlanTableAdapter.BtrViewHolder) holder;
            btrViewHolder.label.setText(mOperData.getBtrLabel(position));
            btrViewHolder.number.setText(mOperData.getBtrValue());
        }
        else {
            OperCtrViewHolder ctrViewHolder = (OperCtrViewHolder) holder;
            ctrViewHolder.label.setText(mOperData.getTotalLabel(position));
            ctrViewHolder.number.setText(mOperData.getTotalValue(position));
            ctrViewHolder.setСollapsible(mOperData.isTotalCollapsible(position));
            ctrViewHolder.setLayout();
        }
    }

    @Override
    public int getItemCount() {
        return mOperData.getTotalsSize();
    }

    /*-------------------------------------------------------------------*/
    public void rebindAdapters(boolean withSub){
        for (int i = 0; i < mOperData.getTotalsSize(); i++ ){
            if(i != collapsedHolderPos){
                notifyItemChanged(i);
            }else {
                collapsedHolder.number.setText(mOperData.getTotalValue(collapsedHolder.getAdapterPosition()));
            }
        }
        if(withSub){
            operTableSubRowAdapter.notifyDataSetChanged();
        }
    }

    public void minimize() {
        if(collapsedHolder != null){
            collapsedHolder.programmaticSqueezeWithoutAnimation();
        }
    }
    public void minimizeWithAnimation() {
        if(collapsedHolder != null){
            collapsedHolder.clickToCollapseSqueeze();
        }
    }

    public void maximize(int tempCollapsedRow) {
        //как-то сделать это!?!?!?!?!?!?!
    }

    public void reverseSubLlm(){
        subLlm.setReverseLayout(mOperData.reverse);
    }

    public void continueCollapse() {
        mHandler.removeCallbacks(screenLockerRunnable);
        if(pendingCollapseHolder != null){
            pendingCollapseHolder.continueCollapse();
        }
    }

    public void cancelCollapsePending() {
        pendingCollapseHolder = null;
        onAnimation = false;
        mLlm.setScrollEnabled(true);
        mHandler.removeCallbacks(screenLockerRunnable);
    }


    /*------------------------------------------------------------------*/


    public class OperCtrViewHolder extends CtrViewHolder{

        public boolean сollapsible = true;

        public OperCtrViewHolder(@NonNull View itemView, Context context, CtrViewHolderInterface ctrViewHolderInterface,
                                 int greyTriangle, int activatedRow, int lightTriangle, int lightText, int darkText,
                                 AccelerateDecelerateInterpolator accelerateDecelerateInterpolator,
                                 MyLinearLayoutManager llm, RecyclerView recyclerView, LinearLayout recyclerViewContainer, RecyclerView tableSubRV) {
            super(itemView, context, ctrViewHolderInterface,
                    greyTriangle, activatedRow, lightTriangle, lightText, darkText,
                    accelerateDecelerateInterpolator,
                    llm, recyclerView, recyclerViewContainer, tableSubRV);
        }

        public void setСollapsible(boolean сollapsible) {
            this.сollapsible = сollapsible;
        }

        @Override
        public void setLayout() {
            super.setLayout();

            if(!сollapsible && !mOperData.localTotalsSelection){
                ctr_triangle.setColorFilter(super.greyTriangle);
            }
        }

        @Override
        public void clickToCollapseSqueeze() {
            if (!onAnimation &&(super.rowCollapseContainer.getTop()>=0)&&сollapsible) {
                mOperTableSubRV.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                if (!tableCollapsed) {
                    onAnimation = true;
                    super.llm.setScrollEnabled(false);
                    pendingCollapseHolder = this;
                    mOperDataCallback.updateOperations(mOperData.getLocalTotalForOperationsUpdate(getAdapterPosition()));
                    mHandler.postDelayed(screenLockerRunnable, 70);


                } else {
                    operTableSubRowAdapter.canClick = false;
                    mOperData.dropOperationsSelection();
                    setAnimationConstsToSqueeze();
                    deactivateCtr();
                    expandSqueezeTable(50);
                    tableCollapsed = false;
                }
            }
        }

        private void continueCollapse(){
            operTableSubRowAdapter.canClick = false;
            setAnimationConstsToExpand();
            super.rowCollapseContainer.addView(mOperTableSubRV);
            if (mOperTableSubRV.getHeight() == 0) {
                mOperTableSubRV.getLayoutParams().height = Math.round(planTableHeight_from - crtContainerHeight_from + super.oneDp);
            }
            mOperTableSubRV.scrollToPosition(0);
            operTableSubRowAdapter.notifyDataSetChanged();
            activateCtr();
            expandSqueezeTable(50);
            tableCollapsed = true;
        }

        @Override
        public void animationEnd() {
            mOperTableSubRV.setLayerType(View.LAYER_TYPE_NONE, null);
            onAnimation = false;
            mRecyclerView.getLayoutParams().height = mRecyclerView.getHeight();
            ((RecyclerView.MarginLayoutParams)mRecyclerView.getLayoutParams()).topMargin = mRecyclerView.getTop();

            if(super.squeeze){
                rowCollapseContainer.setBackground(null);
                rowCollapseContainer.removeView(mOperTableSubRV);
                llm.setScrollEnabled(true);
                super.chameleonTextRow.setEnabled(true);
                notifyAdapterItemChanged(getAdapterPosition());
            }else {
                operTableSubRowAdapter.canClick = true;
            }
        }

        @Override
        public void programmaticSqueezeWithoutAnimation() {
            if(collapsedHolderPos == getAdapterPosition()){
                operTableSubRowAdapter.canClick = false;
                mOperData.dropOperationsSelection();
                setAnimationConstsToSqueeze();
                deactivateCtr();
                super.setFractionTableAnimation(1);
                animationEnd();
                tableCollapsed = false;
            }
        }

        @Override
        public void programmaticCollapseWithoutAnimation() {
        }
    }


    /*CtrViewHolder.CtrViewHolderInterface */

    @Override
    public boolean isTotalSelected(int position) {
        return mOperData.isTotalSelected(position);
    }

    @Override
    public int getCollapsedHolderPos() {
        return collapsedHolderPos;
    }

    @Override
    public void setCollapsedHolderPos(int position, boolean backBtnActionEnabled) {
        collapsedHolderPos = position;
        mOperDataCallback.backButtonCollapse(backBtnActionEnabled);
    }

    @Override
    public void setCollapsedHolder(CtrViewHolder ctrViewHolder) {
        collapsedHolder =(OperCtrViewHolder)ctrViewHolder;
    }

    @Override
    public boolean isLocalTotalSelection() {
        return mOperData.localTotalsSelection;
    }

    @Override
    public void setRowSelected(int position, boolean total) {
        mOperData.setOperationSelected(position, total);
    }

    @Override
    public void setRowUnselected(int position, boolean total) {
        mOperData.setOperationUnselected(position, total);
    }

    @Override
    public void setOnAnimation(boolean onAnimation){
        this.onAnimation = onAnimation;
    }

    @Override
    public void notifyAdapterItemChanged(int position) {
        notifyItemChanged(position);
    }
}
