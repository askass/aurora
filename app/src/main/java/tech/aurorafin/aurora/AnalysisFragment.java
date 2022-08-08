package tech.aurorafin.aurora;


import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import tech.aurorafin.aurora.dbRoom.AnalysisRepository;
import tech.aurorafin.aurora.dbRoom.CategoriesRepository;
import tech.aurorafin.aurora.dbRoom.OperationRepository;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

public class AnalysisFragment extends Fragment implements DateChip.ChipUpdates, DatePickerDialog.OnDateSetListener,
        View.OnClickListener, CategoryFilterAdapter.CategoryFilterCallback, CategoriesRepository.CategoriesUpdateCallback,
        CompoundButton.OnCheckedChangeListener, WfChip.ChipUpdates, AnalysisVPool.AnalysisTableRowClick,
        OnChartValueSelectedListener {

    Context mContext;
    CategoriesRepository mCategoriesRepository;
    AnalysisRepository mAnalysisRepository;
    ScreenLocker analysis_screen_locker;

    /*Type Filter*/
        private AppCompatToggleButton toggle_exp;
        private AppCompatToggleButton toggle_rev;
        private AppCompatToggleButton toggle_cap;

    /*Date Range*/
        TextView analysis_date_from, analysis_date_to;
        int analysisDateCodeFrom;
        int analysisDateCodeTo;
        DatePickerDialog datePickerDialog;
        boolean datePickerFrom = true;
        long defaultMaxDate;
        long defaultMinDate;
        int formatCode;

    /*Date Chips*/
        LinearLayout date_chips_layout1;
        LinearLayout date_chips_layout2;
        String[] mMonths;
        String[] shortMonths;
        String sYear;
        String sWeek;
        DateChip[] dateChips;
        DateChip weekChip;
        DateChip monthChip;
        DateChip quarterChip;
        DateChip halfChip;
        DateChip yearChip;
        DateChip ytdChip;
        int[] quarterMap = new int[]{0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3};
        int[] quarterMmDdFrom = new int[]{101, 401, 701, 1001};
        int[] quarterMmDdTo = new int[]{331, 630, 930, 1231};

    /*Categories*/
        AppCompatImageButton cancel_filter_btn;
        TextView analysis_categories;
        AlertDialog selectCategoryDialog;
        CategoryFilter categoryFilter;
        CheckBox selectAllCB;
        int Dp300;
        HashMap<Long, Boolean> appliedSelectedCategories;
        Switch aggregate_switch;

    /*Data set*/
        List<AnalysisRepository.AnalysisDataStruct> AnalysisDataSet;
        long PLAN_TOTAL;
        long FACT_TOTAL;
        String deltaTotal;
        String deltaTotalPercent;
        int deltaTotalColor;
        long multiplier;
        boolean REV;
        boolean EXP;
        boolean CAP;
        long MAX_VAL;
        long MIN_VAL;
        long LABEL_DIVIDER;
        int LABEL_DECIMAL;

    /*Water Fall*/
        LinearLayout wf_plan_bar;
        LinearLayout wf_fact_bar;
        LinearLayout wf_categories_labels;
        LinearLayout wf_categories_bars;
        View wf_zero_line;
        float baseX;
        WfChip planChip;
        WfChip factChip;
        AnalysisVPool viewsPool;
        WfBar planBar;
        WfBar factBar;
        float wfLabelWidth = 0;
        float dataSetWidth;
        float addition;
        float specAddition;
        List<float[]> wfCoordinates = new ArrayList<>();
        List<String> wfBarLabels = new ArrayList<>();
        TextView wf_scale_label;

    /*Table*/
        RadioButton abs_radio_btn, percent_radio_btn;
        LinearLayout table_container, table_total_row;
        TextView table_plan, table_fact, table_delta, table_scale_label;
        int selectedRow;

    /*Chart*/
        boolean chartHighlight;
        TextView chart_category_label, chart_plan_label, chart_fact_label, chart_date_label, chart_plan_sum, chart_fact_sum;
        TextView chart_x_label1, chart_x_label2, chart_x_label3, chart_scale_label;
        private LineChart line_chart;
        XAxis xAxis; YAxis leftAxis; YAxis rightAxis;
        LineDataSet setPlan, setFact, setFactStart, setFactEnd;
        ArrayList<Entry> planYVals;
        ArrayList<Entry> factYVals;
        ArrayList<Entry> factStartVal, factEndVal;
        Switch cumulative_switch;
        CheckBox chart_plan_cb, chart_fact_cb;
        int primaryColor, blueRowColor, borderRowColor, greyLineColor, greyTxtColor;
        float Dp1f, Dp3f, Dp14f;
        ScrollView analysis_scrollview;
        float dY, dX;
        boolean actionMade = false;

        ArrayList<String> xLabel;
        ArrayList<Integer> xSpans;
        List<Long> planChartData;
        List<Long> factNonCumData;
        List<Long> factChartData;
        int planStartIndex;
        int planEndIndex;
        int factStartIndex;
        int factEndIndex;

        long chartPlanMax;
        long chartPlanMin;
        long chartFactMax;
        long chartFactMin;
        long CHART_DIVIDER;
        int CHART_DECIMAL;
        long chartMax;
        long chartMin;

    /*DATA UPDATER*/
        Future<?> updaterMainFuture;
        Handler analysisHandler;
        Runnable updaterEnterRunnable;
        Runnable updaterMainRunnable;
        Runnable updaterFinishRunnable;
        Runnable lockerRunnable;

    /*MAIN ACTIVITY COMMUNICATOR*/
    MainActivityCommunication mMainActivityCommunication;

    public AnalysisFragment(Context context, CategoriesRepository categoriesRepository, AnalysisRepository analysisRepository, MainActivityCommunication mainActivityCommunication) {
        this.mContext = context;
        this.mCategoriesRepository = categoriesRepository;
        this.mAnalysisRepository = analysisRepository;
        this.mMainActivityCommunication = mainActivityCommunication;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_analysis, container, false);
        analysis_screen_locker = view.findViewById(R.id.analysis_screen_locker);
        AnalysisDataSet = new ArrayList<>();
        mCategoriesRepository.setCategoriesUpdateCallback(this);

        /*Type Filter*/
            toggle_exp = view.findViewById(R.id.toggle_exp);
            toggle_rev = view.findViewById(R.id.toggle_rev);
            toggle_cap = view.findViewById(R.id.toggle_cap);
            toggle_exp.setOnClickListener(this);
            toggle_rev.setOnClickListener(this);
            toggle_cap.setOnClickListener(this);

        /*Date Range*/
            analysis_date_from = view.findViewById(R.id.analysis_date_from);
            analysis_date_from.setOnClickListener(this);
            analysis_date_to = view.findViewById(R.id.analysis_date_to);
            analysis_date_to.setOnClickListener(this);
            //datePickerDialog = new DatePickerDialog(mContext, this, 1990, 0, 1);
            formatCode = DateFormater.getDateFormatKey(mContext);

        /*Date chips*/
            date_chips_layout1 = view.findViewById(R.id.date_chips_layout1);
            date_chips_layout2 = view.findViewById(R.id.date_chips_layout2);
            mMonths = mContext.getResources().getStringArray(R.array.months);
            shortMonths = mContext.getResources().getStringArray(R.array.short_months);
            sWeek = mContext.getResources().getString(R.string.week);
            sYear = mContext.getResources().getString(R.string.year1);
            layoutDateChips();
            dateChips = new DateChip[6];
            dateChips[0] = weekChip;
            dateChips[1] = monthChip;
            dateChips[2] = quarterChip;
            dateChips[3] = halfChip;
            dateChips[4] = yearChip;
            dateChips[5] = ytdChip;

        /*Categories*/
            float density = mContext.getResources().getDisplayMetrics().density;
            Dp300 = (int)(300f * density +0.5f);
            categoryFilter = new CategoryFilter(new ContextThemeWrapper(mContext, R.style.ScrollbarRecyclerView), true, mCategoriesRepository.categories, this);
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT);
            categoryFilter.setLayoutParams(layoutParams);
            categoryFilter.setScrollbarFadingEnabled(false);
            LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.category_filter_dialog, null);
            FrameLayout recycler_holder = ll.findViewById(R.id.recycler_holder);
            recycler_holder.addView(categoryFilter);
            selectAllCB = ll.findViewById(R.id.analysis_category_select_all);
            selectAllCB.setOnClickListener(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setView(ll);
            builder.setNegativeButton(R.string.cancel1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    applyCategoryFilterUpdate();
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                }
            });
            selectCategoryDialog = builder.create();
            analysis_categories = view.findViewById(R.id.analysis_categories);
            analysis_categories.setOnClickListener(this);
            cancel_filter_btn = view.findViewById(R.id.cancel_filter_btn);
            cancel_filter_btn.setEnabled(false);
            cancel_filter_btn.setOnClickListener(this);


        /*Init Filter Settings*/
            toggle_rev.setChecked(mAnalysisRepository.REV);
            toggle_exp.setChecked(mAnalysisRepository.EXP);
            toggle_cap.setChecked(mAnalysisRepository.CAP);
            analysisDateCodeFrom = -1;
            analysisDateCodeTo = -1;
            if(mAnalysisRepository.analysisDateCodeFrom != -1 && mAnalysisRepository.analysisDateCodeTo != -1){
                updateAnalysisDateRange(mAnalysisRepository.analysisDateCodeFrom, mAnalysisRepository.analysisDateCodeTo, false);
                checkChipsDateRangeMatch();
            }else {
                //check if an interval has been already selected
                if(mAnalysisRepository.analysisDateChip != -1){
                    int chipInd = mAnalysisRepository.analysisDateChip;
                    updateAnalysisDateRange(dateChips[chipInd].dateCodeFrom, dateChips[chipInd].dateCodeTo, false);
                    dateChips[chipInd].setChecked();
                }else {
                    updateAnalysisDateRange(weekChip.dateCodeFrom, weekChip.dateCodeTo, false);
                    weekChip.setChecked();
                }

            }
            categoriesFilterInitializer();
            aggregate_switch = view.findViewById(R.id.aggregate_switch);
            aggregate_switch.setChecked(mAnalysisRepository.aggregate);
            aggregate_switch.setOnCheckedChangeListener(this);

        /*Water Fall*/
            viewsPool = new AnalysisVPool(mContext, this);
            wf_plan_bar = view.findViewById(R.id.wf_plan_bar);
            wf_fact_bar = view.findViewById(R.id.wf_fact_bar);
            wf_categories_labels = view.findViewById(R.id.wf_categories_labels);
            wf_categories_bars = view.findViewById(R.id.wf_categories_bars);
            wf_zero_line = view.findViewById(R.id.wf_zero_line);
            baseX = wf_zero_line.getTranslationX();

            planChip = new WfChip(mContext, this, getString(R.string.plan), true);
            factChip = new WfChip(mContext, this, getString(R.string.fact), false);
            wf_plan_bar.addView(planChip);
            wf_fact_bar.addView(factChip);
            planChip.setChecked(mAnalysisRepository.planChip);
            factChip.setChecked(mAnalysisRepository.factChip);
            planBar = viewsPool.getNewWfBar();
            wf_plan_bar.addView(planBar);
            factBar = viewsPool.getNewWfBar();
            wf_fact_bar.addView(factBar);
            wf_scale_label = view.findViewById(R.id.wf_scale_label);

        /*Table*/
            abs_radio_btn = view.findViewById(R.id.abs_radio_btn);
            percent_radio_btn = view.findViewById(R.id.percent_radio_btn);
            abs_radio_btn.setChecked(mAnalysisRepository.abs);
            percent_radio_btn.setChecked(!mAnalysisRepository.abs);
            abs_radio_btn.setOnClickListener(this);
            percent_radio_btn.setOnClickListener(this);
            table_container = view.findViewById(R.id.table_container);
            table_plan = view.findViewById(R.id.table_plan);
            table_fact = view.findViewById(R.id.table_fact);
            table_delta = view.findViewById(R.id.table_delta);
            table_scale_label = view.findViewById(R.id.table_scale_label);
            table_total_row = view.findViewById(R.id.table_total_row);
            table_total_row.setOnClickListener(this);

        /*Chart*/
            chartHighlight = false;
            chart_category_label = view.findViewById(R.id.chart_category_label);
            chart_plan_label = view.findViewById(R.id.chart_plan_label);
            chart_fact_label = view.findViewById(R.id.chart_fact_label);
            chart_date_label = view.findViewById(R.id.chart_date_label);
            chart_plan_sum = view.findViewById(R.id.chart_plan_sum);
            chart_fact_sum = view.findViewById(R.id.chart_fact_sum);
            chart_scale_label = view.findViewById(R.id.chart_scale_label);

            primaryColor = ContextCompat.getColor(mContext, R.color.colorPrimary);
            blueRowColor = ContextCompat.getColor(mContext, R.color.blue_row);
            borderRowColor = ContextCompat.getColor(mContext, R.color.border_row);
            greyLineColor = ContextCompat.getColor(mContext, R.color.grey_line);
            greyTxtColor = ContextCompat.getColor(mContext, R.color.grey_txt_color);

            Dp1f = (1f * density);
            Dp3f = (3f * density);
            Dp14f= (14f * density);
            selectedRow = -1;
            analysis_scrollview = view.findViewById(R.id.analysis_scrollview);
            cumulative_switch = view.findViewById(R.id.cumulative_switch);
            cumulative_switch.setChecked(mAnalysisRepository.cumulative);
            cumulative_switch.setOnCheckedChangeListener(this);
            chart_plan_cb = view.findViewById(R.id.chart_plan_cb);
            chart_plan_cb.setOnClickListener(this);
            chart_fact_cb = view.findViewById(R.id.chart_fact_cb);
            chart_fact_cb.setOnClickListener(this);

            chart_plan_cb.setChecked(mAnalysisRepository.planChart);
            chart_fact_cb.setChecked(mAnalysisRepository.factChart);
            chart_x_label1 = view.findViewById(R.id.chart_x_label1);
            chart_x_label2 = view.findViewById(R.id.chart_x_label2);
            chart_x_label3 = view.findViewById(R.id.chart_x_label3);

            xSpans = new ArrayList<>();
            planChartData = new ArrayList<>();
            factChartData = new ArrayList<>();
            factNonCumData = new ArrayList<>();

            line_chart = view.findViewById(R.id.line_chart);
            line_chart.setTouchEnabled(true);
            line_chart.setPinchZoom(false);
            line_chart.setScaleEnabled(false);
            line_chart.getDescription().setEnabled(false);
            line_chart.getLegend().setEnabled(false);
            line_chart.setNoDataTextColor(primaryColor);
            line_chart.setExtraOffsets(0, 0, 0, 1);
            IMarker marker = new ChartMarker(mContext);
            line_chart.setMarker(marker);
            line_chart.setOnChartValueSelectedListener(this);
            line_chart.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            analysis_scrollview.requestDisallowInterceptTouchEvent(true);
                            dY = event.getRawY();
                            dX = event.getRawX();
                            break;
                        }
                        case MotionEvent.ACTION_MOVE:
                            float y =  event.getRawY();
                            float x =  event.getRawX();
                            if(!actionMade){
                                if(x!=dX || y!=dY ){
                                    if(Math.abs(x-dX)>Math.abs(y-dY)){
                                        analysis_scrollview.requestDisallowInterceptTouchEvent(true);
                                        actionMade = true;
                                    }else {
                                        analysis_scrollview.requestDisallowInterceptTouchEvent(false);
                                        line_chart.setTouchEnabled(false);
                                        actionMade = true;
                                    }
                                }
                            }
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            actionMade = false;
                            line_chart.setTouchEnabled(true);
                        case MotionEvent.ACTION_UP:
                            actionMade = false;
                            line_chart.setTouchEnabled(true);
                            analysis_scrollview.requestDisallowInterceptTouchEvent(false);
                            break;
                    }
                    return false;
                }
            });

            xAxis =  line_chart.getXAxis();
            xAxis.setDrawAxisLine(true);
            xAxis.setDrawGridLines(false);
            xAxis.setTextColor(greyTxtColor);
            xAxis.setTextSize(14f);
            xAxis.setAxisLineWidth(1f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setAxisLineColor(greyLineColor);
            xAxis.setDrawLabels(false);
            xLabel = new ArrayList<>();

            leftAxis =  line_chart.getAxisLeft();
            leftAxis.removeAllLimitLines();
            leftAxis.setLabelCount(4);
            leftAxis.setTextColor(greyTxtColor);
            leftAxis.setTextSize(14f);
            leftAxis.setValueFormatter(new LongFormater.MyValueFormatter());
            leftAxis.setAxisLineWidth(Dp1f);
            leftAxis.setAxisLineColor(greyLineColor);
            leftAxis.setDrawZeroLine(true);
            leftAxis.setDrawAxisLine(false);
            leftAxis.setDrawGridLines(true);
            leftAxis.enableGridDashedLine(Dp3f, Dp3f, 0);
            leftAxis.setGridColor(borderRowColor);

            rightAxis = line_chart.getAxisRight();
            rightAxis.setDrawAxisLine(false);
            rightAxis.setDrawGridLines(false);
            rightAxis.setDrawLabels(false);

            planYVals = new ArrayList<>();
            setPlan = new LineDataSet(planYVals, "plan");
            setPlan.setDrawIcons(false);
            setPlan.setCircleRadius(3f);
            setPlan.setCircleColor(blueRowColor);
            setPlan.setDrawCircles(false);
            setPlan.setDrawCircleHole(true);
            setPlan.setDrawValues(false);
            setPlan.setLineWidth(1f);
            setPlan.setColor(blueRowColor);
            setPlan.setDrawHorizontalHighlightIndicator(false);
            setPlan.setHighlightLineWidth(1f);
            setPlan.setHighLightColor(greyLineColor);
            setPlan.setDrawFilled(true);
            /*//setPlan.setFormLineWidth(1f);
            //setPlan.setFormSize(15.f);*/
            Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.plan_chart_fill);
            setPlan.setFillDrawable(drawable);
            setPlan.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

            factYVals = new ArrayList<>();
            setFact = new LineDataSet(factYVals, "fact");
            setFact.setDrawIcons(false);
            setFact.setCircleRadius(3f);
            setFact.setCircleColor(primaryColor);
            setFact.setDrawCircles(true);
            setFact.setDrawCircleHole(false);
            setFact.setDrawValues(false);
            setFact.setLineWidth(2f);
            setFact.setColor(primaryColor);
            setFact.setDrawHorizontalHighlightIndicator(false);
            setFact.setHighlightLineWidth(1f);
            setFact.setHighLightColor(greyLineColor);
            setFact.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
            //setFact.setDrawCubic(true);
            factStartVal = new ArrayList<>();
            setFactStart = new LineDataSet(factStartVal, "factStartEnd");
            setFactStart.setDrawIcons(false);
            setFactStart.setCircleRadius(3f);
            setFactStart.setCircleColor(primaryColor);
            setFactStart.setDrawCircles(true);
            setFactStart.setDrawCircleHole(false);
            setFactStart.setDrawValues(false);
            setFactStart.setLineWidth(2f);
            setFactStart.setColor(primaryColor);
            setFactStart.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
            setFactStart.setHighlightEnabled(false);

            factEndVal = new ArrayList<>();
            setFactEnd = new LineDataSet(factEndVal, "factStartEnd");
            setFactEnd.setDrawIcons(false);
            setFactEnd.setCircleRadius(3f);
            setFactEnd.setCircleColor(primaryColor);
            setFactEnd.setDrawCircles(true);
            setFactEnd.setDrawCircleHole(false);
            setFactEnd.setDrawValues(false);
            setFactEnd.setLineWidth(2f);
            setFactEnd.setColor(primaryColor);
            setFactEnd.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
            setFactEnd.setHighlightEnabled(false);

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(setPlan);
            dataSets.add(setFact);
            dataSets.add(setFactStart);
            dataSets.add(setFactEnd);
            LineData data = new LineData(dataSets);
            line_chart.setData(data);

        /*DATA UPDATER*/
        analysisHandler = new Handler(Looper.getMainLooper());

        updaterEnterRunnable = new Runnable() {
            @Override
            public void run() {
                analysis_screen_locker.setVisibility(View.VISIBLE);
                analysisHandler.postDelayed(lockerRunnable, 200);
                mAnalysisRepository.mExecutor.execute(updaterMainRunnable);
            }
        };

        updaterMainRunnable = new Runnable() {
            @Override
            public void run() {
                mAnalysisRepository.startUpdater(analysisDateCodeFrom, analysisDateCodeTo, appliedSelectedCategories);
                analysisHandler.post(updaterFinishRunnable);
            }
        };

        updaterFinishRunnable = new Runnable() {
            @Override
            public void run() {
                analysisHandler.removeCallbacks(lockerRunnable);
                analysis_screen_locker.unlockScreen();
                buildDataSet();
            }
        };

        lockerRunnable = new Runnable() {
            @Override
            public void run() {
                analysis_screen_locker.lockScreen();
            }
        };


        if(mCategoriesRepository.mapIdLockedSize() == 0){
            analysisHandler.post(updaterEnterRunnable);
        }else {
            analysis_screen_locker.lockScreen();
        }

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveFilterState();
        analysisHandler.removeCallbacks(updaterEnterRunnable);
        analysisHandler.removeCallbacks(updaterFinishRunnable);
        mAnalysisRepository.mExecutor.remove(updaterMainRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveFilterState();
    }

    private void saveFilterState(){
        mAnalysisRepository.REV = toggle_rev.isChecked();
        mAnalysisRepository.EXP = toggle_exp.isChecked();
        mAnalysisRepository.CAP = toggle_cap.isChecked();
        mAnalysisRepository.analysisDateCodeFrom = this.analysisDateCodeFrom;
        mAnalysisRepository.analysisDateCodeTo = this.analysisDateCodeTo;
        mAnalysisRepository.selectAll = selectAllCB.isChecked();
        mAnalysisRepository.setAppliedSelectedCategories(appliedSelectedCategories);
        mAnalysisRepository.aggregate = aggregate_switch.isChecked();
        mAnalysisRepository.planChip = planChip.isChecked();
        mAnalysisRepository.factChip = factChip.isChecked();
        mAnalysisRepository.abs = abs_radio_btn.isChecked();
        mAnalysisRepository.cumulative = cumulative_switch.isChecked();
        mAnalysisRepository.planChart = chart_plan_cb.isChecked();
        mAnalysisRepository.factChart = chart_fact_cb.isChecked();
        mAnalysisRepository.valid = false;
        mAnalysisRepository.analysisDateChip = getCheckedChipIndex();
        mAnalysisRepository.saveLastFilterState();
    }

    private void categoriesFilterInitializer(){
        if(!mAnalysisRepository.selectAll && isRestoredMapEnable()){
            appliedSelectedCategories = new HashMap<>(mAnalysisRepository.appliedSelectedCategories);
            boolean allSelected = mCategoriesRepository.mapIdCategory.size() == appliedSelectedCategories.size();
            selectAllCB.setChecked(allSelected);
            selectAllCB.jumpDrawablesToCurrentState();
            if(allSelected){
                cancel_filter_btn.setEnabled(false);
                analysis_categories.setText(R.string.all);
            }else {
                cancel_filter_btn.setEnabled(true);
                analysis_categories.setText(R.string.filtered);
            }
        }else {
            selectAllCB.setChecked(true);
            categoryFilter.categoryFilterAdapter.setAllSelected(true);
            appliedSelectedCategories = new HashMap<>(categoryFilter.categoryFilterAdapter.selectedCategories);
        }
    }

    private boolean isRestoredMapEnable(){
        boolean enabled = true;
        if(mAnalysisRepository.appliedSelectedCategories != null){
            for (Long key : mAnalysisRepository.appliedSelectedCategories.keySet()) {
                if(!mCategoriesRepository.mapIdCategory.containsKey(key)){
                    enabled = false;
                    break;
                }
            }
        }else {
            enabled = false;
        }
        return enabled;
    }

    private void updateAnalysisDateRange(int dateCodeFrom, int dateCodeTo, boolean runUpdater){
        if(this.analysisDateCodeFrom != dateCodeFrom || this.analysisDateCodeTo != dateCodeTo ){
            this.analysisDateCodeFrom = dateCodeFrom;
            this.analysisDateCodeTo = dateCodeTo;
            analysis_date_from.setText(DateFormater.getDateFromDateCode(this.analysisDateCodeFrom, formatCode));
            analysis_date_to.setText(DateFormater.getDateFromDateCode(this.analysisDateCodeTo, formatCode));
            if(runUpdater){
                runUpdater(100);
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.toggle_exp:
                toggleBtnClick();
                break;
            case R.id.toggle_rev:
                toggleBtnClick();
                break;
            case R.id.toggle_cap:
                toggleBtnClick();
                break;
            case R.id.analysis_date_from:
                showDatePicker(true);
                break;
            case R.id.analysis_date_to:
                showDatePicker(false);
                break;
            case R.id.analysis_categories:
                showCategoryFilter();
                break;
            case R.id.analysis_category_select_all:
                categoryFilter.categoryFilterAdapter.setAllSelected(selectAllCB.isChecked());
                break;
            case R.id.cancel_filter_btn:
                categoryFilter.categoryFilterAdapter.setAllSelected(true);
                applyCategoryFilterUpdate();
                break;
            case R.id.abs_radio_btn:
                updateTableDeltaLabels(abs_radio_btn.isChecked());
                break;
            case R.id.percent_radio_btn:
                updateTableDeltaLabels(abs_radio_btn.isChecked());
                break;
            case R.id.table_total_row:
                tableSelectionUpdater(-1);
                break;
            case R.id.cumulative_switch:
                buildChartDataSet();
                break;
            case R.id.chart_plan_cb:
                finishChartUpdate();
                break;
            case R.id.chart_fact_cb:
                finishChartUpdate();
                break;

        }
    }

    private void buildDataSet(){
        AnalysisDataSet.clear();
        PLAN_TOTAL = 0;
        FACT_TOTAL = 0;
        MAX_VAL = 0;
        MIN_VAL = 0;
        REV = toggle_rev.isChecked();
        EXP = toggle_exp.isChecked();
        CAP = toggle_cap.isChecked();
        if(!REV){
            multiplier = 1;
        }else {
            multiplier = -1;
        }
        if(aggregate_switch.isChecked()){
            buildAggregatedDataSet();
        }else {
            buildCategorizedDataSet();
        }

        if(PLAN_TOTAL > MAX_VAL){MAX_VAL = PLAN_TOTAL;}if(FACT_TOTAL > MAX_VAL){MAX_VAL = FACT_TOTAL;}
        if(PLAN_TOTAL < MIN_VAL){MIN_VAL = PLAN_TOTAL;}if(FACT_TOTAL < MIN_VAL){MIN_VAL = FACT_TOTAL;}

        LABEL_DIVIDER = LongFormater.getDivider(MAX_VAL, MIN_VAL);
        LABEL_DECIMAL = LongFormater.getDecimals(LABEL_DIVIDER, MAX_VAL, MIN_VAL);

        wf_scale_label.setText(getScaleLabel(LABEL_DIVIDER));
        table_scale_label.setText(getScaleLabel(LABEL_DIVIDER));

        buildWaterFall();
        buildTable();

        /*CHART*/
        selectedRow = -1;
        buildChartLabels();
        buildChartDataSet();

    }

    private void buildChartDataSet() {
        planChartData.clear();
        factChartData.clear();
        factNonCumData.clear();

        chartPlanMax = 0;
        chartPlanMin = 0;

        chartFactMax = 0;
        chartFactMin = 0;

        boolean cumulative  = cumulative_switch.isChecked();
        if(selectedRow == -1){
            buildRangeChart(cumulative, 0, AnalysisDataSet.size());
        }else if(AnalysisDataSet.get(selectedRow).aggregate) {
            buildAggregateChart(cumulative);
        }else {
            buildRangeChart(cumulative, selectedRow, selectedRow+1);
        }

        if(planEndIndex !=-1 && factEndIndex !=-1){
            chartMax = Math.max(chartPlanMax, chartFactMax);
            chartMin = Math.min(chartPlanMin, chartFactMin);
        }else if(planEndIndex !=-1){
            chartMax = chartPlanMax;
            chartMin = chartPlanMin;
        }else {
            chartMax = chartFactMax;
            chartMin = chartFactMin;
        }

        CHART_DIVIDER = LongFormater.getDivider(chartMax, chartMin);
        CHART_DECIMAL = LongFormater.getDecimals(CHART_DIVIDER, chartMax, chartMin);
        setChartScaleAndTitle();
        updateChart();
    }

    private void setChartScaleAndTitle() {
        if(selectedRow == -1){
            chart_category_label.setText(mContext.getString(R.string.total));
        }else {
            String name = AnalysisDataSet.get(selectedRow).categoryName;
            if(viewsPool.getTotalLabelWidth(name) > viewsPool.Dp130 - viewsPool.Dp30){
                name = AnalysisDataSet.get(selectedRow).categoryNick;
            }
            chart_category_label.setText(name);
        }
        chart_scale_label.setText(getScaleLabel(CHART_DIVIDER));
    }

    private void updateChart() {
        stopHighlighting();
        planYVals.clear();
        factYVals.clear();
        factStartVal.clear();
        factEndVal.clear();

        for(int i = 0; i < xSpans.size(); i++){
            if(i >= planStartIndex && i <= planEndIndex){
                long planY = planChartData.get(i);
                planYVals.add(new Entry(xSpans.get(i), LongFormater.getChartFloat(CHART_DIVIDER, planY), i));
            }
            if(i >= factStartIndex && i <= factEndIndex){
                long factY = factChartData.get(i);
                factYVals.add(new Entry(xSpans.get(i), LongFormater.getChartFloat(CHART_DIVIDER, factY), i));
            }
        }

        setPlan.setValues(planYVals);
        setFact.setValues(factYVals);

        if(factYVals.size() > 1){
            factStartVal.add(factYVals.get(0));
            factEndVal.add(factYVals.get(factYVals.size()-1));
        }
        setFactStart.setValues(factStartVal);
        setFactEnd.setValues(factEndVal);

        finishChartUpdate();
    }

    private void finishChartUpdate() {
        boolean planChart = chart_plan_cb.isChecked();
        boolean factChart = chart_fact_cb.isChecked();
        float yMax;
        float yMin;
        if(xSpans.size()>0){
            float xMax = xSpans.get(xSpans.size()-1);
            float xMin = xSpans.get(0);;
            if(!planChart&&factChart){ //Only Fact
                yMax = LongFormater.getChartFloat(CHART_DIVIDER,chartFactMax);
                yMin = LongFormater.getChartFloat(CHART_DIVIDER, chartFactMin);
                setPlan.setVisible(false);
                setPlan.setHighlightEnabled(false);
                setFact.setVisible(true);
                setFactStart.setVisible(true);
                setFactEnd.setVisible(true);
                setFact.setHighlightEnabled(true);
            }else if(planChart&&!factChart){//Only Plan
                yMax = LongFormater.getChartFloat(CHART_DIVIDER,chartPlanMax);
                yMin = LongFormater.getChartFloat(CHART_DIVIDER, chartPlanMin);
                setPlan.setVisible(true);
                setPlan.setHighlightEnabled(true);
                setFact.setVisible(false);
                setFactStart.setVisible(false);
                setFactEnd.setVisible(false);
                setFact.setHighlightEnabled(false);
            }
            else if (planChart&&factChart) {
                yMax = LongFormater.getChartFloat(CHART_DIVIDER, chartMax);
                yMin = LongFormater.getChartFloat(CHART_DIVIDER, chartMin);
                setPlan.setVisible(true);
                setFact.setVisible(true);
                setFactStart.setVisible(true);
                setFactEnd.setVisible(true);
                setPlan.setHighlightEnabled(true);
                setFact.setHighlightEnabled(true);
            }else {
                yMax = 0;
                yMin = 0;
                setPlan.setVisible(false);
                setFact.setVisible(false);
                setFactStart.setVisible(false);
                setFactEnd.setVisible(false);
                setPlan.setHighlightEnabled(false);
                setFact.setHighlightEnabled(false);
                stopHighlighting();
            }

            setPlan.setDrawCircles(planYVals.size() == 1);
            setFact.setDrawCircles(factYVals.size() == 1);

            if(yMin > 0){
                yMin =  yMin - yMin*0.1f;
            }else if(yMin < 0){
                yMin = yMin + yMin*0.1f;
            }

            if(yMax > 0){
                yMax =  yMax + yMax*0.1f;
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            }else if(yMax < 0){
                yMax = yMax - yMax*0.1f;
                xAxis.setPosition(XAxis.XAxisPosition.TOP);
            }


            leftAxis.setAxisMaximum(yMax);
            leftAxis.setAxisMinimum(yMin);

            xAxis.setAxisMaximum(xMax*1.02f);
            xAxis.setAxisMinimum(xMin-xMax*0.02f);

            line_chart.getData().notifyDataChanged();
            line_chart.notifyDataSetChanged();
            line_chart.invalidate();
        }

    }

    private void buildAggregateChart(boolean cumulative) {
        int to = AnalysisDataSet.size();
        for (int i = selectedRow+1; i < AnalysisDataSet.size(); i++){
            if(AnalysisDataSet.get(i).total){
                to = i;
                break;
            }
        }

        buildRangeChart(cumulative, selectedRow + 1, to);
    }

    private void buildRangeChart(boolean cumulative, int from, int to) {
        List<AnalysisRepository.AnalysisTotal>  tempDataMap = mAnalysisRepository.tempDataMap;
        long sumPlan = 0;
        long sumFact = 0;

        planStartIndex = -1;
        planEndIndex = -1;
        factStartIndex = -1;
        factEndIndex = -1;

        for (int i = 0; i < tempDataMap.size(); i++){
            long tempPlan = 0;
            long tempFact = 0;
            for (int a = from; a < to; a++){
                long multiplier = AnalysisDataSet.get(a).multiplier;
                tempPlan = tempPlan + AnalysisDataSet.get(a).planList[i]*multiplier;
                tempFact = tempFact + AnalysisDataSet.get(a).factList[i]*multiplier;
            }
            long tempPlanAdd;
            long tempFactAdd;
            if(cumulative){
                sumPlan = sumPlan + tempPlan;
                sumFact = sumFact + tempFact;
                tempPlanAdd = sumPlan;
                tempFactAdd = sumFact;
            }else {
                tempPlanAdd = tempPlan;
                tempFactAdd = tempFact;
            }
            planChartData.add(tempPlanAdd);
            factChartData.add(tempFactAdd);
            factNonCumData.add(tempFact);

            if(tempPlan != 0 && planStartIndex == -1){
                planStartIndex = i;
            }
            if(tempFact != 0 && factStartIndex == -1){
                factStartIndex = i;
            }

        }

        for(int i = factNonCumData.size()-1; i>=0;i--){
            long tempPlan = planChartData.get(i);
            long tempFact = factNonCumData.get(i);
            long tempFactAdd = factChartData.get(i);

            if(tempPlan != 0 && planEndIndex == -1){
                planEndIndex = i;
                chartPlanMin = tempPlan;
                chartPlanMax = tempPlan;
            }
            if(planEndIndex != -1 && i >=planStartIndex){
                if(tempPlan>chartPlanMax){chartPlanMax=tempPlan;}
                if(tempPlan<chartPlanMin){chartPlanMin=tempPlan;}
            }
            if(tempFact != 0 && factEndIndex == -1){
                factEndIndex = i;
                chartFactMin = tempFactAdd;
                chartFactMax = tempFactAdd;
            }
            if(factEndIndex != -1 && i >=factStartIndex){
                if(tempFactAdd>chartFactMax){chartFactMax=tempFactAdd;}
                if(tempFactAdd<chartFactMin){chartFactMin=tempFactAdd;}
            }
        }
    }

    private void buildChartLabels() {
        xLabel.clear();
        xSpans.clear();
        int spanTotal = 0;
        List<AnalysisRepository.AnalysisTotal>  tempDataMap = mAnalysisRepository.tempDataMap;
        for (int i = 0; i < tempDataMap.size(); i++){
            int type = tempDataMap.get(i).getType();
            int dateCodeTo = tempDataMap.get(i).getDateCodeTo();
            String label;
            if(type == AnalysisRepository.AnalysisTotal.DAY_TYPE){
                if(tempDataMap.get(i).isSequential() || i != 0){
                    label = DateFormater.getDateFromDateCode(dateCodeTo, formatCode);
                }else {
                    label = DateFormater.getDateFromDateCode(tempDataMap.get(i).getDateCodeFrom(), formatCode);
                }

            }else if(type == AnalysisRepository.AnalysisTotal.MONTH_TYPE){
                int monthTo = (dateCodeTo % 10000) / 100 - 1;
                int yearTo = dateCodeTo / 10000;
                label = shortMonths[monthTo] + " " + Integer.toString(yearTo);
            }else {
                int yearTo = dateCodeTo / 10000;
                label = sYear + " " + Integer.toString(yearTo);
            }
            xLabel.add(label);

            spanTotal = spanTotal + tempDataMap.get(i).getSpanSize();
            xSpans.add(spanTotal);
        }


        int lSize = xLabel.size();
        if(lSize>0){
            chart_x_label1.setText(xLabel.get(0));
        }
        if(lSize>2){
            chart_x_label2.setText(xLabel.get((int)(lSize/2)));
        }else {
            chart_x_label2.setText("");
        }

        if(lSize>1) {
            chart_x_label3.setText(xLabel.get(lSize - 1));
        }
    }

    private void updateTableDeltaLabels(boolean abs){
        for (int i = 0; i < table_container.getChildCount(); i++) {
            ((AnalysisVPool.AnalysisTableRow)table_container.getChildAt(i)).setDelta(abs);
        }
        updateTableTotalDelta(abs_radio_btn.isChecked());
    }

    private void setTableTotalDelta(){
        long TOTAL_DELTA = FACT_TOTAL - PLAN_TOTAL;
        boolean positive;
        if(multiplier < 0){
            positive = TOTAL_DELTA > 0;
        }else {
            positive = TOTAL_DELTA < 0;
        }

        if(positive){
            table_delta.setTextColor(viewsPool.positiveColor);
        }else {
            table_delta.setTextColor(viewsPool.labelTxtColor);
        }

        deltaTotal = LongFormater.formatLong(TOTAL_DELTA, LABEL_DIVIDER, LABEL_DECIMAL);
        deltaTotalPercent =  LongFormater.getPercentDelta(TOTAL_DELTA, PLAN_TOTAL, FACT_TOTAL, false);
        updateTableTotalDelta(abs_radio_btn.isChecked());
    }

    private void updateTableTotalDelta(boolean abs){
        if(abs){
            table_delta.setText(deltaTotal);
        }else {
            table_delta.setText(deltaTotalPercent);
        }
    }

    private void buildTable() {
        for (int i = 0; i < table_container.getChildCount(); i++) {
            viewsPool.putTableRow((AnalysisVPool.AnalysisTableRow)table_container.getChildAt(i));
        }
        table_container.removeAllViews();
        AnalysisVPool.AnalysisTableRow tempAggregate = null;
        for (int i = 0; i < AnalysisDataSet.size(); i++){
            String name = AnalysisDataSet.get(i).categoryName;
            String nick = AnalysisDataSet.get(i).categoryNick;
            boolean aggregate = AnalysisDataSet.get(i).aggregate;
            boolean total = AnalysisDataSet.get(i).total;
            boolean positivePlan;
            boolean positiveFact;
            boolean positiveDelta;
            long planTotal = AnalysisDataSet.get(i).getMPlanTotal();
            long factTotal = AnalysisDataSet.get(i).getMFactTotal();
            long delta = factTotal-planTotal;
            if(multiplier < 0){
                positivePlan = planTotal > 0;
                positiveFact = factTotal > 0;
                positiveDelta = delta > 0;
            }else {
                positivePlan = planTotal < 0;
                positiveFact = factTotal < 0;
                positiveDelta = delta < 0;
            }

            AnalysisVPool.AnalysisTableRow tempRow = viewsPool.getTableRow();
            tempRow.updateLabel(i, name, nick, aggregate, total);
            tempRow.updatePlan(LongFormater.formatLong(planTotal, LABEL_DIVIDER, LABEL_DECIMAL), positivePlan);
            tempRow.updateFact(LongFormater.formatLong(factTotal, LABEL_DIVIDER, LABEL_DECIMAL), positiveFact);
            tempRow.updateDelta(LongFormater.formatLong(delta, LABEL_DIVIDER, LABEL_DECIMAL),
                                LongFormater.getPercentDelta(delta, planTotal, factTotal, false), positiveDelta);
            tempRow.setDelta(abs_radio_btn.isChecked());
            table_container.addView(tempRow);

            if(aggregate){
                tempAggregate = tempRow;
            }else if(!total && tempAggregate!=null){
                tempAggregate.addChild(tempRow);
            }
        }
        table_plan.setText(LongFormater.formatLong(PLAN_TOTAL, LABEL_DIVIDER, LABEL_DECIMAL));
        table_fact.setText(LongFormater.formatLong(FACT_TOTAL, LABEL_DIVIDER, LABEL_DECIMAL));
        setTableTotalDelta();
    }

    private String getScaleLabel(long divider) {
        if(divider == LongFormater.One){
           return  mContext.getString(R.string.no_scale);
        }else if(divider == LongFormater.Thousand){
            return mContext.getString(R.string.Thousand);
        }else if (divider == LongFormater.Million){
            return  mContext.getString(R.string.Million);
        }else if (divider == LongFormater.Billion){
            return  mContext.getString(R.string.Billion);
        }else {
            return  mContext.getString(R.string.Trillion);
        }
    }

    private void buildWaterFall() {
        for (int i = 0; i < wf_categories_labels.getChildCount(); i++) {
            viewsPool.putWfLabel((TextView)wf_categories_labels.getChildAt(i));
            viewsPool.putWfBar((WfBar)wf_categories_bars.getChildAt(i));
        }
        wf_categories_labels.removeAllViews();
        wf_categories_bars.removeAllViews();
        dataSetWidth = 0;
        addition = 0;
        specAddition = 0f;
        wfCoordinates.clear();
        wfBarLabels.clear();

        /*-------*/
        float wMax = viewsPool.getWfLabelWidth(LongFormater.formatLong(MAX_VAL, LABEL_DIVIDER, LABEL_DECIMAL)+"0");
        float wMin = viewsPool.getWfLabelWidth(LongFormater.formatLong(MIN_VAL, LABEL_DIVIDER, LABEL_DECIMAL)+"0");
        wfLabelWidth = Math.max(wMax, wMin);

        boolean plan = planChip.isChecked();
        boolean fact = factChip.isChecked();
        if(plan == fact) {
            fillWfCoordinatesPlanFact();
            planBar.setAlpha(1);
            factBar.setAlpha(1);
        }else if(plan){
            fillWfCoordinatesOnlyPlan();
            planBar.setAlpha(1);
            factBar.setAlpha(0);
        }else if(fact){
            fillWfCoordinatesOnlyFact();
            planBar.setAlpha(0);
            factBar.setAlpha(1);
        }

        int wfCoordinatesSize = wfCoordinates.size();
        int iteration = 0;
        for (int i = 0; i < AnalysisDataSet.size(); i++){
            if(AnalysisDataSet.get(i).total){

                String name = AnalysisDataSet.get(i).categoryName;
                TextView label = viewsPool.getWfLabel();
                if(viewsPool.getTotalLabelWidth(name) > viewsPool.Dp130 - viewsPool.Dp30){
                    name = AnalysisDataSet.get(i).categoryNick;
                }
                label.setText(name);
                wf_categories_labels.addView(label);
                if(wfCoordinatesSize>0){
                    float start = wfCoordinates.get(iteration)[0];
                    float end = wfCoordinates.get(iteration)[1];
                    WfBar wfBar = viewsPool.getWfBar();
                    boolean positive;
                    if(multiplier < 0){
                        positive = end > start;
                    }else {
                        positive = end < start;
                    }
                    wfBar.updateBar(dataSetWidth, start + addition + specAddition,
                                end + addition + specAddition, false, positive,
                                wfBarLabels.get(iteration), wfLabelWidth);
                    wf_categories_bars.addView(wfBar);
                }
                iteration++;
            }
        }
        if(wfCoordinatesSize>0){
            int planEnd = plan == fact? 0 : 1;
            planBar.updateBar(dataSetWidth, addition, wfCoordinates.get(0)[planEnd]+addition + specAddition,
                    true, false, LongFormater.formatLong(PLAN_TOTAL, LABEL_DIVIDER, LABEL_DECIMAL), wfLabelWidth);
            planBar.invalidate();
            factBar.updateBar(dataSetWidth, addition, wfCoordinates.get(wfCoordinates.size()-1)[1]+addition + specAddition,
                    true, false, LongFormater.formatLong(FACT_TOTAL, LABEL_DIVIDER, LABEL_DECIMAL), wfLabelWidth);
            factBar.invalidate();
        }else {
            planBar.setAlpha(0);
            factBar.setAlpha(0);
        }
        zeroLineUpdate(addition);
    }

    private void fillWfCoordinatesPlanFact() {
        float prevVal = PLAN_TOTAL;

        float max = PLAN_TOTAL<0 ? 0 : PLAN_TOTAL;
        float min = PLAN_TOTAL>0 ? 0 : PLAN_TOTAL;

        float localMin = max;
        float localMax = min;

        for (int i = 0; i < AnalysisDataSet.size(); i++) {
            if (AnalysisDataSet.get(i).total) {
                long fact=AnalysisDataSet.get(i).getMFactTotal();
                long plan=AnalysisDataSet.get(i).getMPlanTotal();
                float total = fact - plan + prevVal;
                if (total > max) {max = total;}
                if (total < min) {min = total;}
                if(total < localMin){localMin = total;}
                if(total > localMax){localMax = total;}
                wfCoordinates.add(new float[]{prevVal, total});
                wfBarLabels.add(LongFormater.formatLong(fact - plan, LABEL_DIVIDER, LABEL_DECIMAL));
                prevVal = total;
            }
        }
        if(PLAN_TOTAL>0 && localMin > 0 && localMin < max && localMin/max > 0.5){
            specAddition = - localMin + (max - localMin);
            dataSetWidth = max -min + specAddition;
        }else if(PLAN_TOTAL<0 && localMax < 0 &&localMax>min && localMax/min > 0.5){
            specAddition =  -localMax + (min - localMax);
            dataSetWidth = max -min - specAddition;
        }else {
            dataSetWidth = max -min;
        }
        addition = min < 0 ? -min-specAddition: 0;
    }

    private void fillWfCoordinatesOnlyPlan() {
        float prevVal = 0;
        float max = 0;
        float min = 0;
        for (int i = AnalysisDataSet.size()-1; i >= 0; i--) {
            if (AnalysisDataSet.get(i).total) {
                float planTotal = AnalysisDataSet.get(i).getMPlanTotal() + prevVal;
                if (planTotal > max) {max = planTotal;}
                if (planTotal < min) {min = planTotal;}
                wfCoordinates.add(new float[]{prevVal, planTotal});
                wfBarLabels.add(LongFormater.formatLong(AnalysisDataSet.get(i).getMPlanTotal(), LABEL_DIVIDER, LABEL_DECIMAL));
                prevVal = planTotal;
            }
        }
        Collections.reverse(wfCoordinates);
        Collections.reverse(wfBarLabels);
        dataSetWidth = max - min;
        addition = min < 0 ? -min : 0;
    }

    private void fillWfCoordinatesOnlyFact(){
        float prevVal = 0;
        float max = 0;
        float min = 0;
        for (int i = 0; i < AnalysisDataSet.size(); i++){
            if(AnalysisDataSet.get(i).total){
                float factTotal = AnalysisDataSet.get(i).getMFactTotal() + prevVal;
                if(factTotal>max){
                    max = factTotal;
                }
                if(factTotal < min){
                    min = factTotal;
                }
                wfCoordinates.add(new float[]{prevVal, factTotal});
                wfBarLabels.add(LongFormater.formatLong(AnalysisDataSet.get(i).getMFactTotal(), LABEL_DIVIDER, LABEL_DECIMAL));
                prevVal = factTotal;
            }
        }
        dataSetWidth = max - min;
        addition = min < 0 ? -min : 0;
    }

    private void zeroLineUpdate(final float addition){
        if(addition > 0){
            if(factBar.getWidth() > 0){
               wf_zero_line.setTranslationX(baseX + addition*(((float)factBar.getWidth()-wfLabelWidth)/dataSetWidth));
            }else {
                analysisHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(factBar.getWidth() > 0){
                            wf_zero_line.setTranslationX(baseX + addition*(((float)factBar.getWidth()-wfLabelWidth)/dataSetWidth));
                        }else {
                            analysisHandler.postDelayed(this, 100);
                        }
                    }
                },30);
            }
        }else {
            wf_zero_line.setTranslationX(baseX);
        }
    }

    private void buildCategorizedDataSet() {
        int ttlFactCount = 0;
        for(int i = 0; i < mCategoriesRepository.sortedCategories.size(); i++){

            int type = mCategoriesRepository.sortedCategories.get(i).type;
            long categoryId = mCategoriesRepository.sortedCategories.get(i).id;

            long localMultiplier = 1;
            boolean add = false;

            switch (type){
                case CategoriesRepository.ACategory.REVENUE:
                    add = REV;
                    localMultiplier = 1;
                    break;
                case CategoriesRepository.ACategory.EXPENSE:
                    add = EXP;
                    localMultiplier = multiplier;
                    break;
                case CategoriesRepository.ACategory.CAPITAL:
                    add = CAP;
                    localMultiplier = multiplier;
                    break;
            }

            if(add && appliedSelectedCategories.containsKey(categoryId)) {

                AnalysisRepository.AnalysisDataStruct temp = mAnalysisRepository.analysisDataMap.get(categoryId);
                ttlFactCount += temp != null ? temp.factCount : 0;
                if ((temp.factCount + temp.planCount) != 0) {
                    temp.categoryType = type;
                    temp.multiplier = localMultiplier;
                    temp.aggregate = false;
                    temp.categoryName = mCategoriesRepository.sortedCategories.get(i).name;
                    temp.categoryNick = mCategoriesRepository.sortedCategories.get(i).nick;
                    temp.total = true;

                    long plan = temp.getMPlanTotal();
                    long fact = temp.getMFactTotal();

                    PLAN_TOTAL = PLAN_TOTAL + plan;
                    FACT_TOTAL = FACT_TOTAL + fact;
                    AnalysisDataSet.add(temp);

                    if(plan > MAX_VAL){MAX_VAL = plan;}if(fact > MAX_VAL){MAX_VAL = fact;}
                    if(plan < MIN_VAL){MIN_VAL = plan;}if(fact < MIN_VAL){MIN_VAL = fact;}
                }
            }
        }
        sortAnalysisDataSetRevTop(ttlFactCount);
    }

    private void buildAggregatedDataSet() {

        long aggregateId;
        long aggregate_planTotal = 0;
        long aggregate_factTotal = 0;
        int aggregate_count = 0;

        int ttlFactCount = 0;

        for (int i = mCategoriesRepository.categories.size()-1; i >= 0; i--) {
            int type = mCategoriesRepository.categories.get(i).type;
            long categoryId = mCategoriesRepository.categories.get(i).id;
            long aggregatorId = mCategoriesRepository.categories.get(i).aggregatorId;

            long localMultiplier = 1;
            boolean add = false;

            switch (type){
                case CategoriesRepository.ACategory.EMPTY_AGGREGATOR:
                    aggregate_planTotal = 0;
                    aggregate_factTotal = 0;
                    aggregate_count = 0;
                    add = false;
                    break;
                case CategoriesRepository.ACategory.AGGREGATOR:
                    if(aggregate_count != 0){
                        AnalysisRepository.AnalysisDataStruct aggregator;
                        aggregator = new AnalysisRepository.AnalysisDataStruct(categoryId,
                                mAnalysisRepository.tempListInitializer(),mAnalysisRepository.tempListInitializer(),
                                aggregate_planTotal, aggregate_factTotal, aggregate_count, aggregate_count);
                        aggregator.categoryName = mCategoriesRepository.categories.get(i).name;
                        aggregator.categoryNick = mCategoriesRepository.categories.get(i).nick;
                        AnalysisDataSet.add(aggregator);
                        if(aggregate_planTotal > MAX_VAL){MAX_VAL = aggregate_planTotal;}if(aggregate_factTotal > MAX_VAL){MAX_VAL = aggregate_factTotal;}
                        if(aggregate_planTotal < MIN_VAL){MIN_VAL = aggregate_planTotal;}if(aggregate_factTotal < MIN_VAL){MIN_VAL = aggregate_factTotal;}
                    }
                    aggregate_planTotal = 0;
                    aggregate_factTotal = 0;
                    aggregate_count = 0;
                    add = false;
                    break;
                case CategoriesRepository.ACategory.REVENUE:
                    add = REV;
                    localMultiplier = 1;
                    break;
                case CategoriesRepository.ACategory.EXPENSE:
                    add = EXP;
                    localMultiplier = multiplier;
                    break;
                case CategoriesRepository.ACategory.CAPITAL:
                    add = CAP;
                    localMultiplier = multiplier;
                    break;
            }

            if(add && appliedSelectedCategories.containsKey(categoryId)){

                AnalysisRepository.AnalysisDataStruct temp = mAnalysisRepository.analysisDataMap.get(categoryId);
                ttlFactCount += temp != null ? temp.factCount : 0;
                if((temp.factCount + temp.planCount) != 0){
                    temp.categoryType = type;
                    temp.multiplier = localMultiplier;
                    temp.aggregate = false;
                    temp.categoryName = mCategoriesRepository.categories.get(i).name;
                    temp.categoryNick = mCategoriesRepository.categories.get(i).nick;

                    if(aggregatorId != mCategoriesRepository.aggregators.get(0).id){
                        temp.total = false;
                    }

                    long plan = temp.getMPlanTotal();
                    long fact = temp.getMFactTotal();

                    PLAN_TOTAL = PLAN_TOTAL + plan;
                    FACT_TOTAL = FACT_TOTAL + fact;

                    aggregate_planTotal = aggregate_planTotal + plan;
                    aggregate_factTotal = aggregate_factTotal + fact;
                    aggregate_count = aggregate_count + temp.factCount + temp.planCount;

                    AnalysisDataSet.add(temp);

                    if(plan > MAX_VAL){MAX_VAL = plan;}if(fact > MAX_VAL){MAX_VAL = fact;}
                    if(plan < MIN_VAL){MIN_VAL = plan;}if(fact < MIN_VAL){MIN_VAL = fact;}
                }
            }
        }

        Collections.reverse(AnalysisDataSet);

        sortAnalysisDataSetRevTop(ttlFactCount);

    }

    private void sortAnalysisDataSetRevTop(int ttlFactCount) {
        if(AnalysisDataSet.size() != 0){
            List<AnalysisRepository.AnalysisDataStruct> positiveAnalysisDataSet = new ArrayList<>();
            List<AnalysisRepository.AnalysisDataStruct> negativeAnalysisDataSet = new ArrayList<>();

            List<SorterMapping> positiveSorterMapping = new ArrayList<>();
            List<SorterMapping> negativeSorterMapping = new ArrayList<>();

            boolean toPositive = true;
            for (int i = 0; i < AnalysisDataSet.size(); i++){
               // Log.d("MyTag", AnalysisDataSet.get(i).categoryName);
                if(AnalysisDataSet.get(i).total){
                    long ttl;
                    if(ttlFactCount == 0){
                        ttl = AnalysisDataSet.get(i).getMPlanTotal();
                    }else {
                        ttl = AnalysisDataSet.get(i).getMFactTotal();
                    }

                    if(ttl > 0){
                        toPositive = true;
                        positiveAnalysisDataSet.add(AnalysisDataSet.get(i));
                        positiveSorterMapping.add(new SorterMapping(ttl,positiveAnalysisDataSet.size()-1));
                    }else {
                        toPositive = false;
                        negativeAnalysisDataSet.add(AnalysisDataSet.get(i));
                        negativeSorterMapping.add(new SorterMapping(ttl,negativeAnalysisDataSet.size()-1));
                    }
                }else {
                    if(toPositive){
                        positiveAnalysisDataSet.add(AnalysisDataSet.get(i));
                        if(positiveSorterMapping.size()!=0){
                            positiveSorterMapping.get(positiveSorterMapping.size()-1).addChild(positiveAnalysisDataSet.size()-1);
                        }
                    }else {
                        negativeAnalysisDataSet.add(AnalysisDataSet.get(i));
                        if(negativeSorterMapping.size()!=0){
                            negativeSorterMapping.get(negativeSorterMapping.size()-1).addChild(negativeAnalysisDataSet.size()-1);
                        }
                    }
                }
            }

            if(positiveAnalysisDataSet.size()!=0){
                positiveAnalysisDataSet = sortPart(positiveSorterMapping, positiveAnalysisDataSet);
            }
            if(negativeAnalysisDataSet.size()!=0){
                negativeAnalysisDataSet = sortPart(negativeSorterMapping, negativeAnalysisDataSet);
            }
            List<AnalysisRepository.AnalysisDataStruct> sortedAnalysisDataSet = new ArrayList<>(positiveAnalysisDataSet);
            sortedAnalysisDataSet.addAll(negativeAnalysisDataSet);
            AnalysisDataSet = sortedAnalysisDataSet;
        }
    }

    private List<AnalysisRepository.AnalysisDataStruct> sortPart( List<SorterMapping> sorterMapping, List<AnalysisRepository.AnalysisDataStruct> analysisDataSet){
        Collections.sort(sorterMapping, new Comparator<SorterMapping>() {
            @Override
            public int compare(SorterMapping t1, SorterMapping t2) {
                return Long.compare(t2.value,t1.value);
            }
        });
        List<AnalysisRepository.AnalysisDataStruct> sortedDataSet = new ArrayList<>();
        for(int i = 0; i < sorterMapping.size(); i++){
            int index = sorterMapping.get(i).index;
            sortedDataSet.add(analysisDataSet.get(index));
            if(sorterMapping.get(i).childes.size() != 0){
                for(int z = 0; z < sorterMapping.get(i).childes.size(); z++){
                    int childIndex = sorterMapping.get(i).childes.get(z);
                    sortedDataSet.add(analysisDataSet.get(childIndex));
                }
            }
        }
        return sortedDataSet;
    }

    public static class SorterMapping{
        public long value;
        public int index;
        public List<Integer> childes;

        public SorterMapping(long value, int index){
            this.value = Math.abs(value);
            this.index = index;
            childes = new ArrayList<>();
        }

        public void addChild(int index){
            childes.add(index);
        }
    }

    private void runUpdater(int delay){
        analysisHandler.removeCallbacks(updaterEnterRunnable);
        analysisHandler.postDelayed(updaterEnterRunnable, delay);
    }


    private void toggleBtnClick(){
        if((!toggle_exp.isChecked())&&
                (!toggle_rev.isChecked())&&
                    (!toggle_cap.isChecked())
        ){
            toggle_exp.setChecked(true);
            toggle_rev.setChecked(true);
            toggle_cap.setChecked(true);
        }
        buildDataSet();
       // applyCategoryFilter();
    }


    private boolean updateAllSelectCheckBox(){
        boolean allSelected = mCategoriesRepository.mapIdCategory.size() == categoryFilter.categoryFilterAdapter.selectedCategories.size();
        selectAllCB.setChecked(allSelected);
        selectAllCB.jumpDrawablesToCurrentState();
        return allSelected;
    }

    private void applyCategoryFilterUpdate(){
        if(!isKeySetsEquals(appliedSelectedCategories, categoryFilter.categoryFilterAdapter.selectedCategories)){
            appliedSelectedCategories.clear();
            appliedSelectedCategories.putAll(categoryFilter.categoryFilterAdapter.selectedCategories);
            if(updateAllSelectCheckBox()){
                cancel_filter_btn.setEnabled(false);
                analysis_categories.setText(R.string.all);
            }else {
                cancel_filter_btn.setEnabled(true);
                analysis_categories.setText(R.string.filtered);
            }
            runUpdater(100);
        }
    }

    private boolean isKeySetsEquals(HashMap<Long, Boolean> applied, HashMap<Long, Boolean> newOne){
        return applied.keySet().equals(newOne.keySet());
    }

    private void showCategoryFilter(){
        categoryFilter.categoryFilterAdapter.restoreLastAppliedSelectedMap(appliedSelectedCategories);
        updateAllSelectCheckBox();
        selectCategoryDialog.show();
        selectCategoryDialog.getWindow().setLayout(Dp300, ViewGroup.LayoutParams.WRAP_CONTENT);
    }


    private void showDatePicker(boolean datePickerFrom){
        this.datePickerFrom = datePickerFrom;
        int yearFrom = analysisDateCodeFrom / 10000;
        int monthFrom = (analysisDateCodeFrom % 10000) / 100 - 1;
        int dayFrom = analysisDateCodeFrom % 100;
        int yearTo = analysisDateCodeTo / 10000;
        int monthTo = (analysisDateCodeTo % 10000) / 100 - 1;
        int dayTo = analysisDateCodeTo % 100;
        if(datePickerFrom){
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, yearTo);
            c.set(Calendar.MONTH, monthTo);
            c.set(Calendar.DAY_OF_MONTH, dayTo);
            datePickerDialog = new DatePickerDialog(mContext, this, yearFrom, monthFrom, dayFrom);
            datePickerDialog.getDatePicker().setMaxDate(c.getTimeInMillis());
        }else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, yearFrom);
            c.set(Calendar.MONTH, monthFrom);
            c.set(Calendar.DAY_OF_MONTH, dayFrom);
            datePickerDialog = new DatePickerDialog(mContext, this, yearTo, monthTo, dayTo);
            datePickerDialog.getDatePicker().setMinDate(c.getTimeInMillis());
        }
        datePickerDialog.show();
    }


    /*DateChip.ChipUpdates*/
    @Override
    public void chipClicked(int dateCodeFrom, int dateCodeTo, int index) {
        updateChipsCheck(index);
        updateAnalysisDateRange(dateCodeFrom, dateCodeTo, true);
    }

    private void updateChipsCheck(int index){
        for(int i = 0; i < dateChips.length; i++){
            if(index == i){
                dateChips[i].setChecked();
            }else {
                dateChips[i].setDefault();
            }
        }
    }

    private int getCheckedChipIndex(){
        int ind = -1;
        for(int i = 0; i < dateChips.length; i++){
            if(dateChips[i].isChecked()){
                return i;
            }
        }
        return  ind;
    }

    private void layoutDateChips() {
        Calendar init = Calendar.getInstance();
        //init.setFirstDayOfWeek(Calendar.SUNDAY);
        int day = init.get(Calendar.DAY_OF_MONTH);
        int month = init.get(Calendar.MONTH);
        int year = init.get(Calendar.YEAR);

        int week = init.get(Calendar.WEEK_OF_YEAR);
        int yearToWeek;

        if (month == 0 && week >= 52){
            yearToWeek = year - 1;
        }else if(month == 11 && week == 1){
            yearToWeek = year + 1;
        }else {
            yearToWeek = year;
        }
        Calendar cal = (Calendar)init.clone();

        while (cal.get(Calendar.DAY_OF_WEEK) != cal.getFirstDayOfWeek()) {
            cal.add(Calendar.DATE, -1); // Substract 1 day until first day of week.
        }
        int currentWeekFrom = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        cal.add(Calendar.DATE, -1);
        int currentWeekTo = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        cal = (Calendar)init.clone();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        int currentMonthFrom = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        int currentMonthTo = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        String weekChipLabel = sWeek + " " + week;// + ", " + yearToWeek;
        weekChip = new DateChip(mContext, this, weekChipLabel, currentWeekFrom, currentWeekTo, 0);

        String monthChipLabel = mMonths[month] + " " + year;
        monthChip = new DateChip(mContext, this, monthChipLabel, currentMonthFrom, currentMonthTo, 1);

        String quarterChipLabel =  getString(R.string.q) +  Integer.toString(quarterMap[month]+1) + " " + year;
        int quarterDateCodeFrom = year * 10000 + quarterMmDdFrom[quarterMap[month]];
        int quarterDateCodeTo = year * 10000 + quarterMmDdTo[quarterMap[month]];
        quarterChip = new DateChip(mContext, this, quarterChipLabel, quarterDateCodeFrom, quarterDateCodeTo, 2);
        date_chips_layout1.addView(weekChip);
        date_chips_layout1.addView(monthChip);
        date_chips_layout1.addView(quarterChip);

        String halfYearChipLabel = month < 6 ? "1" + getString(R.string.h) + " " + year: "2" + getString(R.string.h) + " " + year;
        int halfYearDateCodeFrom = month < 6 ? year*10000 + quarterMmDdFrom[0] : year*10000 + quarterMmDdFrom[2];
        int halfYearDateCodeTo = month < 6 ? year*10000 + quarterMmDdTo[1] : year*10000 + quarterMmDdTo[3];
        halfChip = new DateChip(mContext, this, halfYearChipLabel, halfYearDateCodeFrom, halfYearDateCodeTo, 3);

        int yearDateCodeFrom = PlanData.getDayCode(year, 0,1);
        int yearDateCodeTo = PlanData.getDayCode(year, 11,31);
        yearChip = new DateChip(mContext, this, sYear + " " + year, yearDateCodeFrom, yearDateCodeTo, 4);

        ytdChip = new DateChip(mContext, this, getString(R.string.ytd), yearDateCodeFrom, PlanData.getDayCode(year, month,day), 5);

        date_chips_layout2.addView(halfChip);
        date_chips_layout2.addView(yearChip);
        date_chips_layout2.addView(ytdChip);
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
        int dateCode = PlanData.getDayCode(year, monthOfYear, dayOfMonth);
        if(datePickerFrom){
            updateAnalysisDateRange(dateCode, analysisDateCodeTo, true);
        }else {
            updateAnalysisDateRange(analysisDateCodeFrom, dateCode, true);
        }
        checkChipsDateRangeMatch();
    }

    private void checkChipsDateRangeMatch() {
        boolean def = false;
        for(int i = 0; i < dateChips.length; i++){
            if(dateChips[i].dateCodeFrom == analysisDateCodeFrom &&
                    dateChips[i].dateCodeTo == analysisDateCodeTo && !def){
                dateChips[i].setChecked();
                def = true;
            }else {
                dateChips[i].setDefault();
            }
        }
    }


    /*CategoryFilterAdapter.CategoryFilterCallback*/
    @Override
    public void CategoryClicked(int position, int type, long id) {
        updateAllSelectCheckBox();
    }

    @Override
    public boolean isCategoryLocked(long id) {
        return mCategoriesRepository.isCategoryLocked(id);

    }


    /*CategoriesRepository.CategoriesUpdateCallback */
    @Override
    public void CategoriesUpdated() {
        categoryFilter.categoryFilterAdapter.notifyDataSetChanged();
        categoriesFilterInitializer();
        analysisHandler.post(updaterEnterRunnable);
    }

    @Override
    public void CategoryUpdated(long id, int index, boolean locked) {
        if(mCategoriesRepository.mapIdLockedSize() != 0){
            analysis_screen_locker.lockScreen();
        }else {
            analysisHandler.post(updaterEnterRunnable);
        }
    }

    /* CompoundButton.OnCheckedChangeListener*/
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        int id = compoundButton.getId();
        switch (id) {

            case R.id.aggregate_switch:
                buildDataSet();
                break;
            case R.id.cumulative_switch:
                buildChartDataSet();
                break;

        }

    }

    /*WfChip.ChipUpdates*/
    @Override
    public void chipClicked(boolean plan) {
        /*if(planChip.isChecked() && factChip.isChecked()){
            if(plan){
                factChip.setChecked(false);
            }else {
                planChip.setChecked(false);
            }
        }*/
        buildWaterFall();
    }

    /*AnalysisVPool.AnalysisTableRowClick*/
    @Override
    public void rowSelected(int dataPosition) {
        tableSelectionUpdater(dataPosition);
        selectedRow = dataPosition;
    }
    @Override
    public void longRowClick(int dataPosition) {
        if(!AnalysisDataSet.get(dataPosition).aggregate){
            //makeTransition
            mMainActivityCommunication.transitionToOperations(AnalysisDataSet.get(dataPosition).categoryId, analysisDateCodeTo / 10000);
        }
    }

    private void tableSelectionUpdater(int dataPosition){
        for (int i = 0; i < table_container.getChildCount(); i++) {
            AnalysisVPool.AnalysisTableRow temp =  (AnalysisVPool.AnalysisTableRow)table_container.getChildAt(i);
            if(temp.position != dataPosition && temp.rSelected){
                temp.unselect();
            }
        }
        selectedRow = dataPosition;
        buildChartDataSet();
    }

    /*OnChartValueSelectedListener*/
    @Override
    public void onValueSelected(Entry e, Highlight h) {
        //Log.d("MyTag", "onValueSelected");
        int index = (int)e.getData();
        updateChartHighlightDetails(index);
    }

    @Override
    public void onNothingSelected() {
        //Log.d("MyTag", "onNothingSelected");
        stopHighlighting();
    }

    private void updateChartHighlightDetails(int index){
        if(cumulative_switch.isChecked()){
            if(index == 0 && !mAnalysisRepository.tempDataMap.get(0).isSequential()){
                String label2 = DateFormater.getDateFromDateCode(mAnalysisRepository.tempDataMap.get(0).getDateCodeTo(), formatCode);
                chart_date_label.setText(String.format("%s - %s", xLabel.get(0), label2));
            }else {
                chart_date_label.setText(String.format("%s - %s", xLabel.get(0), xLabel.get(index)));
            }
        }else {
            if(!mAnalysisRepository.tempDataMap.get(index).isSequential()){
               int dateCodeFrom = mAnalysisRepository.tempDataMap.get(index).getDateCodeFrom();
               int dateCodeTo = mAnalysisRepository.tempDataMap.get(index).getDateCodeTo();
               if(dateCodeFrom!=dateCodeTo){
                   String label1 = DateFormater.getDateFromDateCode(dateCodeFrom, formatCode);
                   String label2 = DateFormater.getDateFromDateCode(dateCodeTo, formatCode);
                   chart_date_label.setText(String.format("%s - %s", label1, label2));
               }else {
                 chart_date_label.setText(xLabel.get(index));
               }
            }else {
                chart_date_label.setText(xLabel.get(index));
            }

        }

        chart_plan_sum.setText(LongFormater.formatLong(planChartData.get(index), CHART_DIVIDER, CHART_DECIMAL));
        chart_fact_sum .setText(LongFormater.formatLong(factChartData.get(index), CHART_DIVIDER, CHART_DECIMAL));
        if(!chartHighlight){
            chart_plan_label.setAlpha(1);
            chart_fact_label.setAlpha(1);
            chart_date_label.setAlpha(1);
            chart_plan_sum.setAlpha(1);
            chart_fact_sum.setAlpha(1);
            chartHighlight = true;
        }
    }

    private void stopHighlighting(){
        line_chart.highlightValues(null);
        chart_date_label.setText("");
        chart_plan_sum.setText("");
        chart_fact_sum.setText("");
        chart_plan_label.setAlpha(0);
        chart_fact_label.setAlpha(0);
        chart_date_label.setAlpha(0);
        chart_plan_sum.setAlpha(0);
        chart_fact_sum.setAlpha(0);
        chartHighlight = false;
    }
}
