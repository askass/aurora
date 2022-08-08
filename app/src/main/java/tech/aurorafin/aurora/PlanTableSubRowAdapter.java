package tech.aurorafin.aurora;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PlanTableSubRowAdapter extends RecyclerView.Adapter<PlanTableSubRowAdapter.InputRowHolder>  {

    final Context mContext;
    PlanData planData;
    PlanData.InputManager mInputManager;
    LinearLayoutManager mLayoutManager;
    RecyclerView mRecyclerView;

    public int ActiveRowPos = -1;
    int ActiveRowPos_prev = -1;
    int ActiveRowBot;
    long delay;

    int dayMapFrom = 0;
    int dayMapTo = 0;

    public boolean canClick = false;

    //int greyText;
    //int darkText;

    Handler handler;
    Runnable r;

    public PlanTableSubRowAdapter(final Context mContext, PlanData planData, PlanData.InputManager inputManager, LinearLayoutManager mLM){
        this.mContext = mContext;
        this.planData = planData;
        this.mInputManager = inputManager;
        this.mLayoutManager = mLM;

        //greyText  = ContextCompat.getColor(mContext, R.color.table_txt_color);
       // darkText  = ContextCompat.getColor(mContext, R.color.table_txt_color);

        handler = new Handler(Looper.getMainLooper());
        r = new Runnable() {
            public void run() {
                //do your stuff here after DELAY milliseconds
                notifyDataSetChanged();
                if(ActiveRowPos == -1){
                    mInputManager.deactivateInputLayout();
                }else {
                    mInputManager.activateInputLayout();
                }

            }
        };
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }


    @NonNull
    @Override
    public InputRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout;
        layout = LayoutInflater.from(mContext).inflate(R.layout.input_text_row, parent, false);
        return new InputRowHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull InputRowHolder holder, int position) {
        holder.label.setText(planData.getPlanDayLabel(dayMapFrom + position));
        holder.number.setText(planData.getPlanDayValue(dayMapFrom + position));
        holder.setLayout();
    }

    @Override
    public int getItemCount() {
        return dayMapTo - dayMapFrom + 1;
    }




    public void updateActiveInputRow(){
        if(ActiveRowPos != -1){
            InputRowHolder h;
            h = (InputRowHolder) mRecyclerView.findViewHolderForAdapterPosition(ActiveRowPos);
            if (h != null) {
                h.number.setText(planData.getPlanDayValue(dayMapFrom + ActiveRowPos));
            }else{
                notifyDataSetChanged();
                mRecyclerView.scrollToPosition(ActiveRowPos);
            }
        }
    }

    public void setDayMap(int position){
        dayMapFrom = planData.getDayMapFrom(position);
        dayMapTo = planData.getDayMapTo(position);
    }


    public int getActiveRowPos() {
        return ActiveRowPos;
    }
    public int getActiveRowBot() {
        return ActiveRowBot;
    }

    public void deactivateInputMode(){
        ActiveRowPos = -1;
        notifyDataSetChanged();
    }

    public void deactivateInputModeWithHandler(){
        ActiveRowPos = -1;
        int delay;
        delay = 180;
        handler.postDelayed(r, delay);
        mInputManager.setPlanInputEditDay(-1);
    }

    public void updateScrollPosition(int newHeight){
        if(//Нужно переписать, и получать ActiveRowBot динамически
           (ActiveRowPos!=-1)&
            ((newHeight < ActiveRowBot)||
             (mLayoutManager.findViewByPosition(ActiveRowPos) == null))
          ){
            mRecyclerView.scrollToPosition(ActiveRowPos);
        }
    }

    public void removeInputBkg(int pos) {

        InputRowHolder h = (InputRowHolder) mRecyclerView.findViewHolderForAdapterPosition(pos);
        if (h != null) {
            h.inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple_wt));
        }

        if ((pos - 1) != ActiveRowPos) {
            InputRowHolder h1 = (InputRowHolder) mRecyclerView.findViewHolderForAdapterPosition(pos - 1);
            if (h1 != null) {
                h1.inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple_wt));
           }
        }
    }

    public class InputRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        TextView label, number;
        LinearLayout inputTextRow;
        ImageView itr_ok_circle_img;

        public InputRowHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.itr_text_lbl);
            number = itemView.findViewById(R.id.itr_value_lbl);
            inputTextRow = itemView.findViewById(R.id.input_text_row);
            itr_ok_circle_img = itemView.findViewById(R.id.itr_ok_circle_img);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View view) {
            if(ActiveRowPos != -1){
                return false;
            }else {
                handleItrSelection(dayMapFrom + getAdapterPosition());
                return true;
            }
            //inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_selected_row_gr));
        }

        @Override
        public void onClick(View view) {
            if(canClick){
                if(planData.planDaySelection){
                    handleItrSelection(dayMapFrom + getAdapterPosition());
                }else{
                    generalClickAction(view);
                }
            }
        }

        public void generalClickAction(View view){
            ActiveRowPos_prev = ActiveRowPos;
            if(ActiveRowPos != getAdapterPosition()) {
                ActiveRowPos = getAdapterPosition();
                ActiveRowBot = view.getBottom();
                int delay;
                if(ActiveRowPos_prev == -1){
                    delay = 180;
                    mInputManager.lockFilterAndCollapsedRow();
                    handler.postDelayed(r, delay);
                }else {
                    notifyDataSetChanged();
                }
                mInputManager.setPlanInputEditDay(dayMapFrom + ActiveRowPos);

            }else{
                mInputManager.deactivateInputLayout();
                ActiveRowPos = -1;
                delay = 180;
                handler.postDelayed(r, delay);
                mInputManager.setPlanInputEditDay(-1);

            }
        }

        public void handleItrSelection(int pos){
            if(planData.isDaySelected(pos)){
                planData.setRowUnselected(pos, false);
            }else {
                planData.setRowSelected(pos,false);
            }
            notifyItemChanged(getAdapterPosition());
        }


        public void setLayout() {
            int pos = getAdapterPosition();
            if (ActiveRowPos != -1) {
                if (pos == ActiveRowPos) {
                    inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_bkg_selected_input));
                    //darkTextColor();
                } else if ((pos == (ActiveRowPos - 1))) {
                    inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_bkg_selected_wt));
                    //greyTextColor();
                } else {
                    inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_wt));
                    //greyTextColor();
                }
            } else {
                if (planData.isDaySelected(dayMapFrom + pos)) {
                    inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_selected_row_gr));
                    itr_ok_circle_img.setVisibility(View.VISIBLE);
                } else {
                    inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple_wt));
                    itr_ok_circle_img.setVisibility(View.INVISIBLE);
                }
            }

        }
    }


}
