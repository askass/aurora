package tech.aurorafin.aurora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class OperTableSubRowAdapter extends RecyclerView.Adapter<OperTableSubRowAdapter.OperationRowHolder> {

    Context mContext;
    OperData mOperData;
    OperData.OperDataCallback mOperDataCallback;

    public boolean canClick = true;

    public OperTableSubRowAdapter(Context context, OperData operData, OperData.OperDataCallback operDataCallback) {
        mContext = context;
        mOperData = operData;
        mOperDataCallback = operDataCallback;
    }

    @NonNull
    @Override
    public OperTableSubRowAdapter.OperationRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout;
        layout = LayoutInflater.from(mContext).inflate(R.layout.input_text_row, parent, false);
        return new OperationRowHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull OperTableSubRowAdapter.OperationRowHolder holder, int position) {
        holder.label.setText(mOperData.getOperationLabel(position));
        holder.number.setText(mOperData.getOperationValue(position));
        holder.setLayout();
    }

    @Override
    public int getItemCount() {
        return mOperData.mOperations.size();
    }


    public class OperationRowHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        TextView label, number;
        LinearLayout inputTextRow;
        ImageView itr_ok_circle_img;

        public OperationRowHolder(@NonNull View itemView) {
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

                handleItrSelection(getAdapterPosition());
                return true;

            //inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_selected_row_gr));
        }

        @Override
        public void onClick(View view) {
            if(canClick){
                if(mOperData.operationsRowsSelection){
                    handleItrSelection(getAdapterPosition());
                }else{
                    generalClickAction(view);
                }
            }
        }

        public void generalClickAction(View view){
            /*Open detail view*/
            mOperDataCallback.operationClicked(mOperData.mOperations.get(getAdapterPosition()));
        }

        public void handleItrSelection(int pos){
            if(mOperData.isOperationSelected(pos)){
                mOperData.setOperationUnselected(pos, false);
            }else {
                mOperData.setOperationSelected(pos,false);
            }
            notifyItemChanged(getAdapterPosition());
        }


        public void setLayout() {
            int pos = getAdapterPosition();
            if (mOperData.isOperationSelected(pos)) {
                inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_selected_row_gr));
                itr_ok_circle_img.setVisibility(View.VISIBLE);
            } else {
                inputTextRow.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple_wt));
                itr_ok_circle_img.setVisibility(View.INVISIBLE);
            }
        }
    }

}
