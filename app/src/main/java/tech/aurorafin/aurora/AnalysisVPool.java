package tech.aurorafin.aurora;

import android.content.Context;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;


import java.util.ArrayList;
import java.util.List;

public class AnalysisVPool {

    Context mContext;
    private static final int maxPoolSize = 10;

    List<TextView> wfLabels;
    List<WfBar>wfBars;
    List<AnalysisTableRow> tableRows;
    int Dp5, Dp20, Dp100, Dp120;
    int Dp10;
    int Dp30;
    int Dp40;
    int Dp130;
    int labelTxtColor;

    int totalColor;
    int positiveColor;
    int negativeColor;
    int selectedRowColor;
    int text16Size;
    int text15Size;
    int text14Size;
    int Dp1;

    TextPaint paint;

    AnalysisTableRowClick mAnalysisTableRowClick;


    public interface AnalysisTableRowClick{
        void rowSelected(int dataPosition);
        void longRowClick(int dataPosition);
    }

    public AnalysisVPool(Context context, AnalysisTableRowClick analysisTableRowClick) {
        this.mContext = context;
        mAnalysisTableRowClick = analysisTableRowClick;
        wfLabels = new ArrayList<>();
        wfBars = new ArrayList<>();
        tableRows = new ArrayList<>();

        float density = mContext.getResources().getDisplayMetrics().density;
        Dp1 = (int)(1f * density + 0.5f);
        Dp10 = (int)(10f * density + 0.5f);
        Dp30 = (int)(30f * density + 0.5f);
        Dp40 = (int)(40f * density + 0.5f);
        Dp130 = (int)(130f * density + 0.5f);

        Dp5 = (int)(5f * density + 0.5f);
        Dp20 = (int)(20f * density + 0.5f);
        Dp100 = (int)(100f * density + 0.5f);
        Dp120 = (int)(120f * density + 0.5f);

        labelTxtColor = ContextCompat.getColor(mContext,  R.color.grey_txt_color);
        totalColor = ContextCompat.getColor(mContext,  R.color.colorPrimary);
        positiveColor = ContextCompat.getColor(mContext,  R.color.blue_row);
        negativeColor = ContextCompat.getColor(mContext,  R.color.grey_line);
        selectedRowColor = ContextCompat.getColor(mContext,  R.color.selected_row_wt);
        text16Size = mContext.getResources().getDimensionPixelSize(R.dimen.Sp16Size);
        text15Size = mContext.getResources().getDimensionPixelSize(R.dimen.barLabelFontSize);
        text14Size = mContext.getResources().getDimensionPixelSize(R.dimen.Sp14Size);
        paint = new TextPaint();

    }

    public float getWfLabelWidth(String label){
        paint.setTextSize(text15Size);
        return paint.measureText(label) +  Dp1*3;
    }

    public float getTotalLabelWidth(String label){
        paint.setTextSize(text15Size);
        return paint.measureText(label);
    }
    public float getSmallLabelWidth(String label){
        paint.setTextSize(text14Size);
        return paint.measureText(label);
    }

    public void putWfLabel(TextView label){
        if(wfLabels.size()<=maxPoolSize && label != null){
            wfLabels.add(label);
        }
    }

    public TextView getWfLabel(){
        if(wfLabels.size() > 0){
            TextView label;
            label = wfLabels.get(wfLabels.size()-1);
            wfLabels.remove(wfLabels.size()-1);
            return label;
        }else {
            return getNewWfLabel();
        }
    }

    private TextView getNewWfLabel() {
        TextView label = new TextView(mContext);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(Dp130, Dp40);
        label.setLayoutParams(lp2);
        label.setPadding(0,0, Dp30,0);
        label.setTextColor(labelTxtColor);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setSingleLine(true);
        return label;
    }

    public WfBar getWfBar(){
        if(wfBars.size() > 0){
            WfBar wfBar;
            wfBar = wfBars.get(wfBars.size()-1);
            wfBars.remove(wfBars.size()-1);
            return wfBar;
        }else {
            return getNewWfBar();
        }
    }
    public void putWfBar(WfBar wfBar){
        if(wfBars.size()<=maxPoolSize && wfBar != null){
            wfBars.add(wfBar);
        }
    }
    public WfBar getNewWfBar(){
        return new WfBar(mContext, totalColor, positiveColor, negativeColor, text15Size, labelTxtColor, Dp1, Dp40);
    }



    public AnalysisTableRow getTableRow(){
        if(tableRows.size() > 0){
            AnalysisTableRow tr;
            tr = tableRows.get(tableRows.size()-1);
            tableRows.remove(tableRows.size()-1);
            return tr;
        }else {
            return getNewTableRow();
        }
    }

    public void putTableRow(AnalysisTableRow tr){
        if(tableRows.size()<=maxPoolSize && tr != null){
            tableRows.add(tr);
        }
    }

    public AnalysisTableRow getNewTableRow(){
        AnalysisTableRow temp = new AnalysisTableRow(mContext, labelTxtColor, positiveColor);
        return temp;
    }

    public class AnalysisTableRow extends LinearLayout implements View.OnClickListener {
            Context mContext;
            LinearLayout labelHolder;
            AppCompatImageView plusMinusImg;
            TextView label, plan, fact, delta;
            String absDelta, percentDelta;
            int labelTxtColor, positiveColor;
            List<AnalysisTableRow> childRows;
            public int position;
            boolean total;
            boolean collapsed = false;
            boolean rSelected = false;


        public AnalysisTableRow(Context context, int labelTxtColor, int positiveColor) {
            super(context);
            mContext = context;
            this.labelTxtColor = labelTxtColor;
            this.positiveColor = positiveColor;
            setOrientation(LinearLayout.HORIZONTAL);

            this.setClickable(true);
            this.setFocusable(false);


            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Dp40);
            setLayoutParams(params);

            plusMinusImg=new AppCompatImageView(context);
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(Dp20, Dp20);
            params1.gravity = Gravity.CENTER_VERTICAL;
            plusMinusImg.setLayoutParams(params1);

            label = new TextView(context);
            label.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Dp100, LinearLayout.LayoutParams.MATCH_PARENT);
            label.setLayoutParams(lp);
            label.setTextColor(labelTxtColor);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setSingleLine(true);

            labelHolder = new LinearLayout(context);
            LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(Dp130, LinearLayout.LayoutParams.MATCH_PARENT);
            labelHolder.setLayoutParams(lp1);
            labelHolder.setBackground(ContextCompat.getDrawable(context, R.drawable.custom_ripple));
            labelHolder.addView(plusMinusImg);
            labelHolder.addView(label);
            labelHolder.setOrientation(LinearLayout.HORIZONTAL);
            labelHolder.setClickable(true);
            labelHolder.setFocusable(true);
            labelHolder.setOnClickListener(this);

            plan = getNumberCell(context);
            fact= getNumberCell(context);
            delta= getNumberCell(context);
            childRows = new ArrayList<>();

            addView(labelHolder);
            addView(plan);
            addView(fact);
            addView(delta);

            setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mAnalysisTableRowClick.longRowClick(position);
                    return true;
                }
            });
        }

        @Override
        public boolean performClick() {
            if(rSelected){
                unselect();
                mAnalysisTableRowClick.rowSelected(-1);
            }else {
                select();
                mAnalysisTableRowClick.rowSelected(position);
            }
            return super.performClick();
        }

        /*@Override
        public boolean performLongClick() {
            mAnalysisTableRowClick.longRowClick(position);
            return super.performLongClick();
        }*/

        private void select() {
            setSelectedBkg();
            rSelected = true;
        }

        public void unselect() {
            setDefaultBkg();
            rSelected = false;
        }

        private TextView getNumberCell(Context context){
            TextView tv = new TextView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Dp40, 1f);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            tv.setPadding(0, 0, Dp5, 0);
            tv.setClickable(false);
            tv.setFocusable(false);
            return tv;
        }

        public void updateLabel(int position, String name, String nick, boolean aggregate, boolean total){
            this.position = position;
            this.total = total;
            if(aggregate){
                labelHolder.setEnabled(true);
                labelHolder.setClickable(true);
                labelHolder.setFocusable(true);
                LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(Dp20, Dp20);
                params1.gravity = Gravity.CENTER_VERTICAL;
                plusMinusImg.setLayoutParams(params1);
                plusMinusImg.setVisibility(View.VISIBLE);
                plusMinusImg.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.analysis_icon_plus));
            }else {
                labelHolder.setEnabled(false);
                labelHolder.setClickable(false);
                labelHolder.setFocusable(false);
                LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(Dp10, Dp20);
                params1.gravity = Gravity.CENTER_VERTICAL;
                plusMinusImg.setLayoutParams(params1);
                plusMinusImg.setVisibility(View.INVISIBLE);
            }
            String finalName;
            int textSize;
            int paddingLeft;
            if(total){
                textSize = 15;
                paddingLeft = 0;
                setVisibility(View.VISIBLE);
                if(getTotalLabelWidth(name)>Dp100){
                    finalName = nick;
                }else {
                    finalName = name;
                }
            }else {
                textSize = 14;
                paddingLeft = Dp20;
                setVisibility(View.GONE);
                if(getSmallLabelWidth(name) > Dp100 - Dp20) {
                    finalName = nick;
                }else {
                    finalName = name;
                }
            }
            setDefaultBkg();
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            plan.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            fact.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            delta.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            label.setPadding(paddingLeft,0,0,0);
            label.setText(finalName);
            childRows.clear();
            collapsed = false;
            rSelected = false;
        }

        private void setDefaultBkg(){
            if(total&&position!=0){
                setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple_transparent_border_top));
                getBackground().jumpToCurrentState();
            }else {
                setBackground(ContextCompat.getDrawable(mContext, R.drawable.custom_ripple));
                getBackground().jumpToCurrentState();
            }
        }
        private void setSelectedBkg(){
            if(total&&position!=0){
                setBackground(ContextCompat.getDrawable(mContext, R.drawable.analysis_selected_row));
            }else {
                setBackgroundColor(selectedRowColor);
            }
        }

        public void updatePlan(String planLabel, boolean positive){
            plan.setText(planLabel);
            if(positive){
                plan.setTextColor(positiveColor);
            }else {
                plan.setTextColor(labelTxtColor);
            }
        }

        public void updateFact(String factLabel, boolean positive){
            fact.setText(factLabel);
            if(positive){
                fact.setTextColor(positiveColor);
            }else {
                fact.setTextColor(labelTxtColor);
            }
        }

        public void updateDelta(String deltaAbsLabel, String deltaPercentLabel, boolean positive){
            absDelta = deltaAbsLabel;
            percentDelta = deltaPercentLabel;
            if(positive){
                delta.setTextColor(positiveColor);
            }else {
                delta.setTextColor(labelTxtColor);
            }
        }

        public void setDelta(boolean abs){
            if(abs){
                delta.setText(absDelta);
            }else {
                delta.setText(percentDelta);
            }
        }

        public void addChild(AnalysisTableRow childRow){
            childRows.add(childRow);
        }

        @Override
        public void onClick(View view) {
            if(collapsed){
                hideChildRows();
                collapsed = false;
                plusMinusImg.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.analysis_icon_plus));
            }else {
                showChildRows();
                collapsed = true;
                plusMinusImg.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.analysis_icon_minus));
            }
        }

        private void showChildRows(){
            for(int i = 0; i < childRows.size(); i++){
                childRows.get(i).setVisibility(View.VISIBLE);
            }
        }
        private void hideChildRows(){
            for(int i = 0; i < childRows.size(); i++){
                if(childRows.get(i).rSelected){
                    childRows.get(i).unselect();
                    mAnalysisTableRowClick.rowSelected(-1);
                }
                childRows.get(i).setVisibility(View.GONE);
            }
        }
    }
}

