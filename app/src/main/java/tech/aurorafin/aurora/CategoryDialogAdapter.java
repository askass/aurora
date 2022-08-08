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

import tech.aurorafin.aurora.dbRoom.CategoriesRepository.ACategory;

import java.util.List;

public class CategoryDialogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context mContext;

    List<ACategory> mCategories;

    CategoryDialogCallback mCategoryDialogCallback;
    int categoryColor;
    int lockedTxtColor;


    public CategoryDialogAdapter(Context context, List<ACategory> categories, CategoryDialogCallback categoryDialogCallback){
        this.mContext =context;
        this.mCategories = categories;
        this.mCategoryDialogCallback = categoryDialogCallback;

        categoryColor = ContextCompat.getColor(mContext, R.color.grey_txt_color);
        lockedTxtColor = ContextCompat.getColor(mContext, R.color.grey_line);

    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =null;
        RecyclerView.ViewHolder viewHolder = null;
        if(viewType == ACategory.AGGREGATOR || viewType == ACategory.EMPTY_AGGREGATOR ){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_dialog_item_aggregator,parent,false);
            viewHolder = new AggregatorDialogHolder(view);
        }else{
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_dialog_item, parent,false);
            viewHolder= new CategoryDialogHolder(view);
        }
        view.setTag(viewType);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if(type == ACategory.AGGREGATOR){
            ((AggregatorDialogHolder)holder).setText(position);
        }else if(type == ACategory.EMPTY_AGGREGATOR){
            ((AggregatorDialogHolder)holder).setNoAggregateBkg();
        }else {
            ((CategoryDialogHolder)holder).setTextAndLock(position);

        }
    }

    @Override
    public int getItemViewType(int position) {
        return mCategories.get(position).type;

    }

    @Override
    public int getItemCount() {
        return mCategories.size();
    }



    // HOLDERS:
    public class CategoryDialogHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        LinearLayout category_dialog_item_layout;
        TextView category_dialog_item_text;
        ProgressBar category_dialog_item_progress;

        public CategoryDialogHolder(@NonNull View itemView) {
            super(itemView);
            category_dialog_item_layout = itemView.findViewById(R.id.category_dialog_item_layout);
            category_dialog_item_text = itemView.findViewById(R.id.category_dialog_item_text);
            category_dialog_item_progress = itemView.findViewById(R.id.category_dialog_item_progress);
            category_dialog_item_layout.setOnClickListener(this);

        }

        public void setTextAndLock(int position){
            if(mCategoryDialogCallback.isCategoryLocked(mCategories.get(position).id)){
                category_dialog_item_progress.setVisibility(View.VISIBLE);
                category_dialog_item_layout.setEnabled(false);
                category_dialog_item_text.setTextColor(lockedTxtColor);

            }else {
                category_dialog_item_progress.setVisibility(View.GONE);
                category_dialog_item_layout.setEnabled(true);
                category_dialog_item_text.setTextColor(categoryColor);
            }
            category_dialog_item_text.setText(mCategories.get(position).name);
        }
        @Override
        public void onClick(View view) {
            int pos = getAdapterPosition();
            mCategoryDialogCallback.CategoryClicked(pos, mCategories.get(pos).id);
        }
    }

    public class AggregatorDialogHolder extends RecyclerView.ViewHolder{
        TextView category_dialog_item_aggregator_text;

        public AggregatorDialogHolder (@NonNull View itemView) {
            super(itemView);
            category_dialog_item_aggregator_text = itemView.findViewById(R.id.category_dialog_item_aggregator_text);

        }

        public void setText(int position){
            category_dialog_item_aggregator_text.setText(mCategories.get(position).name);
            category_dialog_item_aggregator_text.setBackground(null);
        }

        public void setNoAggregateBkg(){
            category_dialog_item_aggregator_text.setText("");
            category_dialog_item_aggregator_text.setBackground(ContextCompat.getDrawable(mContext, R.drawable.center_line_light));
        }
    }


    public interface CategoryDialogCallback{
        void CategoryClicked(int position, long id);
        boolean isCategoryLocked(long id);
    }
}
