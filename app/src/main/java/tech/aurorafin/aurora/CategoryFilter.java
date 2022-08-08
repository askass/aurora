package tech.aurorafin.aurora;


import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import tech.aurorafin.aurora.dbRoom.CategoriesRepository;

import java.util.List;
import java.util.Objects;

public class CategoryFilter extends RecyclerView {

    public CategoryFilterAdapter categoryFilterAdapter;

    public CategoryFilter(@NonNull Context context, boolean multiSelect, List<CategoriesRepository.ACategory> categories, CategoryFilterAdapter.CategoryFilterCallback categoryFilterCallback) {
        super(context);
        setMotionEventSplittingEnabled(false);
        ((SimpleItemAnimator) Objects.requireNonNull(getItemAnimator())).setSupportsChangeAnimations(false);
        categoryFilterAdapter = new CategoryFilterAdapter(context,this, multiSelect, categories, categoryFilterCallback);
        setAdapter(categoryFilterAdapter);
        setLayoutManager(new LinearLayoutManager(context));

        float density = context.getResources().getDisplayMetrics().density;
        int Dp = (int)( 10f * density +0.5f);
        addItemDecoration(new ItemOffsetDecoration(Dp));

    }

    public void setSelectedCategory(int index){
        categoryFilterAdapter.setSelectedCategory(index);
    }


    public CategoryFilter(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CategoryFilter(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
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
                outRect.top = offset;
                outRect.right = offset*2;
                outRect.left = offset*2;

            }else if(type == CategoriesRepository.ACategory.EMPTY_AGGREGATOR){
                outRect.right = offset*2;
                outRect.left = offset*2;
                outRect.top = -offset;
                outRect.bottom = 2*offset;
            }else {
                outRect.right = offset*2;
                outRect.left = offset*2;
            }
        }
    }
}
