package tech.aurorafin.aurora;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;


import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;

import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;

import android.widget.FrameLayout;
import android.widget.LinearLayout;

import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.TextView;

import tech.aurorafin.aurora.dbRoom.CategoriesRepository;
import tech.aurorafin.aurora.dbRoom.PlanRepository;
import tech.aurorafin.aurora.subscription.SubscriptionActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.maltaisn.calcdialog.CalcDialog;


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

import static tech.aurorafin.aurora.NewOperationFragment.getCalcDialog;
import static tech.aurorafin.aurora.subscription.SubscriptionActivity.isPremiumActive;


/**
 * A simple {@link Fragment} subclass.
 */
public class PlanFragment extends Fragment implements PlanData.InputManager,
        View.OnClickListener, CategoryFilterAdapter.CategoryFilterCallback, NumberPicker.OnValueChangeListener,
        CategoriesRepository.CategoriesUpdateCallback, CalcDialog.CalcDialogCallback {

    Context mContext;
    String stringPlan[];
    PlanRepository mPlanRepository;
    MainActivityCommunication mMainActivityCommunication;
    CategoriesRepository mCategoriesRepository;
    DbService mDbService;


//PLAN FILTER---------------------------------------------------------------------------------------
    // ui

    private CurtainMover planCurtainMover;
    private FrameLayout curtain_holder;
    private LinearLayout planFilter;
    private FrameLayout filter_inner_container;
    private FrameLayout category_filter_holder;
    private LinearLayout toolBarPanel;


    private AppCompatImageView toolTrack;
    private View filterShadow;
    private ScreenLocker plan_table_screen_locker;
    private AppCompatImageView planToolbarTriangle;
    private FrameLayout planToolbarCollapseBtn;
    private AppCompatImageButton planToolbarSaveBtn;
    private AppCompatImageButton plan_toolbar_back_btn;
    private AppCompatImageButton plan_toolbar_forward_btn;
    private LinearLayout plan_toolbar_forward_group;
    private LinearLayout group_save_triangle;

    private AppCompatImageButton plan_copy_btn;
    private AppCompatImageButton plan_paste_btn;
    private AppCompatImageButton plan_erase_btn;

    private NumberPicker numberPicker1;

    private RadioButton month_radio_btn;
    private RadioButton week_radio_btn;

    // values
    private int currentYear;
    private int planningYear;
    private int tempYear;

    private long planningCategoryId;
    private long tempCategoryId;


    Runnable forwardBtnR;
    ObjectAnimator animFrwdBtn;

    //Category selector
    CategoryFilter categoryFilter;




// PLAN TABLE---------------------------------------------------------------------------------------
    //ui
    RecyclerView planTableRecyclerView;
    SpecLinearLayout recyclerViewContainer;
    LinearLayout planTotal;
    LinearLayout planInput;
    PlanInpitEdit planInputEditText;
    private AppCompatImageButton plan_input_ok_btn;
    private AppCompatImageButton plan_input_plusminus_btn;
    CalcDialog calcDialog;
    TextView planTotalTextView;
    TextView selectAllCancelBtn;
    //values
    PlanData planData;
    boolean tableCollapsed = false;


    //objects
    PlanTableAdapter planTableAdapter;
    MyLinearLayoutManager llm;
    InputMethodManager imm;
    BottomNavigationView mBottomNavigationView;
    ObjectAnimator animSelectAllCancelBtn;
    OnBackPressedCallback callbackBtnDropInputSelection;
    OnBackPressedCallback callbackBtnCollapseRowWithAnim;


    /*DATA UPDATER*/
    Handler planHandler;
    private int planHandlerDelayAttempts;
    Runnable planDataUpdaterEnterRunnable;
    Runnable planDataUpdaterMainRunnable;
    Runnable planDataUpdaterFinishRunnable;
    Runnable planDataUpdaterAfterSaveMainRunnable;
    Runnable planDataUpdaterAfterSaveFinishRunnable;
    Future<?> updaterMainFuture;

    public PlanFragment(Context context,
                        BottomNavigationView bottomNavigationView,
                        CategoriesRepository categoriesRepository,
                        PlanRepository planRepository,
                        MainActivityCommunication mainActivityCommunication
                        ) {
        this.mContext = context;
        mBottomNavigationView = bottomNavigationView;
        mCategoriesRepository = categoriesRepository;
        mPlanRepository = planRepository;
        mMainActivityCommunication = mainActivityCommunication;
        imm = (InputMethodManager)(mContext.getSystemService(Context.INPUT_METHOD_SERVICE));
    }

    public void resetPlanFragment(Context context,
                        BottomNavigationView bottomNavigationView,
                        CategoriesRepository categoriesRepository,
                        PlanRepository planRepository,
                        MainActivityCommunication mainActivityCommunication
    ) {
        this.mContext = context;
        mBottomNavigationView = bottomNavigationView;
        mCategoriesRepository = categoriesRepository;
        mPlanRepository = planRepository;
        mMainActivityCommunication = mainActivityCommunication;
        imm = (InputMethodManager)(mContext.getSystemService(Context.INPUT_METHOD_SERVICE));
    }

    public void setmDbService(DbService mDbService) {
        this.mDbService = mDbService;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mContext = getContext();
        final View view = inflater.inflate(R.layout.fragment_plan, container, false);

        stringPlan = mContext.getResources().getStringArray(R.array.category_types_plan_names);
        mCategoriesRepository.setCategoriesUpdateCallback(this);

     //PLAN FILTER----------------------------------------------------------------------------------
        /*SET PLAN FILTER OBJECTS*/
        float density = mContext.getResources().getDisplayMetrics().density;
        //oneDp = (int)(density +0.5f);

        curtain_holder = view.findViewById(R.id.curtain_holder);
        toolBarPanel = view.findViewById(R.id.toolBarPanel);
        planFilter = view.findViewById(R.id.planFilter);
        filter_inner_container = view.findViewById(R.id.filter_inner_container);
        category_filter_holder = view.findViewById(R.id.category_filter_holder);
        categoryFilter = new CategoryFilter(mContext, false, mCategoriesRepository.categories, this);
        RecyclerView.LayoutParams layoutParams= new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT);
        categoryFilter.setLayoutParams(layoutParams);
        category_filter_holder.addView(categoryFilter);
        planToolbarCollapseBtn = view.findViewById(R.id.toolbar_collapse_btn);
        toolTrack = view.findViewById(R.id.toolTrack);
        filterShadow = view.findViewById(R.id.filterShadow);
        planToolbarTriangle = view.findViewById(R.id.plan_toolbar_triangle);

        planCurtainMover = new CurtainMover(mContext, toolTrack, curtain_holder, planFilter,
                toolBarPanel, filter_inner_container, filterShadow, planToolbarTriangle, planToolbarCollapseBtn);

        numberPicker1  = view.findViewById(R.id.numberPicker1);
        numberPicker1.setMaxValue(2100);
        numberPicker1.setMinValue(1900);
        currentYear = Calendar.getInstance().get(Calendar.YEAR);
        if(mPlanRepository.tempPlannedYear !=- 1 ){
            planningYear = mPlanRepository.tempPlannedYear;
        }else {
            planningYear = currentYear;
        }
        tempYear = planningYear;
        numberPicker1.setValue(planningYear);
        numberPicker1.setOnValueChangedListener(this);

        month_radio_btn = view.findViewById(R.id.month_radio_btn);
        week_radio_btn = view.findViewById(R.id.week_radio_btn);
        month_radio_btn.setOnClickListener(this);
        week_radio_btn.setOnClickListener(this);

        plan_table_screen_locker = view.findViewById(R.id.plan_table_screen_locker);

        planToolbarSaveBtn = view.findViewById(R.id.plan_toolbar_save_btn);
        planToolbarSaveBtn.setEnabled(false);
        plan_toolbar_back_btn = view.findViewById(R.id.plan_toolbar_back_btn);
        plan_toolbar_forward_btn = view.findViewById(R.id.plan_toolbar_forward_btn);
        plan_toolbar_forward_group = view.findViewById(R.id.plan_toolbar_forward_group);
        group_save_triangle = view.findViewById(R.id.group_save_triangle);
        plan_copy_btn = view.findViewById(R.id.plan_copy_btn);
        plan_paste_btn = view.findViewById(R.id.plan_paste_btn);
        plan_erase_btn = view.findViewById(R.id.plan_erase_btn);


        forwardBtnR = new Runnable() {
            public void run() {
                //do your stuff here after DELAY milliseconds
                animFrwdBtn.start();
            }
        };

        /*SET PLAN FILTER LISTENERS*/
        planToolbarSaveBtn.setOnClickListener(this);
        plan_toolbar_back_btn.setOnClickListener(this);
        plan_toolbar_forward_btn.setOnClickListener(this);
        plan_copy_btn.setOnClickListener(this);
        plan_paste_btn.setOnClickListener(this);
        plan_erase_btn.setOnClickListener(this);

        plan_toolbar_back_btn.setEnabled(false);
        plan_toolbar_forward_btn.setEnabled(false);
        plan_copy_btn.setEnabled(false);
        plan_paste_btn.setEnabled(false);
        plan_erase_btn.setEnabled(false);

     //PLAN TABLE ----------------------------------------------------------------------------------
        /*SET PLAN TABLE OBJECTS*/
        planData = new PlanData(this);
        planData.weekPresentation = mPlanRepository.tempWeekPresentation;
        month_radio_btn.setChecked(!planData.weekPresentation);
        week_radio_btn.setChecked(planData.weekPresentation);


        planTableRecyclerView = view.findViewById(R.id.planTable);
        recyclerViewContainer = view.findViewById(R.id.recyclerViewContainer);
        llm = new MyLinearLayoutManager(getContext());
        planTableRecyclerView.setLayoutManager(llm);
        planTableAdapter = new PlanTableAdapter(getContext(), planData, this, recyclerViewContainer, llm);
        planTableRecyclerView.setAdapter(planTableAdapter);
        planTableRecyclerView.setHasFixedSize(true);
                // Removes blinks
        ((SimpleItemAnimator) Objects.requireNonNull(planTableRecyclerView.getItemAnimator())).setSupportsChangeAnimations(false);
        planTotalTextView  = view.findViewById(R.id.planTotalTextView);
        planTotalTextView.setText(planData.getBtrValue());
        planTotal = view.findViewById(R.id.planTotal);
        planInput = view.findViewById(R.id.planInput);
        planInputEditText = view.findViewById(R.id.planInputEditText);
        planInputEditText.setFilters(new InputFilter[]{ new InputFilterMinMax(-1000000000000.00, 1000000000000.00)});
        planInputEditText.setmInputManager(this);
        plan_input_ok_btn = view.findViewById(R.id.plan_input_ok_btn);
        plan_input_plusminus_btn = view.findViewById(R.id.plan_input_plusminus_btn);
        calcDialog = getCalcDialog();
        recyclerViewContainer.setmInputManager(this);
        selectAllCancelBtn = view.findViewById(R.id.selectAllCancelBtn);
        /*SET PLAN TABLE LISTENERS*/
        plan_input_ok_btn.setOnClickListener(this);
        selectAllCancelBtn.setOnClickListener(this);
        plan_input_plusminus_btn.setOnClickListener(this);


        // BACK BTN CALLBACKS
            callbackBtnCollapseRowWithAnim = new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    planTableAdapter.minimizeWithAnimation();
                }
            };

            callbackBtnDropInputSelection = new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    if(planData.isSelectionMode()){
                        planData.dropSelection();
                    }else if(planInput.getVisibility() == View.VISIBLE){
                        deactivateInputLayout();
                        planTableAdapter.deactivateInputModeWithHandler();
                    }
                }
            };
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callbackBtnCollapseRowWithAnim);
            requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callbackBtnDropInputSelection);

        /*DATA UPDATER*/
            planHandler = new Handler(Looper.getMainLooper());
            planHandlerDelayAttempts = 4;

            planDataUpdaterEnterRunnable = new Runnable() {
                public void run() {
                    if(tempCategoryId !=-1 &&
                            (planningYear != tempYear || planningCategoryId != tempCategoryId)){
                        planningYear = tempYear;
                        planningCategoryId = tempCategoryId;
                        /*LockScreen handle*/
                        if(mCategoriesRepository.isCategoryLocked(planningCategoryId)){
                            plan_table_screen_locker.lockScreen();
                        }else {
                            plan_table_screen_locker.unlockScreen();
                        }
                        /*Interrupt current Update */
                        if(updaterMainFuture!=null
                                &&!updaterMainFuture.isDone()){
                            updaterMainFuture.cancel(true);
                        }
                         updaterMainFuture =  mPlanRepository.mExecutor.submit(planDataUpdaterMainRunnable);

                    }
                }
            };

            planDataUpdaterMainRunnable = new Runnable() {
                public void run() {
                    planData.resetAndMapSafe(mPlanRepository.getPlan(planningCategoryId, planningYear),
                            planningCategoryId, planningYear );
                    planHandler.post(planDataUpdaterFinishRunnable);

                }
            };

            planDataUpdaterFinishRunnable = new Runnable() {
                public void run() {
                        planToolbarSaveBtn.setEnabled(false);
                        planData.resetIU();
                        planTableAdapter.minimize();
                        planTableAdapter.notifyDataSetChanged();
                        planTotalTextView.setText(planData.getBtrValue());
                        if(mPlanRepository.canCollapseRow){
                            if(mPlanRepository.tempCollapsedRow!=-1){
                                planHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        llm.scrollToPositionWithOffset(mPlanRepository.tempCollapsedRow, 10);
                                        RecyclerView.ViewHolder v = planTableRecyclerView.findViewHolderForAdapterPosition(mPlanRepository.tempCollapsedRow);
                                        if(v != null){
                                            planTableAdapter.maximize(mPlanRepository.tempCollapsedRow);
                                        }else {
                                            if(planHandlerDelayAttempts !=0 ){
                                                planHandlerDelayAttempts = planHandlerDelayAttempts -1;
                                                planHandler.postDelayed(this, 100);
                                            }

                                        }
                                    }
                                },10);
                            }
                            mPlanRepository.canCollapseRow = false;
                        }
                }
            };

            planDataUpdaterAfterSaveMainRunnable  = new Runnable() {
                public void run() {
                    planData.resetAndMapSafe(mPlanRepository.getPlan(planningCategoryId, planningYear),
                            planningCategoryId, planningYear );
                    planHandler.post(planDataUpdaterAfterSaveFinishRunnable);
                }
            };

            planDataUpdaterAfterSaveFinishRunnable  = new Runnable() {
                public void run() {
                    planTableAdapter.rebindAdapters(true);
                    planTotalTextView.setText(planData.getBtrValue());
                    plan_table_screen_locker.unlockScreen();
                }
            };


        /*DefaultCategorySetter*/
        defaultCategorySetter();


        // Inflate the layout for this fragment
        return view;
    }

    private void defaultCategorySetter(){
        tempCategoryId = mPlanRepository.tempPlannedCategoryId;
        int index = -1;
        if(tempCategoryId != - 1){
            index =  mCategoriesRepository.indexOfCategory(tempCategoryId);
        }
        if(index == -1){
            tempCategoryId = mCategoriesRepository.getFirstCategoryId();
            index = mCategoriesRepository.indexOfCategory(tempCategoryId);
            mPlanRepository.canCollapseRow = false;
        }
        planningCategoryId = tempCategoryId;
        if(planningCategoryId != -1){
            if(index != -1){
                String s = stringPlan[mCategoriesRepository.categories.get(index).type] +" - " + mCategoriesRepository.categories.get(index).name;
                mMainActivityCommunication.setAppbarText(s);
                categoryFilter.setSelectedCategory(index);
                if(mCategoriesRepository.isCategoryLocked(planningCategoryId)){
                    plan_table_screen_locker.lockScreen();
                }
            }
            updaterMainFuture =  mPlanRepository.mExecutor.submit(planDataUpdaterMainRunnable);
        } else {
            //Categories not loaded? it will be a callback, or there is no categories created yet
            mMainActivityCommunication.setAppbarText(getResources().getString(R.string.plan));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPlanRepository.tempPlannedCategoryId = planningCategoryId;
        mPlanRepository.tempPlannedYear = planningYear;
        mPlanRepository.tempCollapsedRow = planTableAdapter.collapsedHolderPos;
        mPlanRepository.tempWeekPresentation = planData.weekPresentation;
    }


    //-----------------------BUTTONS CLICK--------------------------
    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {

            case R.id.plan_toolbar_save_btn:
                savePlan();
                break;

            case R.id.plan_toolbar_back_btn:
                toolbarBackBtn();
                break;

            case R.id.plan_toolbar_forward_btn:
                toolbarForwardBtn();
                break;

            case R.id.plan_input_ok_btn:
                deactivateInputLayout();
                planTableAdapter.deactivateInputModeWithHandler();
                break;

            case R.id.selectAllCancelBtn:
                SelectAllCancelBtnClick();
                break;

            case R.id.plan_erase_btn:
                EraseBtnClick();
                break;

            case R.id.plan_copy_btn:
                CopyBtnClick();
                break;

            case R.id.plan_paste_btn:
                PasteBtnClick();
                break;

            case R.id.plan_input_plusminus_btn:
                PlusminusBtnClick();
                break;
            case R.id.week_radio_btn:
                onGroupPeriodRadioButtonClicked(true);
                break;
            case R.id.month_radio_btn:
                onGroupPeriodRadioButtonClicked(false);
                break;
        }
    }


    public void onGroupPeriodRadioButtonClicked(boolean weekPresentation) {
        planTableAdapter.minimize();
        planData.switchPresentationMode(weekPresentation);
        planTableAdapter.notifyDataSetChanged();
        //planTableAdapter.rebindSubRVAdapter();
    }

    @Override
    public void onValueChange(NumberPicker numberPicker, int i, int i1) {
        tempYear = i1;
        runUpdater(200);

    }

    //Category click
    @Override
    public void CategoryClicked(int position, int type, long id) {
        String s = stringPlan[type] +" - " + mCategoriesRepository.categories.get(position).name;
        ((MainActivity) requireActivity()).setAppBarText(s);
        tempCategoryId = id;
        runUpdater(100);
    }



    public void runUpdater(int delay){
        planHandler.removeCallbacks(planDataUpdaterEnterRunnable);
        planHandler.postDelayed(planDataUpdaterEnterRunnable, delay);
    }


    /*-----CURTAIN MOVER----------------------------------------------------------------------*/

    public static class CurtainMover implements View.OnTouchListener ,View.OnClickListener, GestureDetector.OnGestureListener{

        private AppCompatImageView mToolTrack;
        private FrameLayout mCurtain_holder;
        private LinearLayout mFilter;
        private LinearLayout mToolBarPanel;
        private FrameLayout mFilter_inner_container;
        private View mFilterShadow;
        private AppCompatImageView mToolbarTriangle;
        private FrameLayout mToolbarCollapseBtn;

        private GestureDetectorCompat gestureDetector;
        private LinearOutSlowInInterpolator linearOutSlowInInterpolator;
        private AccelerateDecelerateInterpolator accelerateDecelerateInterpolator;
        private ValueAnimator filterAnimation;

        private int oneDp;
        private int maxCurtainHolderTop;
        private int maxCurtainHolderBot;
        private int minCurtainHolderBot;
        private int planScrollDirection = 0;
        private float dY;
        private final float filterShadowAlpha = 0.15f;
        private List<Float> yMotions;
        private Long yMotionsTimeStamp;

        public CurtainMover(Context context,  AppCompatImageView toolTrack, FrameLayout curtain_holder, LinearLayout filter,
                            LinearLayout toolBarPanel, FrameLayout filter_inner_container, View filterShadow,
                            AppCompatImageView toolbarTriangle, FrameLayout toolbarCollapseBtn) {

            this.mCurtain_holder = curtain_holder;
            this.mFilter =filter;
            this.mToolBarPanel = toolBarPanel;
            this.mFilter_inner_container = filter_inner_container;
            this.mFilterShadow = filterShadow;
            mFilterShadow.setOnClickListener(this);
            mFilterShadow.setClickable(false);
            this.mToolbarTriangle = toolbarTriangle;
            this.mToolbarCollapseBtn = toolbarCollapseBtn;
            mToolbarCollapseBtn.setOnClickListener(this);


            this.gestureDetector = new GestureDetectorCompat(context,this);
            gestureDetector.setIsLongpressEnabled(false);
            this.mToolTrack = toolTrack;
            this.mToolTrack.setOnTouchListener(this);

            this.yMotions = new ArrayList<>();
            yMotionsTimeStamp = null;

            float density = context.getResources().getDisplayMetrics().density;
            oneDp = (int)(density + 0.5f);

            linearOutSlowInInterpolator = new LinearOutSlowInInterpolator();
            accelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
            filterAnimation = ValueAnimator.ofFloat();

            filterAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    // get the value the interpolator is at
                    float value = (float) animation.getAnimatedValue();
                    movePlanFilter(value - mCurtain_holder.getBottom());
                    mFilter.invalidate();
                }
            });

            ((FrameLayout.MarginLayoutParams)mCurtain_holder.getLayoutParams()).topMargin = -oneDp;


        }

        private void collapseFilter(){
            this.recalculatePlanFilterConsts();
            if (mCurtain_holder.getBottom()  == maxCurtainHolderBot) {
                this.finishMovePlanFilterAnimation(minCurtainHolderBot, 500, accelerateDecelerateInterpolator);
            }else {
                this.finishMovePlanFilterAnimation(maxCurtainHolderBot, 500, accelerateDecelerateInterpolator);
            }
        }

        private void movePlanFilter(float transitionAddition){
            /*container moving*/
            int addition = (int)transitionAddition;
            int curtainHolderBot = mCurtain_holder.getBottom() + addition;
            if(curtainHolderBot < minCurtainHolderBot){
                curtainHolderBot = minCurtainHolderBot;
            }else if(curtainHolderBot > maxCurtainHolderBot){
                curtainHolderBot = maxCurtainHolderBot;
            }
            int topAddition = -oneDp;
            if(curtainHolderBot < minCurtainHolderBot*15){
                if(curtainHolderBot != minCurtainHolderBot){
                    topAddition = (curtainHolderBot - minCurtainHolderBot)/25 -oneDp;
                }
            }else{
                topAddition = maxCurtainHolderTop;
            }
            if(topAddition > maxCurtainHolderTop){
                topAddition = maxCurtainHolderTop;
            }
            mCurtain_holder.layout(mCurtain_holder.getLeft(), topAddition, mCurtain_holder.getRight(), curtainHolderBot);
            mCurtain_holder.getLayoutParams().height = mCurtain_holder.getHeight();
            ((FrameLayout.MarginLayoutParams)mCurtain_holder.getLayoutParams()).topMargin = mCurtain_holder.getTop();
            float progress =  (float) (curtainHolderBot - minCurtainHolderBot)/(float)(maxCurtainHolderBot - minCurtainHolderBot);
            //Log.d("MyTag", Float.toString(progress));
            float newAlfa = filterShadowAlpha*2f*(progress);
            newAlfa = (newAlfa >= filterShadowAlpha) ? filterShadowAlpha : newAlfa;
            mFilterShadow.setAlpha(newAlfa);
            float newAngle =  ((1-progress)*-60);
            mToolbarTriangle.setRotation(newAngle);
            if(progress>0.4f){
                float z = (progress-0.4f);
                mFilter_inner_container.setAlpha(z + (z/3)*2);
            }else{
                mFilter_inner_container.setAlpha(0f);
            }
            if(progress < 0.2){
                mFilterShadow.setClickable(false);
            }else {
                mFilterShadow.setClickable(true);
            }
        }

        private void finishMovePlanFilterAnimation(float finalBottom, int duration, Interpolator interpolator){
            filterAnimation.setFloatValues(mCurtain_holder.getBottom(), finalBottom);
            filterAnimation.setDuration(duration);
            filterAnimation.setInterpolator(interpolator);
            filterAnimation.start();
        }

        private boolean onTrackTouch(View view, MotionEvent motionEvent){
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    this.yMotions.clear();
                    yMotionsTimeStamp = null;
                    filterAnimation.cancel();
                    dY = motionEvent.getRawY();
                    this.recalculatePlanFilterConsts();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaY = motionEvent.getRawY()-dY;
                    this.movePlanFilter(deltaY);
                    updateMotionTracker(deltaY);
                    dY = motionEvent.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                    finishMovePlanFilter(getPlanScrollDirection());
                    break;
            }
            return true;
        }

        private void updateMotionTracker(float deltaY){
            if(this.yMotions.size() == 3){
                this.yMotions.remove(0);
            }
            this.yMotions.add(deltaY);
            yMotionsTimeStamp = System.currentTimeMillis();

            //Log.d("MyTag", "Date(System.currentTimeMillis()).getTime(); = " + yMotionsTimeStamp.toString());

        }

        private int getPlanScrollDirection(){
            if(this.yMotions.size()==0 || yMotionsTimeStamp == null){
                return 0;
            }else {
                long ct = System.currentTimeMillis();
                if((ct - yMotionsTimeStamp)/1000 > 1){
                    return 0;
                }else {
                    float ttlSum = 0;
                    for(int i = 0; i < this.yMotions.size(); i++){
                        ttlSum += this.yMotions.get(i);
                    }
                    float aver = ttlSum/this.yMotions.size();
                    if(aver==0){
                        return 0;
                    }else if(aver>0){
                        return 2;
                    }else {
                        return 1;
                    }
                }
            }
        }

        private void finishMovePlanFilter(int direction){
            float progress =(float) (mCurtain_holder.getBottom() - minCurtainHolderBot)/(float)(maxCurtainHolderBot - minCurtainHolderBot);
            if(progress != 1 && progress != 0){
                if (direction == 0){
                    if (progress < 0.5){
                        direction = 1; // Animation UP
                    }else {
                        direction = 2; // Animation DOWN
                    }
                }
                int time = 300;
                if (direction == 1){
                    int duration = (int) (300+(progress)*time);
                    finishMovePlanFilterAnimation(minCurtainHolderBot, duration, linearOutSlowInInterpolator);
                    // Log.d("MyTag", "Direction: "+Integer.toString(direction));
                }else if (direction == 2){
                    int duration = (int) (300+(1-progress)*time);
                    finishMovePlanFilterAnimation(maxCurtainHolderBot, duration, linearOutSlowInInterpolator);
                    // Log.d("MyTag", "Direction: "+Integer.toString(direction));
                }
            }
        }

        private void recalculatePlanFilterConsts(){
            planScrollDirection = 0;
            maxCurtainHolderTop = (int)(mToolTrack.getHeight()*0.35f);
            maxCurtainHolderBot = mFilter.getHeight() + maxCurtainHolderTop;
            minCurtainHolderBot = mToolBarPanel.getHeight() - oneDp;
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            switch (id){
                case R.id.filterShadow:
                    collapseFilter();
                    break;

                case R.id.toolbar_collapse_btn:
                    collapseFilter();
                    break;
            }

        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            /*if(gestureDetector.onTouchEvent(motionEvent)){
                return true;
            }*/
            return onTrackTouch(view, motionEvent);
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }
        @Override
        public void onShowPress(MotionEvent motionEvent) {
        }
        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return false;
        }
        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            //Log.d("MyTag", "onScroll");
            if(v1>0){
                planScrollDirection = 2;
            }else {
                planScrollDirection = 1;
            }
            return false;
        }
        @Override
        public void onLongPress(MotionEvent motionEvent) {}
        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            //Log.d("MyTag", "onFling");
            finishMovePlanFilter(planScrollDirection);
            return true;
        }

    }



//----------------------------------------------------------------------------

//-----------------------TOOLBAR BUTTONS FUNCTIONS START----------------------




    private void savePlan(){
        planToolbarSaveBtn.setEnabled(false);
        planData.resetIU();

        if(mDbService!=null){
            mDbService.startSavingPlan(planData.planDays, planData.overAllTotal, planningCategoryId, planningYear);
        }else {
            mMainActivityCommunication.makeToast(MainActivity.DB_SERVICE_IS_NULL);
        }

    }

    @Override
    public void CategoriesUpdated() {
        if(planningCategoryId == -1){
            defaultCategorySetter();
        }else {
            int index = mCategoriesRepository.indexOfCategory(planningCategoryId);
            categoryFilter.categoryFilterAdapter.categoriesUpdated(index);
        }
    }

    @Override
    public void CategoryUpdated(long id, int index, boolean locked) {
        categoryFilter.categoryFilterAdapter.notifyItemChanged(index);
        categoryFilter.scrollBy(0, 0);/*recyclerview-wont-update-child-until-i-scroll*/
        if(id == planningCategoryId){
            if(!locked){
                if(updaterMainFuture!=null
                        &&!updaterMainFuture.isDone()){
                    updaterMainFuture.cancel(true);
                }
                updaterMainFuture =  mPlanRepository.mExecutor.submit(planDataUpdaterAfterSaveMainRunnable);
            }else {
                plan_table_screen_locker.lockScreen();
            }
        }
    }

    @Override
    public boolean isCategoryLocked(long id) {
        return mCategoriesRepository.isCategoryLocked(id);
    }




    /*PLUS MINUS -------------------------*/
    private void PlusminusBtnClick(){
        if(!isPremiumActive(mContext)){
            Intent intent1 = new Intent(mContext, SubscriptionActivity.class);
            mContext.startActivity(intent1);
        }else {
            int d = planInputEditText.planDayToUpdate;
            if (d != -1) {
                long v = planData.planDays.get(d).value;
                /*if(v !=0 ){
                    planData.planDays.get(d).setNewValue(d, -v, true);
                    planInputEditText.setText(planData.getPlanDayDotValue(d));
                    planTableAdapter.updateTotals();
                    planTotalTextView.setText(planData.getBtrValue());
                }*/
                BigDecimal f;
                try {
                    f = new BigDecimal(PlanData.longToStringWithDot(v));
                } catch (Exception e) {
                    f = new BigDecimal(0);
                }
                calcDialog.getSettings().setInitialValue(f);
                hideSoftKeyboard();
                calcDialog.show(getChildFragmentManager(), "calc_dialog");
            }
        }
    }

    @Override
    public void onValueEntered(int requestCode, @Nullable BigDecimal value) {
        int d = planInputEditText.planDayToUpdate;
        if(d != -1){
            DecimalFormat df = new DecimalFormat("###.00");
            long v = PlanData.stringNumToLong(value != null ? df.format(value).replace(',','.') : "0");
            planData.planDays.get(d).setNewValue(d, v, true);
            planInputEditText.setText(planData.getPlanDayDotValue(d));
            planTableAdapter.updateTotals();
            planTotalTextView.setText(planData.getBtrValue());
        }
    }

    /*COPY PASTE-------------------------*/
    private void PasteBtnClick(){
        planData.pasteAction();
        planTableAdapter.rebindAdapters(true);
        planTotalTextView.setText(planData.getBtrValue());
        if(planInputEditText.planDayToUpdate !=-1){
            planInputEditText.setText(planData.getPlanDayDotValue(planInputEditText.planDayToUpdate ));
        }
    }


    private void CopyBtnClick(){
        planData.copyAction();
    }

    /*ERASER-------------------------*/
    private void EraseBtnClick(){
        planData.eraseAction();
        planTableAdapter.rebindAdapters(true);
        planTotalTextView.setText(planData.getBtrValue());
        if(planInputEditText.planDayToUpdate !=-1){
            planInputEditText.setText(planData.getPlanDayDotValue(planInputEditText.planDayToUpdate ));
        }
    }


    /*UNDO REDO-------------------------*/
    private void toolbarBackBtn(){
        planData.executeUndoAction();
        planTableAdapter.rebindAdapters(true);
        planTotalTextView.setText(planData.getBtrValue());

        if(planInputEditText.planDayToUpdate !=-1){
            planInputEditText.setText(planData.getPlanDayDotValue(planInputEditText.planDayToUpdate));
        }
    }

    private void toolbarForwardBtn(){
        planData.executeRedoAction();
        planTableAdapter.rebindAdapters(true);
        planTotalTextView.setText(planData.getBtrValue());
        if(planInputEditText.planDayToUpdate !=-1){
            planInputEditText.setText(planData.getPlanDayDotValue(planInputEditText.planDayToUpdate ));
        }
    }

    private void hideToolbarForwardBtn(){
        ObjectAnimator anim = ObjectAnimator.ofFloat(plan_toolbar_forward_group, "alpha", 0);
        anim.setDuration(100);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                plan_toolbar_forward_group.setVisibility(View.INVISIBLE);
                group_save_triangle.animate()
                        .translationX(0)
                        .setDuration(200);
            }
        });
        anim.start();
    }

    public void changeForwardBtnState(boolean newState){
        if(newState != plan_toolbar_forward_btn.isEnabled()){
            plan_toolbar_forward_btn.setEnabled(newState);
            planHandler.removeCallbacks(forwardBtnR);
            if(newState){

                if(plan_toolbar_forward_group.getAlpha() != 1f){

                    plan_toolbar_forward_group.setAlpha(0f);
                    //plan_toolbar_forward_group.setVisibility(View.VISIBLE);
                    float tX = (float) group_save_triangle.getWidth() / 2;
                    animFrwdBtn = ObjectAnimator.ofFloat(group_save_triangle, "translationX", tX);
                    animFrwdBtn.setDuration(300);
                    animFrwdBtn.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            plan_toolbar_forward_group.setVisibility(View.VISIBLE);
                            plan_toolbar_forward_group.animate()
                                    .alpha(1)
                                    .setDuration(70);
                        }
                    });
                    planHandler.postDelayed(forwardBtnR, 300);
                }

            }else {
                animFrwdBtn = ObjectAnimator.ofFloat(plan_toolbar_forward_group, "alpha", 0);
                animFrwdBtn.setDuration(100);
                animFrwdBtn.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        plan_toolbar_forward_group.setVisibility(View.INVISIBLE);
                        group_save_triangle.animate()
                                .translationX(0)
                                .setDuration(200);
                    }
                });
                planHandler.postDelayed(forwardBtnR, 2000);
            }
        }
    }

    /*SELECTOR BUTTONS -------------------*/

    public void SelectAllCancelBtnClick(){
        planData.selectAllCancel();
    }

    public void showSelectionBtn(String txt){
        selectAllCancelBtn.setAlpha(0);
        selectAllCancelBtn.setVisibility(View.VISIBLE);
        selectAllCancelBtn.setText(txt);
        animSelectAllCancelBtn = ObjectAnimator.ofFloat(selectAllCancelBtn, "alpha", 1);
        animSelectAllCancelBtn.setDuration(200);
        animSelectAllCancelBtn.start();
        callbackBtnDropInputSelection.setEnabled(true);
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
        callbackBtnDropInputSelection.setEnabled(false);
    }

    private void lockFilterView(){
        planToolbarTriangle.setEnabled(false);
        planToolbarCollapseBtn.setEnabled(false);
        toolTrack.setEnabled(false);
        planTableAdapter.lockCollapsedRow();
    }

    private void unlockFilterView(){
        planToolbarTriangle.setEnabled(true);
        planToolbarCollapseBtn.setEnabled(true);
        toolTrack.setEnabled(true);
        planTableAdapter.unlockCollapsedRow();
    }

//----------------------------------------------------------------------------

//-----------------------TABLE VIEW INPUT MANAGER INTERFACE----------------------
    @Override
    public void activateInputLayout() {

        if(planInput.getVisibility() != View.VISIBLE){

            mBottomNavigationView.setVisibility(View.GONE);
            mBottomNavigationView.setAlpha(0);
            planInput.setVisibility(View.VISIBLE);
            planInput.setAlpha(0);
            planInput.animate()
                    .alpha(1)
                    .setDuration(300);
            planInputEditText.requestFocus();
            recyclerViewContainer.setNeedUpdate(true);


            try {
                imm.showSoftInput(planInputEditText, InputMethodManager.SHOW_IMPLICIT);
            }catch (Exception e){
                e.printStackTrace();
            }
            callbackBtnDropInputSelection.setEnabled(true);

        }
    }

    @Override
    public void deactivateInputLayout() {
        try {
            imm.hideSoftInputFromWindow(planInputEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }catch (Exception e){
            e.printStackTrace();
        }

        ObjectAnimator anim = ObjectAnimator.ofFloat(planInput, "alpha", 0f);
        anim.setDuration(100);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                planInput.setVisibility(View.GONE);
                mBottomNavigationView.setVisibility(View.VISIBLE);
                unlockFilterView();
                mBottomNavigationView.animate()
                        .alpha(1)
                        .setDuration(100);
            }
        });
        anim.start();
        callbackBtnDropInputSelection.setEnabled(false);
    }

    private void hideSoftKeyboard(){
        try {
            imm.hideSoftInputFromWindow(planInputEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void setPlanInputEditDay(int planDay) {
        planInputEditText.setPlanDayToUpdate(planDay);
        if (planDay != -1) {
            planInputEditText.setNeedUpdate(false);
            planInputEditText.setText(planData.getPlanDayDotValue(planDay));
            planInputEditText.setNeedUpdate(true);
        }
    }

    @Override
    public void updateSubRvHeight(int winHeight) {
        planTableAdapter.updateSubRvHeight(winHeight);
    }

    @Override
    public void setPlanDayString(int planDay, String s) {
        planData.setPlanDayString(planDay, s);
        planTableAdapter.updateTotals();
        planTotalTextView.setText(planData.getBtrValue());
    }

    @Override
    public void updateUndoRedoBtnsState(int stateKey) {
        switch (stateKey) {
            case PlanData.NO_UNDO_NO_REDO:
                plan_toolbar_back_btn.setEnabled(false);
                changeForwardBtnState(false);
                break;
            case PlanData.UNDO_NO_REDO:
                plan_toolbar_back_btn.setEnabled(true);
                changeForwardBtnState(false);
                break;
            case PlanData.NO_UNDO_REDO:
                plan_toolbar_back_btn.setEnabled(false);
                changeForwardBtnState(true);
                break;
            case PlanData.UNDO_REDO:
                plan_toolbar_back_btn.setEnabled(true);
                changeForwardBtnState(true);
                break;
        }
    }

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
    public void planTableAdapterDatasetChanged(boolean withSub) {
        planTableAdapter.rebindAdapters(withSub);
    }

    @Override
    public void rebindSubRVAdapter() {
        planTableAdapter.rebindSubRVAdapter();
    }

    @Override
    public void setEraseBtnEnabled(boolean enabled) {
        plan_erase_btn.setEnabled(enabled);
    }

    @Override
    public void setCopyPasteBtnEnabled(boolean copyEnabled, boolean pasteEnabled) {
        plan_copy_btn.setEnabled(copyEnabled);
        plan_paste_btn.setEnabled(pasteEnabled);
    }

    @Override
    public void makeCopiedToast() {
    /*  Toast toast = Toast.makeText(getContext(), getString(R.string.copied), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP|Gravity.START, 0, 0);
        toast.show();*/
    }

    @Override
    public void lockFilterAndCollapsedRow() {
        lockFilterView();
    }

    @Override
    public void setSaveButtonEnabled(boolean enabled) {
        if(mDbService!=null){
            planToolbarSaveBtn.setEnabled(enabled);
        }else {
            mMainActivityCommunication.makeToast(MainActivity.DB_SERVICE_IS_NULL);
        }
    }

    @Override
    public String[] getMonths() {
        return getResources().getStringArray(R.array.months);
    }

    @Override
    public String[] getShortMonths() {
        return getResources().getStringArray(R.array.short_months);
    }

    @Override
    public String[] getWeekDays() {
        return getResources().getStringArray(R.array.week_days);
    }

    @Override
    public String getWeek() {
        return getResources().getString(R.string.week);
    }

    @Override
    public String getYear() {
        return getResources().getString(R.string.year);
    }

    @Override
    public void backButtonCollapse(boolean enabled) {
        callbackBtnCollapseRowWithAnim.setEnabled(enabled);
    }


    //----------------------------------------------------------------------

}
