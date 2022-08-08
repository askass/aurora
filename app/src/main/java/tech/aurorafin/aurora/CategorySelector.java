package tech.aurorafin.aurora;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

import tech.aurorafin.aurora.dbRoom.CategoriesRepository;

public class CategorySelector extends RecyclerView {

    public CategorySelectorAdapter categorySelectorAdapter;



    public CategorySelector(@NonNull Context context, boolean multiSelect, int defTxtColor, List<CategoriesRepository.ACategory> categories, CategorySelectorAdapter.CategorySelectorCallback categorySelectorCallback) {
        super(context);

        setMotionEventSplittingEnabled(false);

        categorySelectorAdapter = new CategorySelectorAdapter(context, multiSelect, defTxtColor, categories, categorySelectorCallback);
        setAdapter(categorySelectorAdapter);
        GridLayoutManager mLayoutManager = new GridLayoutManager(context, 2);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch(categorySelectorAdapter.getItemViewType(position)){
                    case CategoriesRepository.ACategory.AGGREGATOR:
                        return 2;
                    case CategoriesRepository.ACategory.EMPTY_AGGREGATOR:
                        return 2;
                    default:
                        return 1;
                }
            }
        });
        setLayoutManager(mLayoutManager);

        float density = context.getResources().getDisplayMetrics().density;
        int Dp = (int)( 10f * density +0.5f);

        setClipToPadding(false);
        setPadding(Dp,0,Dp,0);

        addItemDecoration(new ItemOffsetDecoration(Dp));

    }



    public CategorySelector(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CategorySelector(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public class ItemOffsetDecoration extends RecyclerView.ItemDecoration {
        private int offset;

        public ItemOffsetDecoration(int offset) {
            this.offset = offset;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view,
                                   RecyclerView parent, RecyclerView.State state) {

            // Добавление отступов

                int type = (int)view.getTag();

                if(type == CategoriesRepository.ACategory.AGGREGATOR){
                    outRect.right = 0;
                    outRect.left = 0;
                    outRect.top = offset/2;
                    outRect.bottom = offset/4;
                }else if(type == CategoriesRepository.ACategory.EMPTY_AGGREGATOR){
                    outRect.right = offset;
                    outRect.left = offset;
                    outRect.top = offset*2;
                    outRect.bottom = -offset;
                }else {
                    outRect.right = offset;
                    outRect.left = offset;
                    outRect.top = 0;
                    outRect.bottom = offset;
                }
        }
    }

    public static class ACategoryDiffUtilCallback extends DiffUtil.Callback{

        List<CategoriesRepository.ACategory> oldList;
        List<CategoriesRepository.ACategory> newList;

        public ACategoryDiffUtilCallback(List<CategoriesRepository.ACategory> oldList, List<CategoriesRepository.ACategory> newList){
            this.oldList = oldList;
            this.newList = newList;
        }

        public void setNewLists(List<CategoriesRepository.ACategory> oldList, List<CategoriesRepository.ACategory> newList){
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).id == newList.get(newItemPosition).id &&
                    oldList.get(oldItemPosition).isAggregator == newList.get(newItemPosition).isAggregator;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).name == newList.get(newItemPosition).name&&
                    oldList.get(oldItemPosition).type == newList.get(newItemPosition).type&&
                    oldList.get(oldItemPosition).active == newList.get(newItemPosition).active;
        }
    }

}


