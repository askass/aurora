package tech.aurorafin.aurora;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;


import tech.aurorafin.aurora.dbRoom.CategoriesRepository;
import tech.aurorafin.aurora.dbRoom.CategoriesRepository.ACategory;


import java.util.HashMap;
import java.util.List;

public class CategoryFilterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context mContext;
    boolean mMultiSelect;
    List<ACategory> mCategories;
    public HashMap<Long, Boolean> selectedCategories;
    int prevCatSettedChecked;
    CategoryFilterCallback mCategoryFilterCallback;

    int txtColor;
    int lockedTxtColor;

    CategoryFilter parentCF;

    public CategoryFilterAdapter(Context context, CategoryFilter parentCF, boolean multiSelect, List<CategoriesRepository.ACategory> categories, CategoryFilterCallback categoryFilterCallback){
        this.mContext =context;
        this.parentCF = parentCF;
        this.mMultiSelect = multiSelect;
        this.mCategories = categories;
        this.mCategoryFilterCallback = categoryFilterCallback;
        selectedCategories = new HashMap<>();

        txtColor = ContextCompat.getColor(mContext, R.color.grey_txt_color);
        lockedTxtColor = ContextCompat.getColor(mContext, R.color.grey_line);

    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =null;
        RecyclerView.ViewHolder viewHolder = null;
        if(viewType == ACategory.AGGREGATOR || viewType == ACategory.EMPTY_AGGREGATOR ){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_filter_item_aggregator,parent,false);
            viewHolder = new AggregatorFilterHolder(view);
        }else{
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.category_filter_item, parent,false);
            viewHolder= new CategoryFilterHolder(view);
        }
        view.setTag(viewType);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if(type == ACategory.AGGREGATOR){
            ((AggregatorFilterHolder)holder).setTextAndMultiSelect(position);
        }else if(type == ACategory.EMPTY_AGGREGATOR){
            ((AggregatorFilterHolder)holder).setNoAggregateBkg();
        }else {
            ((CategoryFilterHolder)holder).setTextAndMultiSelect(position);

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


    /*Setter*/
    public void setSelectedCategory(int index){
        selectedCategories.clear();
        selectedCategories.put(mCategories.get(index).id, true);
        prevCatSettedChecked = index;
        for(int i = 0; i < mCategories.size(); i++){
             notifyItemChanged(i);
        }
    }

    public void categoriesUpdated(int index) {
        /*selectedCategories = new boolean[mCategories.size()];
        selectedCategories[index] = true;
        if(!mMultiSelect){
            prevCatSettedChecked = index;
        }*/
        notifyDataSetChanged();
    }

    public void setAllSelected(boolean selectAll){
        selectedCategories.clear();
        if(selectAll){
            for(int i = 0; i < mCategories.size(); i++){
                if(mCategories.get(i).type != ACategory.AGGREGATOR && mCategories.get(i).type != ACategory.EMPTY_AGGREGATOR )
                    selectedCategories.put(mCategories.get(i).id, true);
                }
        }
        notifyDataSetChanged();
    }


    public void restoreLastAppliedSelectedMap(HashMap<Long, Boolean> selectedCategories){
        this.selectedCategories.clear();
        this.selectedCategories.putAll(selectedCategories);
        notifyDataSetChanged();
    }


    // HOLDERS:
    public class CategoryFilterHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        SpecCheckBox categoryCB;
        ProgressBar category_filter_progress_bar;


        public CategoryFilterHolder(@NonNull View itemView) {
            super(itemView);
            categoryCB = itemView.findViewById(R.id.category_filter_check_box);
            categoryCB.multiSelect = mMultiSelect;
            categoryCB.setOnClickListener(this);
            category_filter_progress_bar = itemView.findViewById(R.id.category_filter_progress_bar);
        }


        public void setTextAndMultiSelect(int position){
            categoryCB.setText(mCategories.get(position).name);
            categoryCB.setChecked(selectedCategories.containsKey(mCategories.get(position).id));
            categoryCB.jumpDrawablesToCurrentState();
            if(mCategoryFilterCallback.isCategoryLocked(mCategories.get(position).id)){
                category_filter_progress_bar.setVisibility(View.VISIBLE);
                setTextColor(true);
            }else{
                category_filter_progress_bar.setVisibility(View.GONE);
                setTextColor(false);
            }
        }

        private void setTextColor(boolean locked){
            if(locked){
                categoryCB.setTextColor(lockedTxtColor);
            }else {
                categoryCB.setTextColor(txtColor);
            }
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            /*if(selectedCategories.length == 0){
                selectedCategories = new boolean[mCategories.size()];
            }*/

            if(categoryCB.isChecked()){
                selectedCategories.put(mCategories.get(position).id, true);
            }else {
                selectedCategories.remove(mCategories.get(position).id);
            }

            if(!mMultiSelect){
                selectedCategories.remove(mCategories.get(prevCatSettedChecked).id);
                notifyItemChanged(prevCatSettedChecked);
                parentCF.scrollBy(0, 0);/*recyclerview-wont-update-child-until-i-scroll*/
                prevCatSettedChecked = position;

            }
            mCategoryFilterCallback.CategoryClicked(position,  mCategories.get(position).type,
                    mCategories.get(position).id);
        }
    }

    public class AggregatorFilterHolder extends RecyclerView.ViewHolder implements View.OnClickListener  {
        TextView aggregateName;

        public AggregatorFilterHolder (@NonNull View itemView) {
            super(itemView);
            aggregateName = itemView.findViewById(R.id.category_filter_item_aggregator);
            if(!mMultiSelect){
                aggregateName.setEnabled(false);
            }
                aggregateName.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            long aggregatorId = mCategories.get(position).id;
            boolean b = false;
            for(int i = position + 1; i < mCategories.size(); i++){
                if(mCategories.get(i).aggregatorId == aggregatorId){
                    if(!selectedCategories.containsKey(mCategories.get(i).id)){
                        b = true;
                        break;
                    }
                }
            }
            for(int i = position + 1; i < mCategories.size(); i++){
                if(mCategories.get(i).aggregatorId == aggregatorId){
                    if(b){
                        selectedCategories.put(mCategories.get(i).id, true);
                    }else {
                        selectedCategories.remove(mCategories.get(i).id);
                    }
                    notifyItemChanged(i);
                }
            }
            mCategoryFilterCallback.CategoryClicked(position, mCategories.get(position).type,
                    mCategories.get(position).id);
        }



        public void setTextAndMultiSelect(int position){
            aggregateName.setText(mCategories.get(position).name);
        }

        public void setNoAggregateBkg(){
            aggregateName.setText("");
            aggregateName.setEnabled(false);
        }


    }


    public interface CategoryFilterCallback{
        void CategoryClicked(int position, int type, long id);
        boolean isCategoryLocked(long id);
    }
}
