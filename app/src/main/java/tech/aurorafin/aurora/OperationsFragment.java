package tech.aurorafin.aurora;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import tech.aurorafin.aurora.dbRoom.CategoriesRepository;
import tech.aurorafin.aurora.dbRoom.Operation;
import tech.aurorafin.aurora.dbRoom.OperationRepository;
import tech.aurorafin.aurora.dbRoom.OperationTotal;

import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.Future;


public class OperationsFragment extends Fragment implements CategoryFilterAdapter.CategoryFilterCallback,
        CategoriesRepository.CategoriesUpdateCallback, OperData.OperDataCallback, View.OnClickListener,
        NumberPicker.OnValueChangeListener, OperationRepository.OperationsUpdateCallBack, NewOperationFragment.NewOperationCallback {

    Context mContext;
    CategoriesRepository mCategoriesRepository;
    OperationRepository mOperationRepository;
    MainActivityCommunication mMainActivityCommunication;
    OperData mOperData;
    String[] stringOperations;

    DbService mDbService;

    public void setmDbService(DbService mDbService) {
        this.mDbService = mDbService;
    }

    /*Curtain*/
        private PlanFragment.CurtainMover operationsCurtainMover;
        private AppCompatImageView toolTrack;
        private FrameLayout curtain_holder;
        private LinearLayout operationFilter;
        private LinearLayout toolBarPanel;
        private FrameLayout filter_inner_container;
        private View filterShadow;
        private AppCompatImageView operation_toolbar_triangle;
        private FrameLayout operation_toolbar_collapse_btn;
        CategoryFilter categoryFilter;
        private FrameLayout category_filter_holder;
        private NumberPicker numberPicker2;
        private RadioButton month_radio_btn;
        private RadioButton week_radio_btn;

    /*Curtain btns*/
        AppCompatImageButton operation_delete_btn;
        AppCompatImageButton operation_sort_btn;
        AppCompatImageButton operation_new_btn;
        boolean sortUp = false;
        AlertDialog deleteDialog;

    /*Table*/
        LinearLayout recyclerViewContainer;
        RecyclerView operationTable;
        OperTableAdapter mOperTableAdapter;
        ScreenLocker  operations_table_screen_locker;
        TextView cancel_operations_btn;
        MyLinearLayoutManager llm;

    /*Bottom Total*/
        TextView selectAllCancelBtn;
        ObjectAnimator animSelectAllCancelBtn;
        TextView operationsTotalTextView;

    /*NEW OPERATION FRAGMENT*/
        FrameLayout new_operation_holder;
        FrameLayout main_operations_view;
        NewOperationFragment newOperationFragment;
        boolean isAnimating = false;
        CategoriesFragment.TransitionAnimator showNewOperationView;
        CategoriesFragment.TransitionAnimator hideNewOperationView;
        private AnimatorListenerAdapter afterNewOperationViewShown;
        private AnimatorListenerAdapter afterNewOperationViewHidden;

    /*Operations Data*/
        int currentYear;
        int operationsYear;
        int tempYear;
        long tempCategoryId;
        long operationCategoryId;
        Runnable operationsUpdaterMainRunnable;
        Runnable operationsUpdaterFinishRunnable;
        Runnable operationsUpdaterEnterRunnable;
        Runnable operationsUpdaterAfterSaveMainRunnable;
        Runnable operationsUpdaterAfterSaveFinishRunnable;
        Future<?> updaterMainFuture;
        Handler operationHandler;
        int operationHandlerDelayAttempts;
        Future<?> operationsUpdateFuture;
        boolean updateSubRowOperationsAnyway = true;

        OnBackPressedCallback callbackBtnCollapseRowWithAnim;
        OnBackPressedCallback callbackBtnDropSelection;
        OnBackPressedCallback callbackBtnHideNewOperFragment;


    public OperationsFragment(Context context, CategoriesRepository categoriesRepository,
                              OperationRepository operationRepository, MainActivityCommunication mainActivityCommunication) {
        mContext = context;
        mCategoriesRepository = categoriesRepository;
        mOperationRepository = operationRepository;
        mMainActivityCommunication =mainActivityCommunication;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_operations, container, false);

        mCategoriesRepository.setCategoriesUpdateCallback(this);
        mOperationRepository.setOperationsUpdateCallBack(this);


        stringOperations = mContext.getResources().getStringArray(R.array.category_types_operation_names);
        mOperData = new OperData(mContext, this);
        //mOperData.populateEmptyLists(1, 2020);


        /*Curtain*/
            toolTrack = view.findViewById(R.id.toolTrack);
            curtain_holder = view.findViewById(R.id.curtain_holder);
            operationFilter = view.findViewById(R.id.operationFilter);
            toolBarPanel = view.findViewById(R.id.toolBarPanel);
            filter_inner_container = view.findViewById(R.id.filter_inner_container);
            filterShadow = view.findViewById(R.id.filterShadow);
            operation_toolbar_triangle = view.findViewById(R.id.operation_toolbar_triangle);
            operation_toolbar_collapse_btn = view.findViewById(R.id.toolbar_collapse_btn);
            operationsCurtainMover = new PlanFragment.CurtainMover(mContext, toolTrack, curtain_holder, operationFilter,
                    toolBarPanel, filter_inner_container, filterShadow, operation_toolbar_triangle, operation_toolbar_collapse_btn);

            category_filter_holder = view.findViewById(R.id.category_filter_holder);
            categoryFilter = new CategoryFilter(mContext, false, mCategoriesRepository.categories, this);
            RecyclerView.LayoutParams layoutParams= new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT);
            categoryFilter.setLayoutParams(layoutParams);
            category_filter_holder.addView(categoryFilter);


            numberPicker2  = view.findViewById(R.id.numberPicker2);
            numberPicker2.setMaxValue(2100);
            numberPicker2.setMinValue(1900);
            currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if(mOperationRepository.tempOperationYear !=- 1 ){
                operationsYear = mOperationRepository.tempOperationYear;
            }else {
                operationsYear = currentYear;
            }
            tempYear = operationsYear;
            numberPicker2.setValue(operationsYear);
            numberPicker2.setOnValueChangedListener(this);

            month_radio_btn = view.findViewById(R.id.month_radio_btn);
            week_radio_btn = view.findViewById(R.id.week_radio_btn);
            month_radio_btn.setOnClickListener(this);
            week_radio_btn.setOnClickListener(this);
            mOperData.weekPresentation = mOperationRepository.tempWeekPresentation;
            month_radio_btn.setChecked(!mOperData.weekPresentation);
            week_radio_btn.setChecked(mOperData.weekPresentation);


        /*Curtain btns*/
            operation_delete_btn = view.findViewById(R.id.operation_delete_btn);
            operation_delete_btn.setOnClickListener(this);
            operation_delete_btn.setEnabled(false);
            operation_sort_btn = view.findViewById(R.id.operation_sort_btn);
            operation_sort_btn.setOnClickListener(this);

        /*Table*/
            recyclerViewContainer = view.findViewById(R.id.recyclerViewContainer);
            operationTable = view.findViewById(R.id.operationTable);
            operationTable.setMotionEventSplittingEnabled(false);
            operationTable.setHasFixedSize(true);
            ((SimpleItemAnimator) Objects.requireNonNull(operationTable.getItemAnimator())).setSupportsChangeAnimations(false);
            llm = new MyLinearLayoutManager(mContext);
            operationTable.setLayoutManager(llm);
            mOperTableAdapter = new OperTableAdapter(mContext, mOperData, this, llm, operationTable, recyclerViewContainer);
            operationTable.setAdapter(mOperTableAdapter);
            operations_table_screen_locker = view.findViewById(R.id.operations_table_screen_locker);
            cancel_operations_btn = view.findViewById(R.id.cancel_operations_btn);
            cancel_operations_btn.setOnClickListener(this);

        /*Bottom Total*/
            selectAllCancelBtn = view.findViewById(R.id.selectAllCancelBtn);
            selectAllCancelBtn.setOnClickListener(this);
            operationsTotalTextView = view.findViewById(R.id.operationsTotalTextView);

        /*NEW OPERATION*/

            newOperationFragment = new NewOperationFragment(mContext, mCategoriesRepository, this);
            new_operation_holder = view.findViewById(R.id.new_operation_holder);
            getChildFragmentManager().beginTransaction().add(new_operation_holder.getId(), newOperationFragment).commit();
            operation_new_btn = view.findViewById(R.id.operation_new_btn);
            operation_new_btn.setOnClickListener(this);


        /*ANIMATIONS*/
            main_operations_view = view.findViewById(R.id.main_operations_view);

            afterNewOperationViewShown =  new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    main_operations_view.setVisibility(View.GONE);
                    new_operation_holder.getLayoutParams().height = FrameLayout.LayoutParams.MATCH_PARENT;
                    animationLock(false);
                    newOperationFragment.showSoftKeyboardIfNewOperation();
                }
            };
            afterNewOperationViewHidden=  new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    new_operation_holder.setVisibility(View.GONE);
                    main_operations_view.getLayoutParams().height = FrameLayout.LayoutParams.MATCH_PARENT;
                    animationLock(false);
                }
            };

            showNewOperationView = new CategoriesFragment.TransitionAnimator(main_operations_view, new_operation_holder, true, afterNewOperationViewShown);
            hideNewOperationView = new CategoriesFragment.TransitionAnimator(new_operation_holder, main_operations_view, false, afterNewOperationViewHidden);



        /*Back Button callbacks*/
            callbackBtnCollapseRowWithAnim = new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    mOperTableAdapter.minimizeWithAnimation();
                }
            };

             callbackBtnDropSelection = new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                        mOperData.dropSelection();
                }
            };
            callbackBtnHideNewOperFragment = new OnBackPressedCallback(false /* enabled by default */) {
                @Override
                public void handleOnBackPressed() {
                    newOperationFragment.cancelBtnClick();
                }
            };
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callbackBtnCollapseRowWithAnim);
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callbackBtnDropSelection);
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callbackBtnHideNewOperFragment);

        /*DATA UPDATER*/
        operationHandler = new Handler(Looper.getMainLooper());
        operationHandlerDelayAttempts = 4;

        operationsUpdaterEnterRunnable = new Runnable() {
            public void run() {
                if(tempCategoryId !=-1 &&
                        (operationsYear != tempYear || operationCategoryId != tempCategoryId)){
                    operationsYear = tempYear;
                    operationCategoryId = tempCategoryId;
                    /*LockScreen handle*/
                    cancelOperationsLoading();
                    if(mCategoriesRepository.isCategoryLocked(operationCategoryId)){
                        operations_table_screen_locker.lockScreen();
                    }else {
                        operations_table_screen_locker.unlockScreen();
                    }
                    /*Interrupt current Update */
                    if(updaterMainFuture!=null
                            &&!updaterMainFuture.isDone()){
                        updaterMainFuture.cancel(true);
                    }
                    updaterMainFuture =  mOperationRepository.mExecutor.submit(operationsUpdaterMainRunnable);
                }
            }
        };

        operationsUpdaterMainRunnable = new Runnable() {
            @Override
            public void run() {
                OperationTotal[] totalMonths = mOperationRepository.getOperationMonthTotals(operationCategoryId, operationsYear);
                OperationTotal[] totalDays = mOperationRepository.getOperationDayTotals(operationCategoryId, operationsYear);
                OperationTotal totalYear = mOperationRepository.getOperationYearTotal(operationCategoryId, operationsYear);
                mOperData.resetAndMapSafe(totalMonths, totalDays, totalYear, operationCategoryId, operationsYear);
                operationHandler.post(operationsUpdaterFinishRunnable);
            }
        };

        operationsUpdaterFinishRunnable = new Runnable() {
            @Override
            public void run() {
                mOperData.resetIU();
                mOperTableAdapter.minimize();
                mOperTableAdapter.notifyDataSetChanged();
                operationsTotalTextView.setText(mOperData.getBtrValue());
                if(mOperationRepository.canCollapseRow){
                    if(mOperationRepository.tempCollapsedRow!=-1){
                        operationHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                llm.scrollToPositionWithOffset(mOperationRepository.tempCollapsedRow, 10);
                                RecyclerView.ViewHolder v = operationTable.findViewHolderForAdapterPosition(mOperationRepository.tempCollapsedRow);
                                if(v != null){
                                    mOperTableAdapter.maximize(mOperationRepository.tempCollapsedRow);
                                }else {
                                    if(operationHandlerDelayAttempts !=0 ){
                                        operationHandlerDelayAttempts = operationHandlerDelayAttempts -1;
                                        operationHandler.postDelayed(this, 100);
                                    }

                                }
                            }
                        },10);
                    }
                    mOperationRepository.canCollapseRow = false;
                }
            }
        };

        operationsUpdaterAfterSaveMainRunnable = new Runnable() {
            @Override
            public void run() {
                OperationTotal[] totalMonths = mOperationRepository.getOperationMonthTotals(operationCategoryId, operationsYear);
                OperationTotal[] totalDays = mOperationRepository.getOperationDayTotals(operationCategoryId, operationsYear);
                OperationTotal totalYear = mOperationRepository.getOperationYearTotal(operationCategoryId, operationsYear);
                mOperData.resetAndMapSafe(totalMonths, totalDays, totalYear, operationCategoryId, operationsYear);
                if(mOperTableAdapter.collapsedHolderPos!=-1){
                    OperData.LocalTotal lt = mOperData.getLocalTotalForOperationsUpdate(mOperTableAdapter.collapsedHolderPos);
                    mOperationRepository.updateOperationsForSubRowAdapterWoCallback(lt.categoryId, lt.dateCodeFrom, lt.dateCodeTo);
                }
                operationHandler.post(operationsUpdaterAfterSaveFinishRunnable);
            }
        };

        operationsUpdaterAfterSaveFinishRunnable = new Runnable() {
            @Override
            public void run() {

                mOperData.setNewOperations(mOperationRepository.operationsForSubRowAdapter);
                mOperTableAdapter.rebindAdapters(true);
                operationsTotalTextView.setText(mOperData.getBtrValue());
                if(mOperTableAdapter.collapsedHolderPos!=-1){
                    if(mOperData.mOperations.size() == 0){
                        mOperTableAdapter.minimizeWithAnimation();
                       // mOperTableAdapter.notifyItemChanged();
                    }
                }

                operations_table_screen_locker.unlockScreen();
            }
        };


        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                deleteSelectedOperations();
            }
        });
        builder.setNegativeButton(R.string.cancel1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        builder.setTitle(R.string.delete_operations);
        builder.setMessage(R.string.delete_operations_message);
        deleteDialog = builder.create();


        /*Default category set*/
        defaultCategorySetter();

        // Inflate the layout for this fragment
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelOperationsLoading();
        mOperationRepository.tempOperationCategoryId = operationCategoryId;
        mOperationRepository.tempOperationYear = operationsYear;
        //mOperationRepository.tempCollapsedRow = mOperTableAdapter.collapsedHolderPos;
        mOperationRepository.tempWeekPresentation = mOperData.weekPresentation;
    }

    private void defaultCategorySetter() {
        tempCategoryId = mOperationRepository.tempOperationCategoryId;
        int index = -1;
        if(tempCategoryId != - 1){
            index =  mCategoriesRepository.indexOfCategory(tempCategoryId);
        }
        if(index == -1){
            tempCategoryId = mCategoriesRepository.getFirstCategoryId();
            index = mCategoriesRepository.indexOfCategory(tempCategoryId);
            mOperationRepository.canCollapseRow = false;
        }
        operationCategoryId = tempCategoryId;
        if(operationCategoryId != -1){
            if(index != -1){
                String s = stringOperations[mCategoriesRepository.categories.get(index).type] +" - " + mCategoriesRepository.categories.get(index).name;
                mMainActivityCommunication.setAppbarText(s);
                categoryFilter.setSelectedCategory(index);
                if(mCategoriesRepository.isCategoryLocked(operationCategoryId)){
                    operations_table_screen_locker.lockScreen();
                }
            }
            updaterMainFuture =  mOperationRepository.mExecutor.submit(operationsUpdaterMainRunnable);
        } else {
            //Categories not loaded? it will be a callback, or there is no categories created yet
            mMainActivityCommunication.setAppbarText(getResources().getString(R.string.operations));
        }
    }

    @Override
    public void onClick(View view) {
        if(!isAnimating){
            int id = view.getId();
            switch (id) {
                case R.id.selectAllCancelBtn:
                    SelectAllCancelBtnClick();
                    break;
                case R.id.operation_sort_btn:
                    SortBtnClick();
                    break;
                case R.id.week_radio_btn:
                    onGroupPeriodRadioButtonClicked(true);
                    break;
                case R.id.month_radio_btn:
                    onGroupPeriodRadioButtonClicked(false);
                    break;
                case R.id.cancel_operations_btn:
                    cancelOperationsLoading();
                    break;
                case R.id.operation_new_btn:
                    operationNewBtnClick();
                    break;
                case R.id.operation_delete_btn:
                    deleteDialog.show();
                    break;



            }
        }
    }

    private void deleteSelectedOperations() {
        cancelOperationsLoading();
        if(mDbService!=null){
            if(mOperData.operationsRowsSelection){
                mDbService.startDeleteOperationsList(mOperData.CATEGORY_ID, mOperData.YEAR, mOperData.getDeleteOperationsList());
            }else if(mOperData.localTotalsSelection){
                mDbService.startDeleteOperationsTotals(mOperData.CATEGORY_ID, mOperData.YEAR, mOperData.getDeleteOperationsTotals());
            }
            mOperData.resetIU();
        }else {
            mMainActivityCommunication.makeToast(MainActivity.DB_SERVICE_IS_NULL);
        }
    }

    private void hideNewOperationView() {
        animationLock(true);
        callbackBtnHideNewOperFragment.setEnabled(false);
        prepareNewOperationViewToBeHidden();
        mMainActivityCommunication.showBottomNavigation();
        hideNewOperationView.startTransition();
    }

    private void prepareNewOperationViewToBeHidden() {
        new_operation_holder.getLayoutParams().height = new_operation_holder.getHeight();
        main_operations_view.setAlpha(0f);
        main_operations_view.setVisibility(View.VISIBLE);
    }

    private void animationLock(boolean lock) {
        isAnimating = lock;
        newOperationFragment.setAnimating(lock);
    }

    private void operationNewBtnClick() {
        newOperationFragment.setNewOperationLayout();
        newOperationFragment.presetCategory(operationCategoryId);
        showNewOperationView();
    }

    private void showNewOperationView() {
        animationLock(true);
        callbackBtnHideNewOperFragment.setEnabled(true);
        prepareNewOperationViewToBeShown();
        mMainActivityCommunication.hideBottomNavigation();
        showNewOperationView.startTransition();
    }

    private void prepareNewOperationViewToBeShown() {
        main_operations_view.getLayoutParams().height = main_operations_view.getHeight();
        new_operation_holder.getLayoutParams().height = main_operations_view.getHeight() + mMainActivityCommunication.getBottomNavigationHeight();
        new_operation_holder.setAlpha(0f);
        new_operation_holder.setVisibility(View.VISIBLE);
    }




    private boolean isCurrentOperationsListActual(long categoryId, int dateCodeFrom, int dateCodeTo){
        if(!updateSubRowOperationsAnyway){
            if(mOperData.CATEGORY_ID == categoryId){
                if(mOperData.mOperations.size()>0){
                    int currentDcFrom =  mOperData.mOperations.get(0).dateCode;
                    int currentDcTo =  mOperData.mOperations.get(mOperData.mOperations.size()-1).dateCode;
                    return (currentDcFrom == dateCodeFrom && currentDcTo == dateCodeTo)
                            ||(currentDcTo == dateCodeFrom && currentDcFrom == dateCodeTo);
                }else {
                    return false;
                }
            }else {
                return false;
            }
        }else {
            return false;
        }
    }

    private void cancelOperationsLoading() {
        if(operationsUpdateFuture!=null && !operationsUpdateFuture.isDone()){
            operationsUpdateFuture.cancel(true);
            operationsUpdateFuture = null;
            mOperTableAdapter.cancelCollapsePending();
        }
        if(!mCategoriesRepository.isCategoryLocked(operationCategoryId)){
           operations_table_screen_locker.unlockScreen();
        }
        cancel_operations_btn.setVisibility(View.GONE);
    }

    private void onGroupPeriodRadioButtonClicked(boolean weekPresentation) {
        cancelOperationsLoading();
        mOperTableAdapter.minimize();
        mOperData.switchPresentationMode(weekPresentation);
        mOperTableAdapter.notifyDataSetChanged();
    }

    @Override
    public void onValueChange(NumberPicker numberPicker, int i, int i1) {
        tempYear = i1;
        runUpdater(200);
    }

    private void runUpdater(int delay){
        operationHandler.removeCallbacks(operationsUpdaterEnterRunnable);
        operationHandler.postDelayed(operationsUpdaterEnterRunnable, delay);
    }


    private void SortBtnClick() {
        cancelOperationsLoading();
        mOperData.resetIU();
        mOperTableAdapter.minimize();
        mOperData.reverseTotals();
        mOperTableAdapter.notifyDataSetChanged();
        if(mOperData.reverse){
            operation_sort_btn.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.operation_btn_sort_down));
        }else {
            operation_sort_btn.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.operation_btn_sort_up));
        }
    }




    /*SELECTION------------------*/

    public void SelectAllCancelBtnClick(){
        mOperData.selectAllCancel();
    }

    public void showSelectionBtn(String txt){
        selectAllCancelBtn.setAlpha(0);
        selectAllCancelBtn.setVisibility(View.VISIBLE);
        selectAllCancelBtn.setText(txt);
        animSelectAllCancelBtn = ObjectAnimator.ofFloat(selectAllCancelBtn, "alpha", 1);
        animSelectAllCancelBtn.setDuration(200);
        animSelectAllCancelBtn.start();
        callbackBtnDropSelection.setEnabled(true);
    }

    public void switchSelectionBtnTxt(final String txtTo){
        selectAllCancelBtn.setText(txtTo);
    }

    public void hideSelectionBtn(){
        animSelectAllCancelBtn.cancel();
        animSelectAllCancelBtn = ObjectAnimator.ofFloat(selectAllCancelBtn, "alpha", 0);
        animSelectAllCancelBtn.setDuration(150);
        animSelectAllCancelBtn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                selectAllCancelBtn.setVisibility(View.INVISIBLE);
            }
        });
        animSelectAllCancelBtn.start();
        callbackBtnDropSelection.setEnabled(false);
    }



    /*CategoryFilterAdapter.CategoryFilterCallback*/
    @Override
    public void CategoryClicked(int position, int type, long id) {
        String s = stringOperations[type] +" - " + mCategoriesRepository.categories.get(position).name;
        ((MainActivity) requireActivity()).setAppBarText(s);
        tempCategoryId = id;
        runUpdater(100);
    }

    @Override
    public boolean isCategoryLocked(long id) {
        return mCategoriesRepository.isCategoryLocked(id);
    }


    /*CategoriesRepository.CategoriesUpdateCallback*/
    @Override
    public void CategoriesUpdated() {
        newOperationFragment.categoryUpdated();
        if(operationCategoryId == -1){
            defaultCategorySetter();
        }else {
            int index = mCategoriesRepository.indexOfCategory(operationCategoryId);
            categoryFilter.categoryFilterAdapter.categoriesUpdated(index);
        }




    }
    @Override
    public void CategoryUpdated(long id, int index, boolean locked) {
        newOperationFragment.categoryUpdated(id, index, locked);
        categoryFilter.categoryFilterAdapter.notifyItemChanged(index);
        categoryFilter.scrollBy(0, 0);/*recyclerview-wont-update-child-until-i-scroll*/
        if(id == operationCategoryId){
            if(!locked){
                if(updaterMainFuture!=null
                        &&!updaterMainFuture.isDone()){
                    updaterMainFuture.cancel(true);
                }
                updaterMainFuture =  mOperationRepository.mExecutor.submit(operationsUpdaterAfterSaveMainRunnable);
            }else {
                operations_table_screen_locker.lockScreen();
            }
        }
    }

    /*OperDataCallback*/
    @Override
    public void updateSelectAllCancelBtnsState(int stateKey) {
        switch (stateKey) {
            case PlanData.SELECT_ALL:
                if(selectAllCancelBtn.getVisibility() == View.INVISIBLE){
                    showSelectionBtn(getString(R.string.selectAll));
                }else {
                    switchSelectionBtnTxt(getString(R.string.selectAll));
                }
                break;
            case PlanData.CANCEL:
                if(selectAllCancelBtn.getVisibility() == View.INVISIBLE){
                    showSelectionBtn(getString(R.string.cancel));
                }else {
                    switchSelectionBtnTxt(getString(R.string.cancel));
                }
                break;
            case PlanData.HIDDEN:
                if(selectAllCancelBtn.getVisibility() != View.INVISIBLE){
                    hideSelectionBtn();
                }
                break;
        }
    }

    @Override
    public void setDeleteBtnEnabled(boolean enabled) {
        operation_delete_btn.setEnabled(enabled);
    }

    @Override
    public void operTableAdapterDatasetChanged(boolean withSub) {
        mOperTableAdapter.rebindAdapters(withSub);
    }

    @Override
    public void updateOperations(OperData.LocalTotal localTotalForOperationsUpdater) {
        long categoryId = localTotalForOperationsUpdater.categoryId;
        int dateCodeFrom = localTotalForOperationsUpdater.dateCodeFrom;
        int dateCodeTo = localTotalForOperationsUpdater.dateCodeTo;
       // if(!isCurrentOperationsListActual(categoryId, dateCodeFrom, dateCodeTo)){
            operationsUpdateFuture = mOperationRepository.updateOperationsForSubRowAdapter(categoryId, dateCodeFrom, dateCodeTo);
        //}else {
            //operationsUpdated();
        //}
    }

    @Override
    public void lockScreenWhileLoadingOperations() {
        if(operationsUpdateFuture!=null && !operationsUpdateFuture.isDone()){
            cancel_operations_btn.setVisibility(View.VISIBLE);
            operations_table_screen_locker.lockScreen();
        }
    }

    @Override
    public void operationClicked(Operation operation) {
        newOperationFragment.setUpdateOperationLayout(operation);
        showNewOperationView();
    }

    @Override
    public void backButtonCollapse(boolean backBtnActionEnabled) {
        callbackBtnCollapseRowWithAnim.setEnabled(backBtnActionEnabled);
        operation_sort_btn.setEnabled(!backBtnActionEnabled);
    }

    /*OperationRepository.OperationsUpdateCallBack*/

    @Override
    public void operationsUpdated() {
        if(operationsUpdateFuture!=null){
            cancel_operations_btn.setVisibility(View.GONE);
            operations_table_screen_locker.unlockScreen();
            mOperData.setNewOperations(mOperationRepository.operationsForSubRowAdapter);
            mOperTableAdapter.continueCollapse();
            updateSubRowOperationsAnyway = false;
        }
    }

    /*NewOperationFragment.NewOperationCallback*/

    @Override
    public void cancelBtnClicked() {
        hideNewOperationView();
    }

    @Override
    public void saveUpdateOperation(long operationId, long categoryId, long initCategoryId, int day, int month, int year, long value, String description) {
        cancelOperationsLoading();
        mOperData.resetIU();
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
        cancelOperationsLoading();
        mOperData.resetIU();
        if(mDbService!=null){
            mDbService.startDeleteOperation(operationId, categoryId);
            hideNewOperationView();
        }else {
            mMainActivityCommunication.makeToast(MainActivity.DB_SERVICE_IS_NULL);
        }
    }
}
