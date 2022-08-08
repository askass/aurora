package tech.aurorafin.aurora;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import tech.aurorafin.aurora.dbRoom.Operation;
import tech.aurorafin.aurora.dbRoom.CategoriesRepository;
import tech.aurorafin.aurora.subscription.SubscriptionActivity;

import com.google.android.flexbox.FlexboxLayout;
import com.maltaisn.calcdialog.CalcDialog;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static tech.aurorafin.aurora.DateFormater.getDateFromDateCode;
import static tech.aurorafin.aurora.subscription.SubscriptionActivity.isPremiumActive;

public class NewOperationFragment extends Fragment implements View.OnClickListener,
        DatePickerDialog.OnDateSetListener, CategoryDialogAdapter.CategoryDialogCallback, CategoryChip.ChipUpdates,
        CalcDialog.CalcDialogCallback{

    Context mContext;
    CategoriesRepository mCategoriesRepository;
    NewOperationCallback mNewOperationCallback;

    private boolean isAnimating = false;


    TextView new_operation_header, save_operation, cancel_operation;
    InputMethodManager imm;

    String newOperation;
    String updateOperation;
    String saveLabelText;
    String updateLabelText;

    /*Temps*/
        long tempId;
        long tempAmmount;
        int tempYear;
        int tempMonth;
        int tempDay;
        long tempCategoryId;
        long initCategoryId;
        String tempDescription;

    /*Amount*/
        TextView amount_text_label;
        EditText amount_input;
        //SpecEditText amount_input;
        AppCompatImageButton plus_minus_btn;
        InputFilter[] filter12, filter13;
        CalcDialog calcDialog;

    /*Date*/
        TextView operation_date;
        DatePickerDialog datePickerDialog;
        Calendar calendar;
        int formatCode;

    /*Category*/
        TextView operation_category_label;
        TextView operation_category;
        AlertDialog selectCategoryDialog;
        RecyclerView select_dialog_rv;
        CategoryDialogAdapter categoryDialogAdapter;
        int DpW;
        String[] letterTypes;

    /*Chips*/
        FlexboxLayout categories_chips_layout;
        List<CategoryChip> categoryChips;

    /*Description*/
        EditText operation_description;
        String descriptionLabelText;
        TextView operation_description_label;

    /*Delete*/
        AppCompatImageButton operation_delete_btn;
        AlertDialog deleteDialog;

    /*Validators*/
    CategoriesFragment.NewValidatorAnimator labelValueValidatorAnim;
    CategoriesFragment.NewValidatorAnimator dotLabelValidatorAnim;
    CategoriesFragment.NewValidatorAnimator intValueValidatorAnim;
    CategoriesFragment.NewValidatorAnimator decimalValueValidatorAnim;
    CategoriesFragment.NewValidatorAnimator labelCategoryValidatorAnim;
    CategoriesFragment.NewValidatorAnimator selectCategoryValidatorAnim;




    public interface NewOperationCallback{
        void cancelBtnClicked();
        void saveUpdateOperation(long operationId , long categoryId, long initCategoryId,int day, int month, int year, long value, String description );
        void deleteOperation(long operationId, long categoryId);
    }


    public NewOperationFragment(Context context, CategoriesRepository categoriesRepository, NewOperationCallback newOperationCallback){
        this.mContext = context;
        this.mCategoriesRepository = categoriesRepository;
        this.mNewOperationCallback = newOperationCallback;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.new_operation_fragment, container, false);

        imm = (InputMethodManager)(mContext.getSystemService(Context.INPUT_METHOD_SERVICE));
        save_operation= view.findViewById(R.id.save_operation);
        save_operation.setOnClickListener(this);
        cancel_operation= view.findViewById(R.id.cancel_operation);
        cancel_operation.setOnClickListener(this);

        new_operation_header = view.findViewById(R.id.new_operation_header);
        newOperation = getString(R.string.new_operation_header);
        updateOperation = getString(R.string.update_operation_header);
        saveLabelText = getString(R.string.save);
        updateLabelText= getString(R.string.update);

        /*Amount*/

            calcDialog = getCalcDialog();

            amount_text_label = view.findViewById(R.id.amount_text_label);
            /*amount_dot = view.findViewById(R.id.amount_dot);*/
            amount_input = view.findViewById(R.id.amount_input);
            /*filter12 = new InputFilter[] { new InputFilter.LengthFilter(15) };
            filter13 = new InputFilter[] { new InputFilter.LengthFilter(16) };*/
            amount_input.setFilters(new InputFilter[]{ new InputFilterMinMax(-1000000000000.00, 1000000000000.00)});
            amount_input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }
                @Override
                public void afterTextChanged(Editable editable) {


                    amount_input.removeTextChangedListener(this);
                    validateNumber(editable.toString());
                    amount_input.addTextChangedListener(this);
                }
            });


            plus_minus_btn = view.findViewById(R.id.plus_minus_btn);
            plus_minus_btn.setOnClickListener(this);
            /*amount_decimal = view.findViewById(R.id.amount_decimal);*/


        /*Date*/
            operation_date = view.findViewById(R.id.operation_date);
            operation_date.setOnClickListener(this);
            calendar = Calendar.getInstance();
            tempYear = calendar.get(Calendar.YEAR);
            tempMonth = calendar.get(Calendar.MONTH);
            tempDay = calendar.get(Calendar.DAY_OF_MONTH);
            datePickerDialog = new DatePickerDialog(mContext, this, tempYear, tempMonth, tempDay);
            operation_date.setText(getDateString(tempYear, tempMonth, tempDay));
            formatCode = DateFormater.getDateFormatKey(mContext);

        /*Category*/
            operation_category_label = view.findViewById(R.id.operation_category_label);
            operation_category = view.findViewById(R.id.operation_category);
            operation_category.setOnClickListener(this);
            View viewDialog = inflater.inflate(R.layout.category_dialog, null);
            select_dialog_rv = viewDialog.findViewById(R.id.select_dialog_rv);
            categoryDialogAdapter = new CategoryDialogAdapter(mContext, mCategoriesRepository.categories, this);
            select_dialog_rv.setAdapter(categoryDialogAdapter);
            select_dialog_rv.setLayoutManager(new LinearLayoutManager(mContext));
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle(R.string.categories);
            builder.setView(viewDialog);
            builder.setPositiveButton(R.string.cancel1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            selectCategoryDialog = builder.create();
            float density = mContext.getResources().getDisplayMetrics().density;
            DpW = (int)(300f * density +0.5f);
            letterTypes = mContext.getResources().getStringArray(R.array.category_types);

        /*Chips*/
            categories_chips_layout = view.findViewById(R.id.categories_chips_layout);
            categoryChips = new ArrayList<>();
            makeChips();

        /*Description*/
            operation_description_label = view.findViewById(R.id.operation_description_label);
            descriptionLabelText = getString(R.string.description);
            operation_description = view.findViewById(R.id.operation_description);
            operation_description.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override
                public void afterTextChanged(Editable editable) {
                    int l = editable.length();
                    if(l == 0){
                        operation_description_label.setText(descriptionLabelText);
                    }else {
                        String s = " ("+ l + "/500)";
                        operation_description_label.setText(descriptionLabelText + s);
                    }
                }
            });

            amount_input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        operation_description.requestFocus();
                        return true;
                    }
                    return false;
                }
            });

        /*Delete*/
            operation_delete_btn = view.findViewById(R.id.operation_delete_btn);
            operation_delete_btn.setOnClickListener(this);
            AlertDialog.Builder deleteDialogBuilder = new AlertDialog.Builder(mContext);
            deleteDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    hideSoftKeyboard();
                    mNewOperationCallback.deleteOperation(tempId, tempCategoryId);
                }
            });
            deleteDialogBuilder.setNegativeButton(R.string.cancel1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            deleteDialogBuilder.setTitle(R.string.delete_operation);
            deleteDialogBuilder.setMessage(R.string.delete_operation_message);
            deleteDialog = deleteDialogBuilder.create();


        /*Validators*/
        int txtTableColor = ContextCompat.getColor(mContext, R.color.table_txt_color);
        int txtRedColor = ContextCompat.getColor(mContext, R.color.red_txt_color);
        int txtHintColor = ContextCompat.getColor(mContext, R.color.dark_grey);




        labelValueValidatorAnim = new CategoriesFragment.NewValidatorAnimator(amount_text_label, "TextColor", false, txtRedColor, txtTableColor);
        //dotLabelValidatorAnim = new CategoriesFragment.NewValidatorAnimator(amount_dot, "TextColor", false, txtRedColor, txtTableColor);

        intValueValidatorAnim = new CategoriesFragment.NewValidatorAnimator(amount_input, "HintTextColor", true, txtRedColor, txtHintColor);
        //decimalValueValidatorAnim = new CategoriesFragment.NewValidatorAnimator(amount_decimal, "HintTextColor", true, txtRedColor, txtHintColor);

        labelCategoryValidatorAnim = new CategoriesFragment.NewValidatorAnimator(operation_category_label, "TextColor", false, txtRedColor, txtTableColor);
        selectCategoryValidatorAnim = new CategoriesFragment.NewValidatorAnimator(operation_category, "HintTextColor", true, txtRedColor, txtHintColor);

        return view;
    }

    private String getValidatedNumber(String numb) {
        int exitCount = 2;
        boolean passedDot = false;
        int l = numb.length();
        StringBuilder stringBuilder =  new StringBuilder();
        for(int i = 0; i < l; i++){
            stringBuilder.append(numb.charAt(i));

            if(passedDot){
                exitCount--;
            }
            if(exitCount == 0){
                break;
            }

            if(numb.charAt(i) == '.'||numb.charAt(i) == ','){
                passedDot = true;
            }
        }
        return stringBuilder.toString();
    }

    private void validateNumber(String numb){
        int decimalsCount = getDecimalsCount(numb);
        if(decimalsCount > 2){
            //replace string
            String newNumb = numb.substring(0, numb.length()-(decimalsCount-2));
            int selection = amount_input.getSelectionEnd();
            amount_input.setText(newNumb);
            if(selection > newNumb.length()){
                amount_input.setSelection(newNumb.length());
            }else {
                try{
                    amount_input.setSelection(selection);
                }catch (Exception e){

                }
            }

        }else {
            // max len 12/13
        }

    }

    private int getDecimalsCount(String numb) {
        int count = 0;
        if(numb.contains(".")||numb.contains(",")){
            for(int i = numb.length()-1; i>= 0; i--){
                if(numb.charAt(i)=='.' || numb.charAt(i)==','){
                    break;
                }else {
                    count++;
                }
            }
            return count;
        }
        return count;
    }

    public void setAnimating(boolean animating) {
        isAnimating = animating;
        operation_description.setEnabled(!animating);
    }

    @Override
    public void onClick(View view) {
        if(!isAnimating){
            int id = view.getId();
            switch (id) {
                case R.id.plus_minus_btn:
                    plusMinusBtnClick();
                    break;
                case R.id.operation_date:
                    operationDateBtnClick();
                    break;
                case R.id.operation_category:
                    selectCategoryDialog.show();
                    selectCategoryDialog.getWindow().setLayout(DpW, ViewGroup.LayoutParams.WRAP_CONTENT);
                    break;
                case R.id.cancel_operation:
                    cancelBtnClick();
                    break;
                case R.id.save_operation:
                    saveBtnClick();
                    break;
                case R.id.operation_delete_btn:
                    deleteBtnClick();
                    break;
            }
        }
    }

    private void deleteBtnClick() {
        deleteDialog.show();
    }

    private void saveBtnClick() {
        if(validateTempOperation()){
            hideSoftKeyboard();
            if(initCategoryId == -1){
                initCategoryId = tempCategoryId;
            }
            mNewOperationCallback.saveUpdateOperation(tempId, tempCategoryId, initCategoryId,
                    tempDay, tempMonth, tempYear, tempAmmount, operation_description.getText().toString());
        }
    }

    private void hideSoftKeyboard(){
        try {
            imm.hideSoftInputFromWindow(amount_input.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void showSoftKeyboardIfNewOperation(){
        if(tempId == -1){
            amount_input.requestFocus();
            try {
                imm.showSoftInput(amount_input, InputMethodManager.SHOW_IMPLICIT);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private boolean validateTempOperation() {
        boolean validated = true;
        String amount = amount_input.getText().toString();/* +"." + amount_decimal.getText().toString()*/
        tempAmmount = PlanData.stringNumToLong(amount);

        if(tempAmmount == 0){
            labelValueValidatorAnim.playValidatorAnim();
            //dotLabelValidatorAnim.playValidatorAnim();
            intValueValidatorAnim.playValidatorAnim();
            //decimalValueValidatorAnim.playValidatorAnim();
            validated = false;
        }
        if(tempCategoryId == -1){
            labelCategoryValidatorAnim.playValidatorAnim();
            selectCategoryValidatorAnim.playValidatorAnim();
            validated = false;
        }

        return validated;

    }

    public void cancelBtnClick() {
        hideSoftKeyboard();
        mNewOperationCallback.cancelBtnClicked();
    }


    @Override
    public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
        tempYear = year;
        tempMonth = monthOfYear;
        tempDay = dayOfMonth;
        operation_date.setText(getDateString(year, monthOfYear, dayOfMonth));
    }


    @Override
    public void CategoryClicked(int position, long id) {
        setTempOperationCategory(id, mCategoriesRepository.categories.get(position).name);
        updateChipsCheck(id);
        selectCategoryDialog.cancel();
    }

    @Override
    public boolean isCategoryLocked(long id) {
        return mCategoriesRepository.isCategoryLocked(id);
    }

    @Override
    public void chipClicked(long categoryId) {
        updateChipsCheck(categoryId);
        setTempOperationCategory(categoryId, mCategoriesRepository.getCategoryNameById(categoryId));
    }

    public void categoryUpdated(long id, int index, boolean locked) {
        categoryDialogAdapter.notifyItemChanged(index);
        updateChipsLockers();
    }


    /*TEMP OPERATION*/

    public void setUpdateOperationLayout(Operation operation) {
        setTemps(operation);
        new_operation_header.setText(updateOperation);
        save_operation.setText(updateLabelText);
        operation_delete_btn.setVisibility(View.VISIBLE);
        setAmount(operation.value);
        setOperationDate();
        operation_category.setText(mCategoriesRepository.getCategoryNameById(operation.categoryId));
        updateChipsCheck(operation.categoryId);
        operation_description.setText(operation.description);
    }

    private void setOperationDate() {
        datePickerDialog = new DatePickerDialog(mContext, this, tempYear, tempMonth, tempDay);
        operation_date.setText(getDateString(tempYear, tempMonth, tempDay));
    }

    private void setAmount(long value) {
        String amount = PlanData.longToStringWithDot(value);
        amount_input.setText(amount);
       /* StringBuilder newString = new StringBuilder();
        boolean dotReached = false;
        int iterations = 2;

        for (int i = 0; i < amount.length(); i++) {
            if(iterations != 0){
                if(amount.charAt(i) =='.'){
                    dotReached = true;
                    if (newString.length()>0){
                        amount_input.setText(newString.toString());
                        newString.setLength(0);
                    }
                }else {
                    newString.append(amount.charAt(i));
                    if(dotReached){
                        iterations--;
                    }
                }
            }
        }

        if(dotReached){
            amount_decimal.setText(newString.toString());
        }else {
            amount_input.setText(newString.toString());
        }*/
    }

    private void setTemps(Operation operation) {
        tempId = operation.id;
        tempAmmount = operation.value;
        tempYear = operation.year;
        tempMonth = operation.month;
        tempDay = operation.day;
        tempCategoryId = operation.categoryId;
        initCategoryId = tempCategoryId;
        tempDescription = operation.description;
    }


    private void setTempOperationCategory(long categoryId, String name){
        tempCategoryId = categoryId;
        operation_category.setText(name);
    }

    public void setNewOperationLayout() {
        clearTemps();
        new_operation_header.setText(newOperation);
        save_operation.setText(saveLabelText);
        operation_delete_btn.setVisibility(View.GONE);
        amount_input.setText(null);
        //amount_decimal.setText(null);
        setDatePickerToday();
        operation_category.setText(null);
        updateChipsCheck(-1);
        operation_description.setText(null);
    }

    public void presetCategory(long categoryId){
        updateChipsCheck(categoryId);
        setTempOperationCategory(categoryId, mCategoriesRepository.getCategoryNameById(categoryId));
    }

    private void clearTemps() {
        tempId = -1;
        tempAmmount = 0;
        tempYear = -1;
        tempMonth = -1;
        tempDay = -1;
        tempCategoryId = -1;
        initCategoryId = -1;
        tempDescription = "";
    }

    private void setDatePickerToday() {
        calendar = Calendar.getInstance();
        tempYear = calendar.get(Calendar.YEAR);
        tempMonth = calendar.get(Calendar.MONTH);
        tempDay = calendar.get(Calendar.DAY_OF_MONTH);
        datePickerDialog = new DatePickerDialog(mContext, this, tempYear, tempMonth, tempDay);
        operation_date.setText(getDateString(tempYear, tempMonth, tempDay));
    }

    /*AMOUNT ---------------------------------*/

    private void plusMinusBtnClick() {
       /* String s = amount_input.getText().toString();
            if(s.length() == 0 || s.charAt(0) != '-') {
                s = s.replace("-", "");
                //amount_input.setFilters(filter13);
                amount_input.setText("-" + s);
                amount_input.setSelection(amount_input.length());
            }else {
                //amount_input.setFilters(filter12);
                amount_input.setText(s.replace("-", ""));
                amount_input.setSelection(amount_input.length());
        }*/
        if(!isPremiumActive(mContext)){
            Intent intent1 = new Intent(mContext, SubscriptionActivity.class);
            mContext.startActivity(intent1);
        }else {
            BigDecimal f;
            try {
                f = new BigDecimal(amount_input.getText().toString());
            }catch (Exception e){
                f = new BigDecimal(0);
            }
            calcDialog.getSettings().setInitialValue(f);
            hideSoftKeyboard();
            calcDialog.show(getChildFragmentManager(), "calc_dialog");
        }

    }

    @Override
    public void onValueEntered(int requestCode, @Nullable BigDecimal value) {
        DecimalFormat df = new DecimalFormat("###.00");
        //Log.d("MyTag","value = " + df.format(value).replace(',','.'));
        amount_input.setText(value != null ? df.format(value).replace(',','.') : "0");
        amount_input.setSelection(amount_input.getText().toString().length());
    }

    public static CalcDialog getCalcDialog(){
        CalcDialog cd = new CalcDialog();
        cd.getSettings().setExpressionShown(true);
        cd.getSettings().setMaxValue( new BigDecimal("1000000000000.00"));
        cd.getSettings().setMinValue(new BigDecimal("-1000000000000.00"));
        return cd;

    }
    /*private boolean goToDecimalInput(){
        amount_decimal.requestFocus();
        return true;
    }*/

    /*DATE---------------------------------------*/
    private void operationDateBtnClick() {
        datePickerDialog.show();
    }
    private String getDateString(int year, int monthOfYear, int dayOfMonth){
        return getDateFromDateCode(PlanData.getDayCode(year, monthOfYear, dayOfMonth), formatCode);
    }

    /*CHIPS---------------------------------------*/
    private void makeChips(){
        if(categoryChips.size()!=0){
            categories_chips_layout.removeAllViews();
        }
        categoryChips.clear();
        for(int i = 0; i < mCategoriesRepository.lastUsedCategories.size(); i++){
            String name = mCategoriesRepository.lastUsedCategories.get(i).name;
            String type = letterTypes[mCategoriesRepository.lastUsedCategories.get(i).type];
            CategoryChip ccp = new CategoryChip(mContext,this, mCategoriesRepository.lastUsedCategories.get(i).id, name, type);
            categories_chips_layout.addView(ccp);
            categoryChips.add(ccp);
        }
        updateChipsLockers();
    }



    private void updateChipsLockers(){
        for(int i = 0; i < categoryChips.size(); i++){
            long categoryId = categoryChips.get(i).categoryId;
            if(mCategoriesRepository.isCategoryLocked(categoryId)){
                categoryChips.get(i).lockChip();
            }else {
                categoryChips.get(i).unLockChip();
            }
        }
    }

    private void updateChipsCheck(long categoryId){
        for(int i = 0; i < categoryChips.size(); i++){
            if(categoryChips.get(i).isEnabled()){
                long id = categoryChips.get(i).categoryId;
                if(id == categoryId){
                    categoryChips.get(i).setChecked();
                }else {
                    categoryChips.get(i).setDefault();
                }
            }
        }
    }


    public void categoryUpdated() {
        categoryDialogAdapter.notifyDataSetChanged();
        makeChips();
    }

}
