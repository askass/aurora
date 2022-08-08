package tech.aurorafin.aurora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import tech.aurorafin.aurora.dbRoom.Operation;

import java.util.List;

import static tech.aurorafin.aurora.DateFormater.getDateFromDateCode;

public class LastOperationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public List<Operation> mLastOperations;
    LastOperationCallback mLastOperationCallback;
    Context mContext;

    int expColor;
    int revColor;
    int capColor;
    int lockedColor;

    String[] shortTypes;
    int[] textColors;
    int formatCode;

    public LastOperationsAdapter(Context context, List<Operation> mLastOperations, LastOperationCallback lastOperationCallback) {
        this.mContext = context;
        this.mLastOperations = mLastOperations;
        mLastOperationCallback = lastOperationCallback;

        lockedColor = ContextCompat.getColor(mContext, R.color.grey_line);
        revColor = ContextCompat.getColor(mContext, R.color.colorPrimary);
        expColor = ContextCompat.getColor(mContext, R.color.grey_txt_color);
        capColor = ContextCompat.getColor(mContext, R.color.black);

        textColors = new int[3];
        textColors[0] = revColor;
        textColors[1] = expColor;
        textColors[2] = capColor;

        //shortTypes = context.getResources().getStringArray(R.array.category_short_types);category_types
        shortTypes = context.getResources().getStringArray(R.array.category_types);
        formatCode = DateFormater.getDateFormatKey(context);

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.last_operation,parent,false);
        return new LastOperationHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((LastOperationHolder)holder).setTextAndLayout(position);
    }

    @Override
    public int getItemCount() {
        return mLastOperations.size();
    }

    /*Locker*/
    public void updateLockState(long categoryId, boolean locked){
        for(int i = 0; i < mLastOperations.size(); i++){
            if(mLastOperations.get(i).categoryId == categoryId){
                notifyItemChanged(i);
            }
        }
    }

    public class LastOperationHolder extends RecyclerView.ViewHolder implements View.OnClickListener  {
        LinearLayout operation_layout;
        TextView operation_date, category_type, category_name, operation_amount;
        ProgressBar operation_progress_bar;

        public LastOperationHolder (@NonNull View itemView) {
            super(itemView);
            operation_layout = itemView.findViewById(R.id.operation_layout);
            operation_date = itemView.findViewById(R.id.operation_date);
            category_type = itemView.findViewById(R.id.category_type);
            category_name = itemView.findViewById(R.id.category_name);
            operation_amount = itemView.findViewById(R.id.operation_amount);
            operation_progress_bar = itemView.findViewById(R.id.operation_progress_bar);
            operation_layout.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int pos = getAdapterPosition();
            mLastOperationCallback.LastOperationClicked(mLastOperations.get(pos));
        }

        public void setTextAndLayout(int pos){
            long categoryId = mLastOperations.get(pos).categoryId;
            int type = mLastOperationCallback.getCategoryType(categoryId);
            if(mLastOperationCallback.isCategoryLocked(categoryId)){
                lockLastOperation();
            }else {
                setGeneralLayout(type);
            }
            setText(pos, categoryId, type);
            //setBackground(pos);
        }

        private void setGeneralLayout(int type){
            operation_layout.setEnabled(true);
            operation_progress_bar.setVisibility(View.GONE);
            int txtColor = expColor;

            if(type > 1 && type < 5){
                txtColor = textColors[type-2];
            }
            setTxtColor(txtColor);
        }

        private void setBackground(int position){
            if(position == mLastOperations.size()-1){
                operation_layout.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple));
            }else {
                operation_layout.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple_transparent_border_bottom));
            }

        }

        private void setText(int position, long categoryId, int type){
            operation_date.setText(getDateLbl(position));
            String s = "";
            if(type>-1 && type < 5){
                s = shortTypes[type];
            }
            category_type.setText(s);
            category_name.setText(mLastOperationCallback.getCategoryName(categoryId));
            operation_amount.setText(PlanData.longNumToString(mLastOperations.get(position).value));

        }

        private String getDateLbl(int position){
            return getDateFromDateCode(mLastOperations.get(position).dateCode, formatCode);
        }


        private void lockLastOperation(){
            operation_layout.setEnabled(false);
            operation_progress_bar.setVisibility(View.VISIBLE);
            setTxtColor(lockedColor);
        }

        private void setTxtColor(int color){
            operation_date.setTextColor(color);
            category_type.setTextColor(color);
            category_name.setTextColor(color);
            operation_amount.setTextColor(color);
        }
    }

    public interface LastOperationCallback{
        void LastOperationClicked(Operation operation);
        boolean isCategoryLocked(long categoryId);
        String getCategoryName(long categoryId);
        int getCategoryType(long categoryId);
    }
}
