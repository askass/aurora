package tech.aurorafin.aurora.export;


import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import tech.aurorafin.aurora.CategoriesFragment;
import tech.aurorafin.aurora.CategoryFilter;
import tech.aurorafin.aurora.CategoryFilterAdapter;
import tech.aurorafin.aurora.DateFormater;
import tech.aurorafin.aurora.PlanData;
import tech.aurorafin.aurora.R;
import tech.aurorafin.aurora.dbRoom.CashFlowDB;
import tech.aurorafin.aurora.dbRoom.CategoriesRepository;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ExportActivity extends AppCompatActivity implements View.OnClickListener,
        DatePickerDialog.OnDateSetListener, CategoriesRepository.CategoriesUpdateCallback,
        CategoryFilterAdapter.CategoryFilterCallback {

    AppCompatImageButton export_back_btn;
    TextView export_btn;
    private final static String EXPORT_WORK_TAG = "EXPORT_WORK_TAG";


    /*Date range*/
    TextView date_from, date_to;
    int dateCodeFrom;
    int dateCodeTo;
    DatePickerDialog datePickerDialog;
    boolean datePickerFrom = true;
    int formatCode;

    /*Categories*/
    CategoriesRepository categoriesRepository;
    TextView categories_text_label, export_categories;
    AlertDialog selectCategoryDialog;
    CategoryFilter categoryFilter;
    CheckBox selectAllCB;
    int Dp300;
    HashMap<Long, Boolean> appliedSelectedCategories;
    long[] categoriesIds;
    int[] categoriesTypes;
    long[] aggregateIds;
    String[] categoriesNames;
    String[] aggregateNames;

    /*Group*/
    RadioButton day_radio_btn, month_radio_btn, year_radio_btn, no_group_radio_btn;
    RadioButton [] groupIds;

    /*Tables*/
    TextView tables_label;
    CheckBox plan_table_cb, fact_table_cb;

    /*Delim*/
    RadioButton comma_radio_btn;

    AlertDialog alertDialog;
    WorkInfo.State currentState;
    Observer<List<WorkInfo>> mObserver;
    LiveData<List<WorkInfo>> mWorkInfos;
    private static final int BTN_ENABLED = 100;
    private static final int BTN_EXPORTING = 102;
    ProgressBar export_progress_bar;
    int blueRowColor;

    /*Validators*/
    CategoriesFragment.NewValidatorAnimator labelCategoryValidatorAnim;
    CategoriesFragment.NewValidatorAnimator selectorCategoryValidatorAnim;

    CategoriesFragment.NewValidatorAnimator labelTableValidatorAnim;
    CategoriesFragment.NewValidatorAnimator cbPlanValidatorAnim;
    CategoriesFragment.NewValidatorAnimator cbFactValidatorAnim;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        export_back_btn= findViewById(R.id.export_back_btn);
        export_back_btn.setOnClickListener(this);

        /*Date range*/
            date_from = findViewById(R.id.date_from);
            date_from.setOnClickListener(this);
            date_to = findViewById(R.id.date_to);
            date_to.setOnClickListener(this);
            formatCode = DateFormater.getDateFormatKey(this);
            Calendar init = Calendar.getInstance();
            int year = init.get(Calendar.YEAR);
            int yearDateCodeFrom = PlanData.getDayCode(year, 0,1);
            int yearDateCodeTo = PlanData.getDayCode(year, 11,31);
            updateDateRange(yearDateCodeFrom, yearDateCodeTo);

        /*Categories*/
            ThreadPoolExecutor mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            categoriesRepository = new CategoriesRepository(this, mExecutor, new Handler(Looper.getMainLooper()));
            categoriesRepository.setCategoriesUpdateCallback(this);
            float density = getResources().getDisplayMetrics().density;
            Dp300 = (int)(300f * density +0.5f);
            categoryFilter = new CategoryFilter(new ContextThemeWrapper(this, R.style.ScrollbarRecyclerView), true, categoriesRepository.categories, this);
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT);
            categoryFilter.setLayoutParams(layoutParams);
            categoryFilter.setScrollbarFadingEnabled(false);
            LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.category_filter_dialog, null);
            FrameLayout recycler_holder = ll.findViewById(R.id.recycler_holder);
            recycler_holder.addView(categoryFilter);
            selectAllCB = ll.findViewById(R.id.analysis_category_select_all);
            selectAllCB.setOnClickListener(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            export_categories =  findViewById(R.id.export_categories);
            export_categories.setOnClickListener(this);
            categories_text_label =  findViewById(R.id.categories_text_label);

        /*Group*/
            day_radio_btn =  findViewById(R.id.day_radio_btn);
            month_radio_btn =  findViewById(R.id.month_radio_btn);
            year_radio_btn =  findViewById(R.id.year_radio_btn);
            no_group_radio_btn =  findViewById(R.id.no_group_radio_btn);
            day_radio_btn.setOnClickListener(this);
            month_radio_btn.setOnClickListener(this);
            year_radio_btn.setOnClickListener(this);
            no_group_radio_btn.setOnClickListener(this);
            groupIds = new RadioButton[4];
            groupIds[0] = no_group_radio_btn;
            groupIds[1] = day_radio_btn;
            groupIds[2] = month_radio_btn;
            groupIds[3] = year_radio_btn;


        /*Tables*/
            tables_label = findViewById(R.id.tables_label);
            plan_table_cb = findViewById(R.id.plan_table_cb);
            fact_table_cb = findViewById(R.id.fact_table_cb);
        /*Delim*/
        comma_radio_btn = findViewById(R.id.comma_radio_btn);

        /*Validators*/
            int txtTableColor = ContextCompat.getColor(this, R.color.table_txt_color);
            int txtRedColor = ContextCompat.getColor(this, R.color.red_txt_color);
            int txtHintColor = ContextCompat.getColor(this, R.color.grey_txt_color);

            labelCategoryValidatorAnim= new CategoriesFragment.NewValidatorAnimator(categories_text_label, "TextColor", false, txtRedColor, txtTableColor);
            selectorCategoryValidatorAnim= new CategoriesFragment.NewValidatorAnimator(export_categories, "TextColor", false, txtRedColor, txtHintColor);

            labelTableValidatorAnim= new CategoriesFragment.NewValidatorAnimator(tables_label, "TextColor", false, txtRedColor, txtTableColor);
            cbPlanValidatorAnim= new CategoriesFragment.NewValidatorAnimator(plan_table_cb, "TextColor", false, txtRedColor, txtTableColor);
            cbFactValidatorAnim= new CategoriesFragment.NewValidatorAnimator(fact_table_cb, "TextColor", false, txtRedColor, txtTableColor);

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setPositiveButton(R.string.continue1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                startQuery();

            }
        });
        alertBuilder.setNegativeButton(R.string.cancel1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        alertBuilder.setTitle(R.string.warning);
        alertBuilder.setMessage(R.string.warning_msg);
        alertDialog = alertBuilder.create();


        export_btn = findViewById(R.id.export_btn);
        export_btn.setOnClickListener(this);
        export_progress_bar = findViewById(R.id.export_progress_bar);
        blueRowColor = ContextCompat.getColor(this, R.color.blue_row);
        currentState = WorkInfo.State.SUCCEEDED;

        mObserver = new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                if(workInfos.size()>0){
                    WorkInfo.State state = workInfos.get(workInfos.size()-1).getState();
                    if(state != currentState){
                        if(state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED){
                            exportBtnStateUpdate(BTN_EXPORTING);
                        }else {
                            exportBtnStateUpdate(BTN_ENABLED);
                        }
                        currentState = state;
                    }
                }else {
                    exportBtnStateUpdate(BTN_ENABLED);
                    currentState = WorkInfo.State.SUCCEEDED;
                }
            }
        };

        resetObserver();

    }

    @Override
    public void onClick(View view) {

        int id = view.getId();
        switch (id) {
            case R.id.export_back_btn:
                onBackPressed();
                break;
            case R.id.export_btn:
                startExport();
                break;
            case R.id.date_from:
                showDatePicker(true);
                break;
            case R.id.date_to:
                showDatePicker(false);
                break;
            case R.id.export_categories:
                showCategoryFilter();
                break;
            case R.id.analysis_category_select_all:
                categoryFilter.categoryFilterAdapter.setAllSelected(selectAllCB.isChecked());
                break;
            case R.id.day_radio_btn:
            case R.id.month_radio_btn:
            case R.id.year_radio_btn:
            case R.id.no_group_radio_btn:
                updateGroupChecks(id);
                break;

        }
    }


    private void exportBtnStateUpdate(int state){
        switch (state) {
            case BTN_ENABLED:
                export_btn.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_btn_ripple));
                export_btn.setTextColor(getResources().getColorStateList(R.color.pressed_txt_color));
                export_progress_bar.setVisibility(View.GONE);
                export_btn.setEnabled(true);
                break;
            case BTN_EXPORTING:
                export_btn.setBackground(ContextCompat.getDrawable(this, R.drawable.connected_btn));
                export_btn.setTextColor(blueRowColor);
                export_progress_bar.setVisibility(View.VISIBLE);
                export_btn.setEnabled(false);
                break;
        }
    }

    private void resetObserver() {
        if(mWorkInfos != null){
            mWorkInfos.removeObserver(mObserver);
        }
        mWorkInfos = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(EXPORT_WORK_TAG);
        mWorkInfos.observe(this, mObserver);
    }

    private void startExport() {
        boolean valid = true;
        if(appliedSelectedCategories.size() == 0){
            labelCategoryValidatorAnim.playValidatorAnim();
            selectorCategoryValidatorAnim.playValidatorAnim();
            valid = false;
        }

        if(!plan_table_cb.isChecked()&&!fact_table_cb.isChecked()){
            labelTableValidatorAnim.playValidatorAnim();
            cbPlanValidatorAnim.playValidatorAnim();
            cbFactValidatorAnim.playValidatorAnim();
            valid = false;
        }
        if(valid) {
            if (CashFlowDB.isLastDbCloseCorrect(this)) {
                startQuery();
            } else {
                alertDialog.show();
            }
        }
    }

    private void startQuery() {
        updateCategoriesIdsArrays();
        Data data = new Data.Builder()
                .putInt(ExportWorker.DC_FROM, dateCodeFrom)
                .putInt(ExportWorker.DC_TO, dateCodeTo)
                .putInt(ExportWorker.GROUP_BY, getGroupCode())
                .putBoolean(ExportWorker.PLAN_TABLE, plan_table_cb.isChecked())
                .putBoolean(ExportWorker.FACT_TABLE, fact_table_cb.isChecked())
                .putLongArray(ExportWorker.CAT_IDS, categoriesIds)
                .putIntArray(ExportWorker.CAT_TYPES, categoriesTypes)
                .putStringArray(ExportWorker.CAT_NAMES, categoriesNames)
                .putLongArray(ExportWorker.AGGREGATE_IDS, aggregateIds)
                .putStringArray(ExportWorker.AGGREGATE_NAMES, aggregateNames)
                .putBoolean(ExportWorker.COMMA, comma_radio_btn.isChecked())
                .build();
        OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(ExportWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(EXPORT_WORK_TAG, ExistingWorkPolicy.REPLACE, uploadRequest);
        resetObserver();
    }

    private void updateCategoriesIdsArrays() {
        int size = appliedSelectedCategories.size();
        categoriesIds = new long[size];
        categoriesTypes = new int[size];
        aggregateIds = new long[size];
        categoriesNames = new String[size];
        aggregateNames  = new String[size];

        int index = 0;

        long currentAggregateId = 0;
        String currentAggregateName = "";

        for(int i = 0; i < categoriesRepository.categories.size();i++){
            int type = categoriesRepository.categories.get(i).type;
            long id = categoriesRepository.categories.get(i).id;

            if(type == CategoriesRepository.ACategory.AGGREGATOR
                ||type == CategoriesRepository.ACategory.EMPTY_AGGREGATOR){
                currentAggregateId = id;
                currentAggregateName = categoriesRepository.categories.get(i).name;
            }else {
                if(appliedSelectedCategories.containsKey(id)){
                    categoriesIds[index] = id;
                    categoriesTypes[index] = type;
                    aggregateIds[index] = currentAggregateId;
                    categoriesNames[index] = categoriesRepository.categories.get(i).name;
                    aggregateNames[index] = currentAggregateName;
                    index++;
                }
            }
        }



    }

    private int getGroupCode(){
        for(int i = 0; i < groupIds.length; i++){
            if(groupIds[i].isChecked()){
                return i;
            }
        }
        return 1;
    }


    private void updateGroupChecks(int id){
        for(int i = 0; i < groupIds.length; i++){
            if(groupIds[i].getId() != id){
                groupIds[i].setChecked(false);
            }
        }
    }


    /*DatePickerDialog.OnDateSetListener*/
    @Override
    public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
        int dateCode = PlanData.getDayCode(year, monthOfYear, dayOfMonth);
        if(datePickerFrom){
            updateDateRange(dateCode, dateCodeTo);
        }else {
            updateDateRange(dateCodeFrom, dateCode);
        }

    }

    private void updateDateRange(int dateCodeFrom, int dateCodeTo){
        this.dateCodeFrom = dateCodeFrom;
        this.dateCodeTo = dateCodeTo;
        date_from.setText(DateFormater.getDateFromDateCode(this.dateCodeFrom,formatCode));
        date_to.setText(DateFormater.getDateFromDateCode(this.dateCodeTo,formatCode));
    }

    private void showDatePicker(boolean datePickerFrom){
        this.datePickerFrom = datePickerFrom;
        int yearFrom = dateCodeFrom / 10000;
        int monthFrom = (dateCodeFrom % 10000) / 100 - 1;
        int dayFrom = dateCodeFrom % 100;
        int yearTo = dateCodeTo / 10000;
        int monthTo = (dateCodeTo % 10000) / 100 - 1;
        int dayTo = dateCodeTo % 100;
        if(datePickerFrom){
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, yearTo);
            c.set(Calendar.MONTH, monthTo);
            c.set(Calendar.DAY_OF_MONTH, dayTo);
            datePickerDialog = new DatePickerDialog(this, this, yearFrom, monthFrom, dayFrom);
            datePickerDialog.getDatePicker().setMaxDate(c.getTimeInMillis());
        }else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, yearFrom);
            c.set(Calendar.MONTH, monthFrom);
            c.set(Calendar.DAY_OF_MONTH, dayFrom);
            datePickerDialog = new DatePickerDialog(this, this, yearTo, monthTo, dayTo);
            datePickerDialog.getDatePicker().setMinDate(c.getTimeInMillis());
        }
        datePickerDialog.show();
    }



    /*CategoriesRepository.CategoriesUpdateCallback*/
    @Override
    public void CategoriesUpdated() {
        categoryFilter.categoryFilterAdapter.notifyDataSetChanged();
        selectAllCB.setChecked(true);
        categoryFilter.categoryFilterAdapter.setAllSelected(true);
        appliedSelectedCategories = new HashMap<>(categoryFilter.categoryFilterAdapter.selectedCategories);
    }

    @Override
    public void CategoryUpdated(long id, int index, boolean locked) {

    }

    /*CategoryFilterAdapter.CategoryFilterCallback*/
    @Override
    public void CategoryClicked(int position, int type, long id) {
        updateAllSelectCheckBox();
    }

    @Override
    public boolean isCategoryLocked(long id) {
        return false;
    }

    private void applyCategoryFilterUpdate(){
        if(!isKeySetsEquals(appliedSelectedCategories, categoryFilter.categoryFilterAdapter.selectedCategories)){
            appliedSelectedCategories.clear();
            appliedSelectedCategories.putAll(categoryFilter.categoryFilterAdapter.selectedCategories);
            if(updateAllSelectCheckBox()){
                export_categories.setText(R.string.all);
            }else {
                export_categories.setText(R.string.filtered);
            }
        }
    }

    private boolean isKeySetsEquals(HashMap<Long, Boolean> applied, HashMap<Long, Boolean> newOne){
        return applied.keySet().equals(newOne.keySet());
    }
    private boolean updateAllSelectCheckBox(){
        boolean allSelected = categoriesRepository.mapIdCategory.size() == categoryFilter.categoryFilterAdapter.selectedCategories.size();
        selectAllCB.setChecked(allSelected);
        selectAllCB.jumpDrawablesToCurrentState();
        return allSelected;
    }

    private void showCategoryFilter(){
        categoryFilter.categoryFilterAdapter.restoreLastAppliedSelectedMap(appliedSelectedCategories);
        updateAllSelectCheckBox();
        selectCategoryDialog.show();
        selectCategoryDialog.getWindow().setLayout(Dp300, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
