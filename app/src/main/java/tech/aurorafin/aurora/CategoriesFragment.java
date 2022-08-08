package tech.aurorafin.aurora;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;


import tech.aurorafin.aurora.dbRoom.Aggregator;
import tech.aurorafin.aurora.dbRoom.CategoriesRepository;
import tech.aurorafin.aurora.dbRoom.CategoriesRepository.ACategory;
import tech.aurorafin.aurora.dbRoom.Category;
import tech.aurorafin.aurora.subscription.SubscriptionActivity;

import java.util.ArrayList;
import java.util.List;

import static tech.aurorafin.aurora.MainActivity.RATE_FILE_KEY;
import static tech.aurorafin.aurora.MainActivity.RATE_STATE_KEY;
import static tech.aurorafin.aurora.subscription.SubscriptionActivity.isPremiumActive;

public class CategoriesFragment extends Fragment implements View.OnClickListener, View.OnTouchListener,
        CategoriesRepository.CategoriesUpdateCallback, CategorySelectorAdapter.CategorySelectorCallback {

    /*GENERAL*/
    private Context mContext;
    InputMethodManager imm;
    OnBackPressedCallback callbackBtn;
    public List<ACategory> filteredCategories;
    public List<ACategory> oldCategories;
    CategorySelector.ACategoryDiffUtilCallback aCategoryDiffUtilCallback;

    MainActivityCommunication mMainActivityCommunication;
    private CategoriesRepository mCategoriesRepository;

    DbService mDbService;

    public void setmDbService(DbService mDbService) {
        this.mDbService = mDbService;
    }

    /*MAIN CATEGORY VIEW*/
    private AppCompatToggleButton toggle_exp;
    private AppCompatToggleButton toggle_rev;
    private AppCompatToggleButton toggle_cap;
    private FrameLayout category_selector_holder;
    private TextView category_add_btn;
    private LinearLayout main_category_view;
    private CategorySelector mCategorySelector;

    /*NEW UPDATE*/
    private AppCompatToggleButton type_toggle_btns[];
    private TableRow aggregator_selector;
    private TableRow active_cat_selector;
    private EditText category_full_name;
    private EditText category_short_name;
    private boolean needUpdateShortName = true;
    private Spinner aggregator_spinner;
    private LinearLayout new_update_view;
    private TextView update_save_btn;
    private TextView cancel_btn;
    private SpinnerAggregatorAdapter spinnerAdapter;
    private TextView name_text_view;
    private TextView nick_text_view;
    private AppCompatCheckBox active_cat_chekbox;
    private TextView new_update_view_header;
    int txtTableColor;
    int txtRedColor;
    long editCategoryId = -1;
    private AppCompatImageButton category_delete_btn;
    AlertDialog deleteDialog;
    private TableLayout new_update_input_table;
    private int lastCatSelected = 1;

    /*ANIMATIONS*/

        private AnimatorListenerAdapter afterNewUpdateViewShown;
        private AnimatorListenerAdapter afterNewUpdateViewHidden;

        TransitionAnimator showNewUpdateView;
        TransitionAnimator hideNewUpdateView;

        boolean isAnimating = false;

        private NewValidatorAnimator nameLabelValidatorAnim;
        private NewValidatorAnimator nameEditValidatorAnim;
        private NewValidatorAnimator nickLabelValidatorAnim;
        private NewValidatorAnimator nickEditValidatorAnim;

        private LayoutTransition mLayoutTransition = new LayoutTransition();


    public CategoriesFragment(Context context, CategoriesRepository categoriesRepository, MainActivityCommunication mainActivityCommunication) {
        mCategoriesRepository = categoriesRepository;
        this.mContext = context;
        mMainActivityCommunication = mainActivityCommunication;

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_categories, container, false);

       // mCategoryData = new CategoryData(mAggregatorsRepository);
        mCategoriesRepository.setCategoriesUpdateCallback(this);
        filteredCategories = new ArrayList<>();
        oldCategories = new ArrayList<>();
        aCategoryDiffUtilCallback = new CategorySelector.ACategoryDiffUtilCallback(oldCategories, filteredCategories);
        imm = (InputMethodManager)(mContext.getSystemService(Context.INPUT_METHOD_SERVICE));

        /*MAIN CATEGORY VIEW*/
            toggle_exp = view.findViewById(R.id.toggle_exp);
            toggle_rev = view.findViewById(R.id.toggle_rev);
            toggle_cap = view.findViewById(R.id.toggle_cap);

            toggle_exp.setOnClickListener(this);
            toggle_rev.setOnClickListener(this);
            toggle_cap.setOnClickListener(this);

            category_selector_holder = view.findViewById(R.id.category_selector_holder);
            int defTxtColor = ContextCompat.getColor(mContext, R.color.grey_txt_color);
            mCategorySelector = new CategorySelector(mContext, false, defTxtColor, filteredCategories, this);
            RecyclerView.LayoutParams layoutParams= new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT);
            mCategorySelector.setLayoutParams(layoutParams);
            //mCategorySelector.setLayoutTransition(new LayoutTransition());
            //((SimpleItemAnimator) Objects.requireNonNull(mCategorySelector.getItemAnimator())).setSupportsChangeAnimations(false);
            category_selector_holder.addView(mCategorySelector);

            category_add_btn = view.findViewById(R.id.category_add_btn);
            category_add_btn.setOnClickListener(this);

            main_category_view = view.findViewById(R.id.main_category_view);

        /*NEW UPDATE*/
            type_toggle_btns = new AppCompatToggleButton[4];
            type_toggle_btns[0] = view.findViewById(R.id.type_agg);
            type_toggle_btns[1] = view.findViewById(R.id.type_rev);
            type_toggle_btns[2] = view.findViewById(R.id.type_exp);
            type_toggle_btns[3] = view.findViewById(R.id.type_cap);
            type_toggle_btns[0].setOnClickListener(this);
            type_toggle_btns[1].setOnClickListener(this);
            type_toggle_btns[2].setOnClickListener(this);
            type_toggle_btns[3].setOnClickListener(this);

            aggregator_selector = view.findViewById(R.id.aggregator_selector);
            active_cat_selector = view.findViewById(R.id.active_cat_selector);

            category_full_name = view.findViewById(R.id.category_full_name);
            category_full_name.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    //Log.d("MyTag", "beforeTextChanged" + charSequence);
                }
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    //Log.d("MyTag", "onTextChanged" + charSequence);
                    setShortName(charSequence.toString());
                }
                @Override
                public void afterTextChanged(Editable editable) {
                    //Log.d("MyTag", "onTextChanged" + editable.toString());
                }
            });

            category_short_name = view.findViewById(R.id.category_short_name);
            category_short_name.setOnTouchListener(this);

            aggregator_spinner = view.findViewById(R.id.aggregator_spinner);
            spinnerAdapter = new SpinnerAggregatorAdapter(mContext, R.layout.spinner_item, mCategoriesRepository.aggregators);
            aggregator_spinner.setAdapter(spinnerAdapter);

            new_update_view = view.findViewById(R.id.new_update_view);
            update_save_btn = view.findViewById(R.id.update_save_btn);
            update_save_btn.setOnClickListener(this);
            cancel_btn = view.findViewById(R.id.cancel_btn);
            cancel_btn.setOnClickListener(this);
            name_text_view = view.findViewById(R.id.name_text_view);
            nick_text_view = view.findViewById(R.id.nick_text_view);
            active_cat_chekbox = view.findViewById(R.id.active_cat_chekbox);
            new_update_view_header = view.findViewById(R.id.new_update_view_header);
            category_delete_btn = view.findViewById(R.id.category_delete_btn);
            category_delete_btn.setOnClickListener(this);
            new_update_input_table = view.findViewById(R.id.new_update_input_table);

            /*delete alert*/
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        deleteCategory(editCategoryId);
                    }
                });
                builder.setNegativeButton(R.string.cancel1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
                builder.setTitle(R.string.delete_title);
                builder.setMessage(R.string.delete_message);
                deleteDialog = builder.create();

            /*back button call*/
                callbackBtn = new OnBackPressedCallback(false /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                     cancelBtnClick();
                     }
                };

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callbackBtn);

        /*ANIMATIONS*/
            afterNewUpdateViewShown =  new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    main_category_view.setVisibility(View.GONE);
                    new_update_view.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
                    isAnimating = false;
                }
            };
            afterNewUpdateViewHidden=  new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    new_update_view.setVisibility(View.GONE);
                    main_category_view.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
                    isAnimating = false;
                }
            };

            showNewUpdateView = new TransitionAnimator(main_category_view, new_update_view, true, afterNewUpdateViewShown);
            hideNewUpdateView = new TransitionAnimator(new_update_view, main_category_view, false, afterNewUpdateViewHidden);

            txtTableColor = ContextCompat.getColor(mContext, R.color.table_txt_color);
            txtRedColor = ContextCompat.getColor(mContext, R.color.red_txt_color);
            int txtHintColor = ContextCompat.getColor(mContext, R.color.dark_grey);

            nameLabelValidatorAnim = new NewValidatorAnimator(name_text_view, "TextColor", false, txtRedColor, txtTableColor);
            nameEditValidatorAnim = new NewValidatorAnimator(category_full_name, "HintTextColor", true, txtRedColor, txtHintColor);
            nickLabelValidatorAnim = new NewValidatorAnimator(nick_text_view, "TextColor", false, txtRedColor, txtTableColor);
            nickEditValidatorAnim = new NewValidatorAnimator(category_short_name, "HintTextColor", true, txtRedColor, txtHintColor);


        if(mCategoriesRepository.aggregators.size() == 0){
            mCategoriesRepository.updateACategories();
        }
        applyCategoryFilter();


        //mCategoriesRepository.updateACategories();
        return view;

    }

    @Override
    public void onClick(View view) {
        if(!isAnimating){
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
                case R.id.type_agg:
                    typeSelectorClick(0);
                    break;
                case R.id.type_exp:
                    typeSelectorClick(2);
                    break;
                case R.id.type_rev:
                    typeSelectorClick(1);
                    break;
                case R.id.type_cap:
                    typeSelectorClick(3);
                    break;
                case R.id.category_add_btn:
                    addBtnClick();
                    break;
                case R.id.update_save_btn:
                    updateSaveBtnClick();
                    break;
                case R.id.cancel_btn:
                    cancelBtnClick();
                    break;
                case R.id.category_delete_btn:
                    deleteBtnClick();
                    break;
            }
        }

    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int id = view.getId();
        if(id == R.id.category_short_name){
            if(motionEvent.getAction()==MotionEvent.ACTION_UP){
                needUpdateShortName = false;
            }
        }
        return false;
    }

    @Override
    public void CategoriesUpdated() {
        spinnerAdapter.notifyDataSetChanged();
        applyCategoryFilter();

        if(mCategoriesRepository.sortedCategories.size()==5){
            if(getRateViewedState() == 0){
                mMainActivityCommunication.askForFeedback();
            }
        }
    }

    public int getRateViewedState(){
        SharedPreferences sharedPref = mContext.getSharedPreferences(
                RATE_FILE_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(RATE_STATE_KEY, 0);
    }

    @Override
    public void CategoryUpdated(long id, int index, boolean locked) {
        mCategorySelector.categorySelectorAdapter.notifyItemChanged(index);
    }

    @Override
    public void CategoryClicked(int position) {
        setUpdateCategoryLayout(position);
        showNewUpdateView();
    }

    @Override
    public boolean isCategoryLocked(long id) {
        return mCategoriesRepository.isCategoryLocked(id);
    }


    /*MAIN CATEGORY VIEW*/

    //Top Category Filter
        private void toggleBtnClick(){
            if((!toggle_exp.isChecked())&&
               (!toggle_rev.isChecked())&&
               (!toggle_cap.isChecked())
              ){
                toggle_exp.setChecked(true);
                toggle_rev.setChecked(true);
                toggle_cap.setChecked(true);
            }

            applyCategoryFilter();
        }


        private void applyCategoryFilter(){
            oldCategories.clear();
            oldCategories.addAll(filteredCategories);
            filteredCategories.clear();

            boolean EXP = toggle_exp.isChecked();
            boolean REV = toggle_rev.isChecked();
            boolean CAP = toggle_cap.isChecked();

            //Log.d("MyTag", Boolean.toString(EXP) + Boolean.toString(REV) +Boolean.toString(CAP) );

            if(EXP && REV && CAP){
                filteredCategories.addAll(mCategoriesRepository.categories);
            }else {
                for(int i = 0; i < mCategoriesRepository.categories.size(); i++){
                    boolean add = false;
                    int type = mCategoriesRepository.categories.get(i).type;
                    switch (type){
                        case ACategory.AGGREGATOR:
                            add = true;
                            break;
                        case ACategory.EMPTY_AGGREGATOR:
                            add = true;
                            break;
                        case ACategory.EXPENSE:
                            add = EXP;
                            break;
                        case ACategory.REVENUE:
                            add = REV;
                            break;
                        case ACategory.CAPITAL:
                            add = CAP;
                            break;
                    }

                    if(add){
                        filteredCategories.add(mCategoriesRepository.categories.get(i));
                    }
                }
            }

            DiffUtil.DiffResult dr = DiffUtil.calculateDiff(aCategoryDiffUtilCallback);
            dr.dispatchUpdatesTo(mCategorySelector.categorySelectorAdapter);
           //mCategorySelector.categorySelectorAdapter.notifyDataSetChanged();

        }

    private void addBtnClick(){
        if(!isPremiumActive(mContext)&&mCategoriesRepository.sortedCategories.size()>14){
            Intent intent1 = new Intent(mContext, SubscriptionActivity.class);
            mContext.startActivity(intent1);
        }  else {
            setNewCategoryLayout();
            showNewUpdateView();
        }
    }

    /*NEW UPDATE*/

    private void deleteBtnClick(){
        if(editCategoryId != -1){
            int categoryType = getNewUpdateCategoryType();
            if(categoryType == ACategory.AGGREGATOR){
                deleteAggregator(editCategoryId);
                hideNewUpdateView();
            }else {
                deleteDialog.show();
            }
        }
    }

    private void cancelBtnClick(){
        hideNewUpdateView();
    }

    private void updateSaveBtnClick(){

       ACategory tempCategory = pickACategory();

        if(validateTempCategory(tempCategory)){
            if(tempCategory.type == ACategory.AGGREGATOR){
                saveOrUpdateAggregator(tempCategory);
            }else{
                saveOrUpdateCategory(tempCategory);
            }
            hideNewUpdateView();
        }

    }

    private void deleteCategory(long id){
        if(mDbService!=null){
            mCategoriesRepository.deleteCategory(id);
            mDbService.clearDeletedCategoryData(id);
            hideNewUpdateView();
        }else {
            mMainActivityCommunication.makeToast(MainActivity.DB_SERVICE_IS_NULL);
        }
    }

    private void deleteAggregator(long id){
        mCategoriesRepository.deleteAggregator(id);
    }

    private void saveOrUpdateAggregator(ACategory tempCategory){
        Aggregator aggregator = new Aggregator(ACategory.AGGREGATOR, tempCategory.name, tempCategory.nick);
        if(tempCategory.id == -1){
            mCategoriesRepository.insetAggregator(aggregator);
        }else {
            aggregator.setId(tempCategory.id);
            mCategoriesRepository.updateAggregator(aggregator);
        }
    }

    private void saveOrUpdateCategory(ACategory tempCategory){
        Category category = new Category(tempCategory.type, tempCategory.aggregatorId, tempCategory.name, tempCategory.nick, tempCategory.active, System.currentTimeMillis());
        if(tempCategory.id == -1){
            mCategoriesRepository.insetCategory(category);
        }else {
            category.setId(tempCategory.id);
            mCategoriesRepository.updateCategory(category);
        }
    }


    private boolean validateTempCategory(ACategory tempCategory){
        boolean validated = true;
        if(tempCategory.type == ACategory.EMPTY_AGGREGATOR){
            validated = false;
        }
        if(tempCategory.name.isEmpty()){
            validated = false;
            nameLabelValidatorAnim.playValidatorAnim();
            nameEditValidatorAnim.playValidatorAnim();
        }
        if(tempCategory.nick.isEmpty()){
            validated = false;
            nickLabelValidatorAnim.playValidatorAnim();
            nickEditValidatorAnim.playValidatorAnim();
        }

        return validated;
    }


    private ACategory pickACategory(){
        int categoryType = getNewUpdateCategoryType();
        long aggregatorId = 0;
        if(categoryType != ACategory.AGGREGATOR){
            aggregatorId = mCategoriesRepository.aggregators.get(aggregator_spinner.getSelectedItemPosition()).id;
        }


        ACategory aCategory;
        aCategory = new ACategory(
                        editCategoryId,
                        categoryType,
                        null,
                        false,
                        aggregatorId,
                        category_full_name.getText().toString(),
                        category_short_name.getText().toString(),
                        active_cat_chekbox.isChecked());

        return aCategory;
    }

    private int getNewUpdateCategoryType(){
        int checkedId = 0;

        for(int i  = 0; i < type_toggle_btns.length; i++){
            if(type_toggle_btns[i].isChecked()&&
               type_toggle_btns[i].isEnabled()){
                checkedId = i+1;
            }
        }

        switch (checkedId){
            case 1:return ACategory.AGGREGATOR;
            case 2:return ACategory.REVENUE;
            case 3:return ACategory.EXPENSE;
            case 4:return ACategory.CAPITAL;
            default:return ACategory.EMPTY_AGGREGATOR;
        }
    }

    private void setNewCategoryLayout(){
        new_update_view_header.setText(getString(R.string.new_category));
        update_save_btn.setText(getString(R.string.save));
        category_delete_btn.setVisibility(View.GONE);
        editCategoryId = -1;
        typeSelectorAggRevExpCapEnabledSet(true);
        typeSelectorClick(lastCatSelected);
        category_full_name.getText().clear();
        category_short_name.getText().clear();
        needUpdateShortName = true;
        aggregator_spinner.setSelection(0);
    }

    private void setUpdateCategoryLayout(int position){
        new_update_view_header.setText(getString(R.string.update_category));
        update_save_btn.setText(getString(R.string.update));
        category_delete_btn.setVisibility(View.VISIBLE);

        ACategory temp = filteredCategories.get(position);
        //ACategory temp = mCategorySelector.categorySelectorAdapter.getCategory(position);

        editCategoryId = temp.id;
        typeSelectorClick(temp.type - 1);
        setTypeSelectorState(temp.type);
        category_full_name.setText(temp.name);
        category_short_name.setText(temp.nick);
        needUpdateShortName = false;
        aggregator_spinner.setSelection(mCategoriesRepository.indexOfAggregator(temp.aggregatorId));
        active_cat_chekbox.setChecked(temp.active);
        active_cat_chekbox.jumpDrawablesToCurrentState();
    }

    private void setTypeSelectorState(int type){
        if(type == ACategory.AGGREGATOR){
            type_toggle_btns[0].setEnabled(true);
            typeSelectorRevExpCapEnabledSet(false);
        }else {
            type_toggle_btns[0].setEnabled(false);
            typeSelectorRevExpCapEnabledSet(true);
        }
    }

    private void typeSelectorRevExpCapEnabledSet(boolean enabled){
        for(int i  = 1; i < type_toggle_btns.length; i++){
            type_toggle_btns[i].setEnabled(enabled);
        }
    }

    private void typeSelectorAggRevExpCapEnabledSet(boolean enabled){
        for(int i  = 0; i < type_toggle_btns.length; i++){
            type_toggle_btns[i].setEnabled(enabled);
        }
    }

    private void typeSelectorClick(int selectorIndex){
        for(int i  = 0; i < type_toggle_btns.length; i++){
            if(i == selectorIndex){
                type_toggle_btns[i].setChecked(true);
                if(i == 0){
                    setAggregatorLayout();
                }else if (i == 3){
                    setCapitalLayout();
                }else {
                    setExpRevLayout();
                }

            }else {
                type_toggle_btns[i].setChecked(false);
            }
        }
        lastCatSelected = selectorIndex;
    }

    private void setAggregatorLayout(){
        aggregator_selector.setVisibility(View.GONE);
        active_cat_selector.setVisibility(View.GONE);
    }

    private void setCapitalLayout(){
        aggregator_selector.setVisibility(View.VISIBLE);
        active_cat_selector.setVisibility(View.VISIBLE);
    }

    private void setExpRevLayout(){
        aggregator_selector.setVisibility(View.VISIBLE);
        active_cat_selector.setVisibility(View.GONE);
    }

    private void setShortName(String fullName){
        if(needUpdateShortName){
            category_short_name.setText(getShortNameFromString(fullName));
        }
    }

    private String getShortNameFromString(String fullName){
        int len = fullName.length();
        StringBuilder stringBuilder =  new StringBuilder();
        if(len >= 1){
            for(int i = 0; i < 7; i++){
                if(i < len){
                    stringBuilder.append(fullName.charAt(i));
                }
            }
            stringBuilder.append('.');
            return stringBuilder.toString();
        }else{
            return "";
        }
    }



    /*ANIMATIONS*/
    private void showNewUpdateView(){
        isAnimating = true;
        callbackBtn.setEnabled(true);
        prepareNewUpdateViewToBeShown();
        new_update_input_table.setLayoutTransition(mLayoutTransition);
        mMainActivityCommunication.hideBottomNavigation();
        showNewUpdateView.startTransition();

    }

    private void hideNewUpdateView(){
        isAnimating = true;
        callbackBtn.setEnabled(false);
        prepareNewUpdateViewToBeHidden();
        new_update_input_table.setLayoutTransition(null);
        mMainActivityCommunication.showBottomNavigation();
        hideNewUpdateView.startTransition();

    }

    private void prepareNewUpdateViewToBeShown(){
        main_category_view.getLayoutParams().height = main_category_view.getHeight();
        new_update_view.getLayoutParams().height = main_category_view.getHeight() + mMainActivityCommunication.getBottomNavigationHeight();
        new_update_view.setAlpha(0f);
        new_update_view.setVisibility(View.VISIBLE);
    }

    private void prepareNewUpdateViewToBeHidden(){
        try {
            imm.hideSoftInputFromWindow(category_full_name.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }catch (Exception e){
            e.printStackTrace();
        }
        new_update_view.getLayoutParams().height = new_update_view.getHeight();
        main_category_view.setAlpha(0f);
        main_category_view.setVisibility(View.VISIBLE);
    }

    public static class TransitionAnimator{

        ObjectAnimator scaleX_VV;
        ObjectAnimator scaleY_VV;
        ObjectAnimator alpha_VV;
        float scale_VVf; float scale_VVt; float alpha_VVf; float alpha_VVt;

        ObjectAnimator scaleX_IV;
        ObjectAnimator scaleY_IV;
        ObjectAnimator alpha_IV;
        float scale_IVf; float scale_IVt; float alpha_IVf; float alpha_IVt;

        long duration;

        private AnimatorSet TransitionAnimatorSet;
        private FastOutSlowInInterpolator fastOutSlowInInterpolator = new FastOutSlowInInterpolator();

        public  TransitionAnimator(View visibleView, View invisibleView, boolean toFront, AnimatorListenerAdapter animatorListenerAdapter){

            float x = 0.08f;
            if(toFront){
                scale_VVf = 1f; scale_VVt = 1f+x;
                scale_IVf = 1f-x; scale_IVt = 1f;
                duration = 300L;
            }else {
                scale_VVf = 1f; scale_VVt = 1f-x;
                scale_IVf = 1f+x; scale_IVt = 1f;
                duration = 300L;
            }
            alpha_VVf = 1; alpha_VVt = 0;
            alpha_IVf = 0; alpha_IVt = 1;

            long delAddition = 0;

            scaleX_VV = ObjectAnimator.ofFloat(visibleView, "scaleX", scale_VVf, scale_VVt);
            scaleY_VV =ObjectAnimator.ofFloat(visibleView, "scaleY", scale_VVf, scale_VVt);
            alpha_VV = ObjectAnimator.ofFloat(visibleView, "Alpha", alpha_VVf, alpha_VVt);
            scaleX_VV.setDuration(duration);
            scaleY_VV.setDuration(duration);

            scaleX_VV.setStartDelay(delAddition);
            scaleY_VV.setStartDelay(delAddition);

            alpha_VV.setDuration(125);
            alpha_VV.setStartDelay(25 + delAddition);

            scaleX_IV = ObjectAnimator.ofFloat(invisibleView, "scaleX", scale_IVf, scale_IVt);
            scaleY_IV =ObjectAnimator.ofFloat(invisibleView, "scaleY", scale_IVf, scale_IVt);
            alpha_IV = ObjectAnimator.ofFloat(invisibleView, "Alpha", alpha_IVf, alpha_IVt);
            scaleX_IV.setDuration(duration);
            scaleY_IV.setDuration(duration);

            scaleX_IV.setStartDelay(delAddition);
            scaleY_IV.setStartDelay(delAddition);

            alpha_IV.setDuration(100);
            alpha_IV.setStartDelay(25+delAddition);


            TransitionAnimatorSet = new AnimatorSet();
            TransitionAnimatorSet.setInterpolator(fastOutSlowInInterpolator);
            TransitionAnimatorSet.addListener(animatorListenerAdapter);
            TransitionAnimatorSet.playTogether(scaleX_VV, scaleY_VV, alpha_VV, scaleX_IV, scaleY_IV, alpha_IV);
            TransitionAnimatorSet.setStartDelay(50);

        }

        public void startTransition(){
            TransitionAnimatorSet.start();
        }
    }



    public static class NewValidatorAnimator{
        final int mViewColorFrom;
        final int mViewColorTo;
        View mView;
        String mProperty;
        boolean mHint;
        private ObjectAnimator bounceValidatorAnimator;
        private ObjectAnimator colorValidatorAnimator;
        private BounceInterpolator bounceInterpolator =  new BounceInterpolator();
        private AnimatorListenerAdapter animatorListenerAdapter;

        public NewValidatorAnimator(View view, String property, boolean hint ,int viewColorFrom, int viewColorTo) {
            this.mViewColorFrom = viewColorFrom;
            this.mViewColorTo = viewColorTo;
            this.mView = view;
            this.mProperty = property;
            this.mHint = hint;
            bounceValidatorAnimator = ObjectAnimator.ofFloat(mView, "translationX", 15, 0);
            bounceValidatorAnimator.setDuration(300);
            bounceValidatorAnimator.setInterpolator(bounceInterpolator);
            animatorListenerAdapter = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    colorValidatorAnimator = ObjectAnimator.ofObject(mView, mProperty, new ArgbEvaluator(), mViewColorFrom, mViewColorTo);
                    colorValidatorAnimator.setDuration(300);
                    colorValidatorAnimator.start();
                }
            };
        }

        public void playValidatorAnim(){
            if(bounceValidatorAnimator!=null){
                bounceValidatorAnimator.removeListener(animatorListenerAdapter);
                bounceValidatorAnimator.cancel();
            }
            if(colorValidatorAnimator!=null){
                colorValidatorAnimator.cancel();
            }
            bounceValidatorAnimator.addListener(animatorListenerAdapter);
            if(!mHint){
                if (mView instanceof TextView) {
                    ((TextView)mView).setTextColor(mViewColorFrom);
                }
                if (mView instanceof CheckBox) {
                    ((CheckBox)mView).setTextColor(mViewColorFrom);
                }
            }else {
                if (mView instanceof TextView) {
                    ((TextView)mView).setHintTextColor(mViewColorFrom);
                }
                if (mView instanceof EditText) {
                    ((EditText)mView).setHintTextColor(mViewColorFrom);
                }
            }
            bounceValidatorAnimator.start();
        }


    }
}
