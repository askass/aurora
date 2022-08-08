package tech.aurorafin.aurora;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;



import tech.aurorafin.aurora.dbRoom.CategoriesRepository;
import tech.aurorafin.aurora.dbRoom.Operation;
import tech.aurorafin.aurora.dbRoom.OperationRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Future;


public class BalanceFragment extends Fragment implements LastOperationsAdapter.LastOperationCallback,
        CategoriesRepository.CategoriesUpdateCallback, View.OnClickListener, NewOperationFragment.NewOperationCallback,
        OperationRepository.OperationsUpdateCallBack, BalanceRow.BalanceRowCallback{

    Context mContext;
    CategoriesRepository mCategoriesRepository;
    OperationRepository mOperationRepository;
    MainActivityCommunication mMainActivityCommunication;
    OnBackPressedCallback callbackBtn;
    DbService mDbService;

    public void setmDbService(DbService mDbService) {
        this.mDbService = mDbService;
    }

    /*TOP BLUE BAR*/
    RadioButton rev_radio_btn, exp_radio_btn, cap_radio_btn;
    TextView last_7_sum, last_28_sum, last_90_sum;
    TextView last_7_vs_plan, last_28_vs_plan, last_90_vs_plan;
    TextView top_bar_scale_label;
    int todayDateCode, last7startDateCode, last28startDateCode, last90startDateCode;
    int currentYear;
    String vs_plan;
    LinearLayout last_7_group, last_28_group, last_90_group;

    /*LAST OPERATIONS*/
    RecyclerView last_operations_rv;
    MyLinearLayoutManager llm;
    LastOperationsAdapter lastOperationsAdapter;
    List<Operation> oldLastOperations;
    List<Operation> newLastOperations;
    OperationDiffUtilCallback operationDiffUtilCallback;

    /*NEW OPERATION*/
    FrameLayout new_operation_holder;
    NewOperationFragment newOperationFragment;
    TextView new_operation_btn;

    /*BALANCE*/
    List<BalanceRow> balanceRows;
    LinearLayout get_balance_btn;
    ImageView plus_minus_image;
    ProgressBar balance_progress_bar;
    LinearLayout balance_container, assets_container, equity_container;
    TextView total_assets_sum, total_equity_sum;
    View balance_line;
    ScrollView balance_scrollView;
    int selectedBalanceYear;
    ArrayList<Integer> spinnerYears;
    Spinner year_spinner;
    SpinnerIntegerAdapter yearSpinnerAdapter;
    int Dp2, Dp10,Dp40, Dp110;
    int greyTxtColor, blueColor, greyColor, greyTriangle;
    int sp15TextSize;
    Paint paint;
    boolean balanceCollapsed, needCollapseBalance;
        /*balance updater*/
        private Handler balanceHandler;
        Future<?> balanceUpdateFuture;
        Runnable lockerBalanceUpdater;
        Runnable mainBalanceRunnable;
        Runnable finishBalanceRunnable;
    ArrayList<DividerLabel> dividerLabels;
    int dividerIndex;
    Spinner scale_spinner;
    SpinnerScaleAdapter scaleSpinnerAdapter;

    /*ANIMATIONS*/
    boolean isAnimating = false;
    FrameLayout main_balance_view;

    CategoriesFragment.TransitionAnimator showNewOperationView;
    CategoriesFragment.TransitionAnimator hideNewOperationView;
    private AnimatorListenerAdapter afterNewOperationViewShown;
    private AnimatorListenerAdapter afterNewOperationViewHidden;


    public BalanceFragment(Context context,
                           CategoriesRepository categoriesRepository,
                           OperationRepository operationRepository,
                           MainActivityCommunication mainActivityCommunication) {
        mContext = context;
        mCategoriesRepository = categoriesRepository;
        mOperationRepository = operationRepository;
        mMainActivityCommunication = mainActivityCommunication;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_balance, container, false);
        mCategoriesRepository.setCategoriesUpdateCallback(this);
        mOperationRepository.setOperationsUpdateCallBack(this);

        /*TOP BLUE BAR*/
            setBlueBarDateRanges();
            rev_radio_btn = view.findViewById(R.id.rev_radio_btn);
            exp_radio_btn = view.findViewById(R.id.exp_radio_btn);
            cap_radio_btn = view.findViewById(R.id.cap_radio_btn);
            rev_radio_btn.setChecked(mOperationRepository.REV);
            exp_radio_btn.setChecked(mOperationRepository.EXP);
            cap_radio_btn.setChecked(mOperationRepository.CAP);
            rev_radio_btn.setOnClickListener(this);
            exp_radio_btn.setOnClickListener(this);
            cap_radio_btn.setOnClickListener(this);
            last_7_group = view.findViewById(R.id.last_7_group);
            last_28_group = view.findViewById(R.id.last_28_group);
            last_90_group = view.findViewById(R.id.last_90_group);
            last_7_group.setOnClickListener(this);
            last_28_group.setOnClickListener(this);
            last_90_group.setOnClickListener(this);;
            vs_plan = getString(R.string.vs_plan);
            last_7_sum = view.findViewById(R.id.last_7_sum);
            last_28_sum = view.findViewById(R.id.last_28_sum);
            last_90_sum = view.findViewById(R.id.last_90_sum);
            last_7_vs_plan = view.findViewById(R.id.last_7_vs_plan);
            last_28_vs_plan = view.findViewById(R.id.last_28_vs_plan);
            last_90_vs_plan = view.findViewById(R.id.last_90_vs_plan);
            top_bar_scale_label = view.findViewById(R.id.top_bar_scale_label);

        /*LAST OPERATIONS*/
            oldLastOperations = new ArrayList<>();
            newLastOperations = new ArrayList<>();
            operationDiffUtilCallback = new OperationDiffUtilCallback(oldLastOperations, newLastOperations);

            llm = new MyLinearLayoutManager(mContext);
            last_operations_rv = view.findViewById(R.id.last_operations_rv);
            llm.setScrollEnabled(false);
            last_operations_rv.setLayoutManager(llm);
            lastOperationsAdapter = new LastOperationsAdapter(mContext, newLastOperations, this);
            last_operations_rv.setAdapter(lastOperationsAdapter);
            last_operations_rv.setMotionEventSplittingEnabled(false);

        /*NEW OPERATION*/
            new_operation_holder = view.findViewById(R.id.new_operation_holder);
            newOperationFragment = new NewOperationFragment(mContext, mCategoriesRepository, this);
            getChildFragmentManager().beginTransaction().add(new_operation_holder.getId(), newOperationFragment).commit();
            new_operation_btn = view.findViewById(R.id.new_operation_btn);
            new_operation_btn.setOnClickListener(this);

        /*BALANCE*/
            balanceRows = new ArrayList<>();
            get_balance_btn = view.findViewById(R.id.get_balance_btn);
            get_balance_btn.setOnClickListener(this);
            plus_minus_image = view.findViewById(R.id.plus_minus_image);
            balance_progress_bar = view.findViewById(R.id.balance_progress_bar);
            balance_container = view.findViewById(R.id.balance_container);
            balance_line = view.findViewById(R.id.balance_line);
            assets_container = view.findViewById(R.id.assets_container);
            equity_container = view.findViewById(R.id.equity_container);
            total_assets_sum = view.findViewById(R.id.total_assets_sum);
            total_equity_sum = view.findViewById(R.id.total_equity_sum);
            balance_scrollView = view.findViewById(R.id.balance_scrollView);
            float density = mContext.getResources().getDisplayMetrics().density;
            Dp2= (int)(2f * density + 0.5f);
            Dp10 = (int)(10f * density + 0.5f);
            Dp40 = (int)(40f * density + 0.5f);
            Dp110 = (int)(110f * density + 0.5f);
            blueColor = ContextCompat.getColor(mContext,  R.color.blue_row);
            greyColor = ContextCompat.getColor(mContext,  R.color.border_row);
            greyTxtColor = ContextCompat.getColor(mContext,  R.color.grey_txt_color);
            greyTriangle = ContextCompat.getColor(mContext, R.color.grey_line);
            paint = new Paint();
            sp15TextSize = mContext.getResources().getDimensionPixelSize(R.dimen.barLabelFontSize);
            paint.setTextSize(sp15TextSize);
            balanceCollapsed = false;
            needCollapseBalance = mOperationRepository.needCollapseBalance;
            /*Year spinner*/
            if(mOperationRepository.selectedBalanceYear !=-1){
                selectedBalanceYear = mOperationRepository.selectedBalanceYear;
            }else {
                selectedBalanceYear = currentYear;
            }
            spinnerYears = new ArrayList<>();
            year_spinner = view.findViewById(R.id.year_spinner);
            yearSpinnerAdapter = new SpinnerIntegerAdapter(mContext, R.layout.spinner_item, spinnerYears);
            year_spinner.setAdapter(yearSpinnerAdapter);
            year_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if(spinnerYears.get(i)!=selectedBalanceYear){
                        selectedBalanceYear = spinnerYears.get(i);
                        updateBalanceTable();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

            /*Balance updater*/
            balanceHandler  = new Handler(Looper.getMainLooper());
            mainBalanceRunnable = new Runnable() {
                @Override
                public void run() {
                    mOperationRepository.getBalance(selectedBalanceYear, mCategoriesRepository.sortedCategories);
                    balanceHandler.post(finishBalanceRunnable);
                }
            };
            finishBalanceRunnable = new Runnable() {
                @Override
                public void run() {
                    balanceHandler.removeCallbacks(lockerBalanceUpdater);
                    buildBalanceTable();
                    if(needCollapseBalance){
                        collapseBalance();
                    }
                    balanceLockerUpdater();
                }
            };
            lockerBalanceUpdater = new Runnable() {
                @Override
                public void run() {
                    balanceLockerUpdater();
                }
            };

            /*Scale spinner*/
            dividerLabels = new ArrayList<>();
            dividerLabels.add(new DividerLabel(LongFormater.One, getString(R.string.no_scale)));
            dividerLabels.add(new DividerLabel(LongFormater.Thousand, getString(R.string.Thousand)));
            dividerLabels.add(new DividerLabel(LongFormater.Million, getString(R.string.Million)));
            dividerLabels.add(new DividerLabel(LongFormater.Billion, getString(R.string.Billion)));
            dividerLabels.add(new DividerLabel(LongFormater.Trillion, getString(R.string.Trillion)));
            scale_spinner = view.findViewById(R.id.scale_spinner);
            scaleSpinnerAdapter = new SpinnerScaleAdapter(mContext, R.layout.spinner_item, dividerLabels);
            scale_spinner.setAdapter(scaleSpinnerAdapter);
            dividerIndex = mOperationRepository.dividerIndex;
            scale_spinner.setSelection(dividerIndex);
            scale_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if(dividerIndex != i){
                        dividerIndex = i;
                        updateBalanceSumLabels();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });

        /*ANIMATIONS*/
            main_balance_view = view.findViewById(R.id.main_balance_view);

            afterNewOperationViewShown =  new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    main_balance_view.setVisibility(View.GONE);
                    new_operation_holder.getLayoutParams().height = FrameLayout.LayoutParams.MATCH_PARENT;
                    animationLock(false);
                    newOperationFragment.showSoftKeyboardIfNewOperation();
                }
            };
            afterNewOperationViewHidden=  new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    new_operation_holder.setVisibility(View.GONE);
                    main_balance_view.getLayoutParams().height = FrameLayout.LayoutParams.MATCH_PARENT;
                    animationLock(false);
                }
            };

            showNewOperationView = new CategoriesFragment.TransitionAnimator(main_balance_view,
                    new_operation_holder, true, afterNewOperationViewShown);
            hideNewOperationView = new CategoriesFragment.TransitionAnimator(new_operation_holder,
                    main_balance_view, false, afterNewOperationViewHidden);

        /*Back button callbacks*/
            callbackBtn = new OnBackPressedCallback(false /* enabled by default */) {
                @Override
                public void handleOnBackPressed() {
                    newOperationFragment.cancelBtnClick();
                }
            };
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callbackBtn);



        /*UPDATE DATA*/
        mOperationRepository.updateLastOperations(mCategoriesRepository.sortedCategories,
                todayDateCode, last7startDateCode, last28startDateCode, last90startDateCode);
        balanceLockerUpdater();
        // Inflate the layout for this fragment
        return view;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mOperationRepository.REV = rev_radio_btn.isChecked();
        mOperationRepository.EXP = exp_radio_btn.isChecked();
        mOperationRepository.CAP = cap_radio_btn.isChecked();
        mOperationRepository.selectedBalanceYear = selectedBalanceYear;
        mOperationRepository.needCollapseBalance = balanceCollapsed;
        mOperationRepository.dividerIndex = dividerIndex;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.new_operation_btn:
                newOperationBtnClick();
                break;
            case R.id.rev_radio_btn:
                setTopBlueBarData();
                break;
            case R.id.exp_radio_btn:
                setTopBlueBarData();
                break;
            case R.id.cap_radio_btn:
                setTopBlueBarData();
                break;
            case R.id.last_7_group:
                goToAnalysis(last7startDateCode, todayDateCode);
                break;
            case R.id.last_28_group:
                goToAnalysis(last28startDateCode, todayDateCode);
                break;
            case R.id.last_90_group:
                goToAnalysis(last90startDateCode, todayDateCode);
                break;
            case R.id.get_balance_btn:
                handleBalanceClick();
                break;
        }
    }

    private void handleBalanceClick() {
        if(balanceCollapsed){
            cancelBalanceLoading();
            hideBalance();
            balanceLockerUpdater();
        }else {
            cancelBalanceLoading();
            needCollapseBalance = true;
            balanceUpdateFuture = mOperationRepository.mExecutor.submit(mainBalanceRunnable);
            balanceHandler.postDelayed(lockerBalanceUpdater, 150);
        }
    }

    private void cancelBalanceLoading(){
        if(balanceUpdateFuture!=null && !balanceUpdateFuture.isDone()){
            needCollapseBalance = false;
            balanceUpdateFuture.cancel(true);
            balanceUpdateFuture = null;
        }
        balanceLockerUpdater();
    }
    private void balanceLockerUpdater(){
        if(mCategoriesRepository.mapIdLockedSize()!=0 ||
                (balanceUpdateFuture != null && !balanceUpdateFuture.isDone())){
            balance_progress_bar.setVisibility(View.VISIBLE);
            if(!balanceCollapsed){
                plus_minus_image.setColorFilter(greyTriangle);
                get_balance_btn.setEnabled(false);
            }else {
                plus_minus_image.setColorFilter(null);
                get_balance_btn.setEnabled(true);
            }
        }else {
            balance_progress_bar.setVisibility(View.GONE);
            plus_minus_image.setColorFilter(null);
            get_balance_btn.setEnabled(true);
        }
    }

    private void collapseBalance(){
        balanceCollapsed = true;
        needCollapseBalance = false;
        balance_line.setBackgroundColor(blueColor);
        setBalanceContainerHeight();
        plus_minus_image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.analysis_icon_minus));
    }

    private void setBalanceContainerHeight(){
        //final int height = Dp40*4 + Dp40*assets_container.getChildCount()+Dp40*equity_container.getChildCount();
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)balance_container.getLayoutParams();
        lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        balance_container.setLayoutParams(lp);
    }

    private void hideBalance(){
        balanceCollapsed = false;
        balance_line.setBackgroundColor(greyColor);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)balance_container.getLayoutParams();
        lp.height = 0;
        balance_container.setLayoutParams(lp);
        plus_minus_image.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.analysis_icon_plus));
    }

    private void buildBalanceTable() {
        assets_container.removeAllViews();
        equity_container.removeAllViews();
        balanceRows.clear();
        for(int i = 0; i < mOperationRepository.balanceRowData.size(); i++){
            String name =  mOperationRepository.balanceRowData.get(i).name;
            if(paint.measureText(name) > Dp110 - Dp10){
                name = mOperationRepository.balanceRowData.get(i).nick;
            }
            String val = LongFormater.formatLong(mOperationRepository.balanceRowData.get(i).value,
                    dividerLabels.get(dividerIndex).divider, LongFormater.twoDecimal);
            BalanceRow tempRow = new BalanceRow(mContext,this, name, val, i, Dp2, Dp10, Dp40, Dp110, greyTxtColor);
            if(mOperationRepository.balanceRowData.get(i).asset){
                assets_container.addView(tempRow);
            }else {
                equity_container.addView(tempRow);
            }
            balanceRows.add(tempRow);
        }
        total_assets_sum.setText(LongFormater.formatLong(mOperationRepository.totalAssets, dividerLabels.get(dividerIndex).divider, LongFormater.twoDecimal));
        total_equity_sum.setText(LongFormater.formatLong(mOperationRepository.totalEquity, dividerLabels.get(dividerIndex).divider, LongFormater.twoDecimal));
    }

    private void updateBalanceSumLabels(){
        for(int i = 0; i < balanceRows.size(); i++){
            int index = balanceRows.get(i).index;
            String val = LongFormater.formatLong(mOperationRepository.balanceRowData.get(index).value,
                    dividerLabels.get(dividerIndex).divider, LongFormater.twoDecimal);
            balanceRows.get(i).updateLabel(val);
        }
        total_assets_sum.setText(LongFormater.formatLong(mOperationRepository.totalAssets, dividerLabels.get(dividerIndex).divider, LongFormater.twoDecimal));
        total_equity_sum.setText(LongFormater.formatLong(mOperationRepository.totalEquity, dividerLabels.get(dividerIndex).divider, LongFormater.twoDecimal));
    }

    @Override
    public void balanceRowClicked(int index) {
        if(mOperationRepository.balanceRowData.get(index).appliedSelectedCategories.size()>1){
            mMainActivityCommunication.setAnalysisSelectedCategories(mOperationRepository.balanceRowData.get(index).appliedSelectedCategories);
            int dateCodeFrom = PlanData.getDayCode(mOperationRepository.minYear, 0, 1);
            int dateCodeTo = PlanData.getDayCode(selectedBalanceYear, 11, 31);
            boolean rev = true;
            boolean exp = true;
            if(index>1){
                rev = false;
                exp = false;
            }
            mMainActivityCommunication.transitToAnalysis(dateCodeFrom, dateCodeTo,
                    rev, exp, true, true, false);
        }else {

            mMainActivityCommunication.transitionToOperations(mOperationRepository.balanceRowData.get(index).categoryId, selectedBalanceYear);
        }


    }

    private void goToAnalysis(int dateCodeFrom, int dateCodeTo){
        mMainActivityCommunication.transitToAnalysis(dateCodeFrom, dateCodeTo,
                rev_radio_btn.isChecked(), exp_radio_btn.isChecked(), cap_radio_btn.isChecked(), false, true);
    }

    private void setBlueBarDateRanges() {
        Calendar init = Calendar.getInstance();
        int day = init.get(Calendar.DAY_OF_MONTH);
        int month = init.get(Calendar.MONTH);
        int year = init.get(Calendar.YEAR);
        currentYear = year;
        todayDateCode = PlanData.getDayCode(year, month, day);

        Calendar cal = (Calendar)init.clone();
        cal.add(Calendar.DATE, -6);
        day = cal.get(Calendar.DAY_OF_MONTH);
        month = cal.get(Calendar.MONTH);
        year = cal.get(Calendar.YEAR);
        last7startDateCode = PlanData.getDayCode(year, month, day);

        cal.add(Calendar.DATE, -21);
        day = cal.get(Calendar.DAY_OF_MONTH);
        month = cal.get(Calendar.MONTH);
        year = cal.get(Calendar.YEAR);
        last28startDateCode = PlanData.getDayCode(year, month, day);

        cal.add(Calendar.DATE, -62);
        day = cal.get(Calendar.DAY_OF_MONTH);
        month = cal.get(Calendar.MONTH);
        year = cal.get(Calendar.YEAR);
        last90startDateCode = PlanData.getDayCode(year, month, day);
    }


    private void newOperationBtnClick() {
        newOperationFragment.setNewOperationLayout();
        showNewOperationView();
    }

    private void showNewOperationView() {
        animationLock(true);
        callbackBtn.setEnabled(true);
        prepareNewOperationViewToBeShown();
        mMainActivityCommunication.hideBottomNavigation();
        showNewOperationView.startTransition();

    }

    private void animationLock(boolean lock) {
        isAnimating = lock;
        newOperationFragment.setAnimating(lock);
    }

    private void prepareNewOperationViewToBeShown() {
        main_balance_view.getLayoutParams().height = main_balance_view.getHeight();
        new_operation_holder.getLayoutParams().height = main_balance_view.getHeight() + mMainActivityCommunication.getBottomNavigationHeight();
        new_operation_holder.setAlpha(0f);
        new_operation_holder.setVisibility(View.VISIBLE);
    }

    private void hideNewOperationView() {
        animationLock(true);
        callbackBtn.setEnabled(false);
        prepareNewOperationViewToBeHidden();
        mMainActivityCommunication.showBottomNavigation();
        hideNewOperationView.startTransition();
    }

    private void prepareNewOperationViewToBeHidden() {
        new_operation_holder.getLayoutParams().height = new_operation_holder.getHeight();
        main_balance_view.setAlpha(0f);
        main_balance_view.setVisibility(View.VISIBLE);
    }

    private void applyLastOperationsChanges() {
        oldLastOperations.clear();
        oldLastOperations.addAll(newLastOperations);
        newLastOperations.clear();
        newLastOperations.addAll(mOperationRepository.lastOperations);
        DiffUtil.DiffResult dr = DiffUtil.calculateDiff(operationDiffUtilCallback);
        dr.dispatchUpdatesTo(lastOperationsAdapter);
    }


    /*LastOperationAdapter Callback*/
    @Override
    public void LastOperationClicked(Operation operation) {
        newOperationFragment.setUpdateOperationLayout(operation);
        showNewOperationView();
    }
    @Override
    public boolean isCategoryLocked(long categoryId) {
        return mCategoriesRepository.isCategoryLocked(categoryId);
    }

    @Override
    public String getCategoryName(long categoryId) {
        return mCategoriesRepository.getCategoryNameById(categoryId);
    }

    @Override
    public int getCategoryType(long categoryId) {
        return mCategoriesRepository.getCategoryTypeById(categoryId);
    }


    /*CategoriesUpdateCallback*/
    @Override
    public void CategoriesUpdated() {
        lastOperationsAdapter.notifyDataSetChanged();
        newOperationFragment.categoryUpdated();
        mOperationRepository.updateLastOperations(mCategoriesRepository.sortedCategories,
                todayDateCode, last7startDateCode, last28startDateCode, last90startDateCode);
        balanceLockerUpdater();
    }

    @Override
    public void CategoryUpdated(long id, int index, boolean locked) {
        lastOperationsAdapter.updateLockState(id, locked);
        newOperationFragment.categoryUpdated(id, index, locked);
        if(!locked){
            mOperationRepository.updateLastOperations(mCategoriesRepository.sortedCategories,
                    todayDateCode, last7startDateCode, last28startDateCode, last90startDateCode);
        }
        balanceLockerUpdater();
    }

    /*NewOperationFragment.NewOperationCallback*/
    @Override
    public void cancelBtnClicked() {
        hideNewOperationView();
    }

    @Override
    public void saveUpdateOperation(long operationId, long categoryId, long initCategoryId,
                                    int day, int month, int year, long value, String description) {
        if(mDbService!=null){
            Operation operation = new Operation(categoryId, day, month, year, value, description);
            mDbService.startSavingOperation(operation, operationId, initCategoryId);
            hideNewOperationView();
        }else {
            mMainActivityCommunication.makeToast(MainActivity.DB_SERVICE_IS_NULL);
        }
    }

    @Override
    public void deleteOperation(long operationId, long categoryId) {
        if(mDbService!=null){
            mDbService.startDeleteOperation(operationId, categoryId);
            hideNewOperationView();
        }else {
            mMainActivityCommunication.makeToast(MainActivity.DB_SERVICE_IS_NULL);
        }
    }


    /*OperationRepository.OperationsUpdateCallBack*/
    @Override
    public void operationsUpdated() {
        //RemoveCallBack
        applyLastOperationsChanges();
        setTopBlueBarData();
        updateYearSpinner();
        updateBalanceTable();
    }

    private void updateBalanceTable() {
        if((balanceCollapsed || needCollapseBalance) && mCategoriesRepository.mapIdLockedSize() == 0){
            cancelBalanceLoading();
            balanceUpdateFuture = mOperationRepository.mExecutor.submit(mainBalanceRunnable);
            balanceHandler.postDelayed(lockerBalanceUpdater, 150);
        }
    }


    private void updateYearSpinner() {
        spinnerYears.clear();
        int startYear = mOperationRepository.minYear;
        int endYear = mOperationRepository.maxYear;
        for(int i = startYear; i<=endYear;i++){
            spinnerYears.add(i);
        }
        if(selectedBalanceYear < startYear){
            selectedBalanceYear = currentYear;
        }else if(selectedBalanceYear > endYear){
            spinnerYears.add(selectedBalanceYear);
        }
        yearSpinnerAdapter.notifyDataSetChanged();
        setSpinnerSelection(selectedBalanceYear);
    }

    private void setSpinnerSelection(int selectedBalanceYear) {
        for (int i = 0; i<spinnerYears.size();i++){
            if(selectedBalanceYear == spinnerYears.get(i)){
                year_spinner.setSelection(i);
                //Log.d("MyTag","selectedBalanceYear " + selectedBalanceYear);
                break;
            }
        }
    }

    private void setTopBlueBarData() {
        int index = getTopBarCatIndex();
        long last7fact = mOperationRepository.last7daysFact[index];
        long last28fact = mOperationRepository.last28daysFact[index];
        long last90fact = mOperationRepository.last90daysFact[index];
        long max = Math.max(Math.max(last7fact, last28fact),last90fact);
        long min = Math.min(Math.min(last7fact, last28fact),last90fact);

        long divider = LongFormater.getDivider(max, min);
        int decimal = LongFormater.getDecimals(divider, max, min);

        last_7_sum.setText(LongFormater.formatLong(last7fact, divider, decimal));
        last_28_sum.setText(LongFormater.formatLong(last28fact, divider, decimal));
        last_90_sum.setText(LongFormater.formatLong(last90fact, divider, decimal));

        long last7plan = mOperationRepository.last7daysPlan[index];
        long last28plan = mOperationRepository.last28daysPlan[index];
        long last90plan = mOperationRepository.last90daysPlan[index];

        last_7_vs_plan.setText(String.format("%s%s", LongFormater.getPercentDelta(last7fact - last7plan, last7plan, last7fact, true), vs_plan));
        last_28_vs_plan.setText(String.format("%s%s", LongFormater.getPercentDelta(last28fact - last28plan, last28plan, last28fact, true), vs_plan));
        last_90_vs_plan.setText(String.format("%s%s", LongFormater.getPercentDelta(last90fact - last90plan, last90plan, last90fact, true), vs_plan));

        top_bar_scale_label.setText(getScaleLabel(divider));
    }

    private int getTopBarCatIndex() {
        if(rev_radio_btn.isChecked()){
            return 0;
        }else if (exp_radio_btn.isChecked()){
            return 1;
        }else {
            return 2;
        }
    }

    private String getScaleLabel(long divider) {
        if(divider == LongFormater.One){
            return mContext.getString(R.string.no_scale);
        }else if(divider == LongFormater.Thousand){
            return mContext.getString(R.string.Thousand);
        }else if (divider == LongFormater.Million){
            return mContext.getString(R.string.Million);
        }else if (divider == LongFormater.Billion){
            return mContext.getString(R.string.Billion);
        }else {
            return mContext.getString(R.string.Trillion);
        }
    }



    public static class OperationDiffUtilCallback extends DiffUtil.Callback{
        List<Operation> oldList;
        List<Operation> newList;
        public OperationDiffUtilCallback(List<Operation> oldList, List<Operation> newList){
            this.oldList = oldList;
            this.newList = newList;
        }
        public void setNewLists(List<Operation> oldList, List<Operation> newList){
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
            return oldList.get(oldItemPosition).id == newList.get(newItemPosition).id;
        }
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).dateCode == newList.get(newItemPosition).dateCode&&
                    oldList.get(oldItemPosition).value == newList.get(newItemPosition).value&&
                    oldList.get(oldItemPosition).categoryId == newList.get(newItemPosition).categoryId;
        }
    }

    public static class DividerLabel{
        public long divider;
        public String dividerLabel;
        public DividerLabel(long divider, String dividerLabel) {
            this.divider = divider;
            this.dividerLabel = dividerLabel;
        }
    }
}
