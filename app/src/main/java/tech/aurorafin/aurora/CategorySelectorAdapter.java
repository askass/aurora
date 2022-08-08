package tech.aurorafin.aurora;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

import tech.aurorafin.aurora.dbRoom.CategoriesRepository;

public class CategorySelectorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context mContext;
    boolean multiSelect;
    int defTxtColor;
    public List<CategoriesRepository.ACategory> categories;
    CategorySelectorCallback mCategorySelectorCallback;
    int inactiveCapColor;
    int activeCapColor;

    public CategorySelectorAdapter(Context context, boolean multiSelect, int defTxtColor, List<CategoriesRepository.ACategory> categories, CategorySelectorCallback categorySelectorCallback){
        this.mContext = context;
        this.multiSelect = multiSelect;
        this.defTxtColor = defTxtColor;
        this.categories =categories;
        this.mCategorySelectorCallback = categorySelectorCallback;
        inactiveCapColor = ContextCompat.getColor(mContext, R.color.grey_line);
        activeCapColor = ContextCompat.getColor(mContext, R.color.blue_row);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =null;
        RecyclerView.ViewHolder viewHolder = null;
        if(viewType == CategoriesRepository.ACategory.AGGREGATOR || viewType == CategoriesRepository.ACategory.EMPTY_AGGREGATOR ){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_item_aggregat,parent,false);
            viewHolder = new AggregatorHolder(view);
        }else{
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_item, parent,false);
            viewHolder= new CategoryHolder(view);
        }
        view.setTag(viewType);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if(type == CategoriesRepository.ACategory.AGGREGATOR){
            ((AggregatorHolder)holder).aggregateName.setText(categories.get(position).name);
        }else if(type == CategoriesRepository.ACategory.EMPTY_AGGREGATOR){
            ((AggregatorHolder)holder).setNoAggregateBkg();
        }else {
            ((CategoryHolder)holder).setTextAndBkg(position, type);
        }

    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    @Override
    public int getItemViewType(int position) {
        return categories.get(position).type;

    }


    // HOLDERS:
    public class CategoryHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView categoryName, categoryType;
        FrameLayout category_container;
        ProgressBar category_progress_bar;

        public CategoryHolder(@NonNull View itemView) {
            super(itemView);
            category_container = itemView.findViewById(R.id.category_container);
            categoryName = itemView.findViewById(R.id.category_name);
            categoryType = itemView.findViewById(R.id.category_type);
            categoryName.setOnClickListener(this);
            category_progress_bar = itemView.findViewById(R.id.category_progress_bar);
        }

        public void setTextAndBkg(int position, int type){
            categoryName.setText(categories.get(position).name);
            String stype = categories.get(position).textType;
            categoryType.setText(stype);
            if(type == CategoriesRepository.ACategory.CAPITAL){
                if(categories.get(position).active){
                    categoryType.setTextColor(activeCapColor);
                }else {
                    categoryType.setTextColor(inactiveCapColor);
                }
            }

            if(mCategorySelectorCallback.isCategoryLocked(categories.get(position).id)){
                category_progress_bar.setVisibility(View.VISIBLE);
                categoryType.setVisibility(View.GONE);
                categoryName.setTextColor(inactiveCapColor);
                categoryName.setEnabled(false);
            }else {
                category_progress_bar.setVisibility(View.GONE);
                categoryType.setVisibility(View.VISIBLE);
                categoryName.setTextColor(defTxtColor);
                categoryName.setEnabled(true);
            }
        }

        @Override
        public void onClick(View view) {
            mCategorySelectorCallback.CategoryClicked(getAdapterPosition());
        }
    }

    public class AggregatorHolder extends RecyclerView.ViewHolder implements View.OnClickListener  {
        TextView aggregateName;
        FrameLayout aggregate_container;
        public AggregatorHolder (@NonNull View itemView) {
            super(itemView);
            aggregateName = itemView.findViewById(R.id.aggregate_name);
            aggregate_container = itemView.findViewById(R.id.aggregate_container);
            aggregateName.setOnClickListener(this);
        }

        public void setNoAggregateBkg(){
            aggregateName.setText("");
            aggregateName.setBackground(null);
            aggregateName.setEnabled(false);
            aggregate_container.setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_transparent_border_top));
        }

        @Override
        public void onClick(View view) {
            mCategorySelectorCallback.CategoryClicked(getAdapterPosition());
        }
    }

    public interface CategorySelectorCallback{
        void CategoryClicked(int position);
        boolean isCategoryLocked(long id);
    }



}
