package tech.aurorafin.aurora;

import android.content.Context;


import tech.aurorafin.aurora.dbRoom.Operation;
import tech.aurorafin.aurora.dbRoom.OperationTotal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static tech.aurorafin.aurora.PlanData.getDayCode;
import static tech.aurorafin.aurora.PlanData.longNumToString;

public class OperData {

    Context mContext;
    OperDataCallback mOperDataCallback;

    String[] mMonths;
    String[] shortMonths;
    String[] mWeekDays;
    String sYear;
    String sWeek;

    public List<Operation> mOperations;
    boolean operationsSelections[];

    List<LocalTotal> totalMonths;
    List<LocalTotal> totalWeeks;
    HashMap<Integer, Integer> totalWeeksMap;

    Calendar start;
    Calendar end;

    boolean weekPresentation = false;

    int YEAR;
    long CATEGORY_ID;
    long overAllTotal = 0L;


    // -----SELECTION HANDLE---------------------------------------------

    int selectedRowsCount = 0;
    int maxSelectedRowsCount;


    public static final int HIDDEN = 5;
    public static final int SELECT_ALL = 6;
    public static final int CANCEL = 7;

    public boolean localTotalsSelection = false;
    public boolean operationsRowsSelection = false;
   //--------------------------------------------------------------------

    boolean reverse = false;


    public  OperData(Context context, OperDataCallback operDataCallback){
        mContext =context;
        mOperDataCallback = operDataCallback;

        mMonths = mContext.getResources().getStringArray(R.array.months);
        shortMonths = mContext.getResources().getStringArray(R.array.short_months);
        mWeekDays = mContext.getResources().getStringArray(R.array.week_days);
        sWeek = mContext.getResources().getString(R.string.week);
        sYear = mContext.getResources().getString(R.string.year);

        totalMonths = new ArrayList<>();
        totalWeeks = new ArrayList<>();
        totalWeeksMap = new HashMap<>();
        mOperations = new ArrayList<>();

    }

    public void populateEmptyLists(long categoryId, int year){

        this.YEAR = year;
        this.CATEGORY_ID = categoryId;
        start = Calendar.getInstance();
        //start.setFirstDayOfWeek(Calendar.SUNDAY);
        start.set(Calendar.YEAR,year);
        start.set(Calendar.MONTH,0);
        start.set(Calendar.DAY_OF_MONTH,1);

        end = Calendar.getInstance();
        //end.setFirstDayOfWeek(Calendar.SUNDAY);
        end.set(Calendar.YEAR,year);
        end.set(Calendar.MONTH,11);
        end.set(Calendar.DAY_OF_MONTH,31);

        totalMonths.add(new LocalTotal(1,year, 0,0,false, true, categoryId));
        totalWeeks.add(new LocalTotal(1,year,0,0,false, true, categoryId));

        int i = 0;
        int prev_week = 0;
        int prev_month = 0;
        int prevDateCode = 0;
        int weekDayCodeFrom = 0;
        int monthDayCodeFrom = 0;

        int totalWeekCount = 1;

        for (Date date = start.getTime(); !start.after(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
            int day = start.get(Calendar.DAY_OF_MONTH);
            int week = start.get(Calendar.WEEK_OF_YEAR);
            int month = start.get(Calendar.MONTH);
            int tyear = start.get(Calendar.YEAR);

            if(i == 0){
                prev_week = week;
                prev_month = month;
                weekDayCodeFrom = getDayCode(tyear, month, day);
                monthDayCodeFrom = getDayCode(tyear, month, day);

            }
            if (prev_week != week){
                int yearToTW;
                if (month == 0 && prev_week >= 52){
                    yearToTW = tyear - 1;
                }else if(month == 11 && prev_week == 1){
                    yearToTW = tyear + 1;
                }else {
                    yearToTW = tyear;
                }
                LocalTotal lt = new LocalTotal(prev_week, yearToTW, weekDayCodeFrom, prevDateCode,true, false, categoryId);
                totalWeeks.add(lt);
                //map
                weekDayCodeFrom = getDayCode(tyear, month, day);
                totalWeekCount++;
            }
            if (prev_month != month){
                LocalTotal lt = new LocalTotal(prev_month,tyear, monthDayCodeFrom, prevDateCode,false, false, categoryId);
                totalMonths.add(lt);
                monthDayCodeFrom = getDayCode(tyear, month, day);
            }

            prevDateCode = getDayCode(tyear, month, day);

            if ((month==11 && day ==31)){
                int yearToTW;
                if(week == 1){
                    yearToTW = tyear + 1;
                }else {
                    yearToTW = tyear;
                }
                totalWeeks.add(new LocalTotal(week, yearToTW, weekDayCodeFrom, prevDateCode,true, false, categoryId));
                totalMonths.add( new LocalTotal(month,tyear, monthDayCodeFrom, prevDateCode,false, false, categoryId));

            }

            totalWeeksMap.put(prevDateCode, totalWeekCount);

            i++;
            prev_week = week;
            prev_month = month;
        }

        //operationsSelections = new boolean[mOperations.size()];

       /* totalMonths.add(new LocalTotal(1,year, 0,0,false, true));
        totalWeeks.add(new LocalTotal(1,year,0,0,false, true));*/
    }

    public List<LocalTotal> getDeleteOperationsTotals() {
        /*if(selectedRowsCount == maxSelectedRowsCount){
            if(weekPresentation){
                return totalWeeks;
            }else {
                return totalMonths;
            }
        }else {
            if(weekPresentation){
                return selectedTotalWeeks();
            }else {
                return selectedTotalMonths();
            }
        }*/
        if(weekPresentation){
            return selectedTotalWeeks();
        }else {
            return selectedTotalMonths();
        }
    }

    private List<LocalTotal> selectedTotalWeeks() {
        List<LocalTotal> selectedTotalWeeks = new ArrayList<>();
        for(int i = 0; i < totalWeeks.size(); i++){
            if(totalWeeks.get(i).selected && totalWeeks.get(i).rowId != -1){
                selectedTotalWeeks.add(totalWeeks.get(i));
            }
        }
        return selectedTotalWeeks;
    }
    private List<LocalTotal> selectedTotalMonths() {
        List<LocalTotal> selectedTotalMonths = new ArrayList<>();
        for(int i = 0; i < totalMonths.size(); i++){
            if(totalMonths.get(i).selected && totalMonths.get(i).rowId != -1){
                selectedTotalMonths.add(totalMonths.get(i));
            }
        }
        return selectedTotalMonths;
    }


    public List<Operation> getDeleteOperationsList() {
        List<Operation> deleteOperations = new ArrayList<>();
        if(selectedRowsCount == maxSelectedRowsCount){
            deleteOperations.addAll(mOperations);
        }else {
            for(int i = 0; i<mOperations.size(); i++){
                if(operationsSelections[i]){
                    deleteOperations.add(mOperations.get(i));
                }
            }
        }
        return deleteOperations;
    }


    public void setNewOperations(List<Operation> operations){
        mOperations.clear();
        mOperations.addAll(operations);
        operationsSelections = new boolean[mOperations.size()];
        if(reverse){
            Collections.reverse(mOperations);
        }
    }

    public LocalTotal getLocalTotalForOperationsUpdate(int position) {
        if(weekPresentation){
            return totalWeeks.get(position);
        }else {
            return totalMonths.get(position);
        }
    }


    public void switchPresentationMode(boolean weekPresentation) {
        if (this.weekPresentation != weekPresentation){
            dropSelection();
            this.weekPresentation = weekPresentation;
        }
    }


    public synchronized void resetAndMapSafe(OperationTotal[] totalMonths,
                                             OperationTotal[] totalDays,
                                             OperationTotal totalYear,
                                             long operationCategoryId,
                                             int operationsYear) {
        overAllTotal = 0L;
        mOperations.clear();
        this.totalMonths.clear();
        this.totalWeeks.clear();
        this.totalWeeksMap.clear();
        populateEmptyLists(operationCategoryId, operationsYear);
        mapOperationTotals(totalMonths, totalDays, totalYear);
        if(reverse){
            reverse = false;
            reverseTotals();
        }
    }

    private void mapOperationTotals(OperationTotal[] totalMonths, OperationTotal[] totalDays, OperationTotal totalYear) {
        for(int i = 0; i < totalMonths.length; i++){
            this.totalMonths.get(totalMonths[i].period + 1).setRowId(totalMonths[i].id);
            this.totalMonths.get(totalMonths[i].period + 1).setLocalTotal(totalMonths[i].value);
        }

        for(int i = 0; i < totalDays.length; i++){
            if(totalWeeksMap.containsKey(totalDays[i].dateCodeFrom)){

                int index = totalWeeksMap.get(totalDays[i].dateCodeFrom);
                this.totalWeeks.get(index).setRowId(totalDays[i].id);
                this.totalWeeks.get(index).setLocalTotal( this.totalWeeks.get(index).getLocalTotal() + totalDays[i].value);
            }
        }

        if(totalYear != null){
            overAllTotal = totalYear.value;
        }
    }

    public void resetIU() {
        dropSelection();
    }



    public void reverseTotals(){
        if(totalMonths.size()>0 &&totalWeeks.size()>0){
            totalMonths.remove(0);
            totalWeeks.remove(0);
            Collections.reverse(totalMonths);
            Collections.reverse(totalWeeks);
            totalMonths.add(0, new LocalTotal(1,YEAR, 0,0,false, true, CATEGORY_ID));
            totalWeeks.add(0, new LocalTotal(1,YEAR, 0,0,false, true, CATEGORY_ID));
            Collections.reverse(mOperations);
        }
            reverse = !reverse;


    }





    /*Selection*/

    public boolean isOperationSelected(int pos) {
        return operationsSelections[pos];
    }

    public boolean isTotalSelected(int position) {
        if(weekPresentation){
            return totalWeeks.get(position).selected;
        }else {
            return totalMonths.get(position).selected;
        }
    }

    public void selectAllCancel(){
        if(maxSelectedRowsCount == selectedRowsCount){
            dropSelection();
        }else {
            selectAll();
        }
    }

    public void dropSelection(){
        if(localTotalsSelection){
            dropAllTotals();
            //mInputManager.planTableAdapterDatasetChanged(false);
        }else if(operationsRowsSelection){
            dropOperationsSelection();
        }
    }

    public void selectAll(){
        if(localTotalsSelection){
            selectAllTotals();
            mOperDataCallback.operTableAdapterDatasetChanged(false);
        }else if(operationsRowsSelection){
            selectAllOperations();
            mOperDataCallback.operTableAdapterDatasetChanged(true);
        }
    }


    public void selectAllTotals(){
        if(weekPresentation){
            for(int i = 1; i < totalWeeks.size(); i++){
                totalWeeks.get(i).selected = true;
            }
        }else {
            for(int i = 1; i < totalMonths.size(); i++){
                totalMonths.get(i).selected = true;
            }
        }
        selectedRowsCount = maxSelectedRowsCount;
        mOperDataCallback.updateSelectAllCancelBtnsState(CANCEL);
    }


    public void selectAllOperations(){
        for(int i = 0; i < operationsSelections.length; i++ ){
            operationsSelections[i] = true;
        }
         selectedRowsCount = maxSelectedRowsCount;
         mOperDataCallback.updateSelectAllCancelBtnsState(CANCEL);

    }

    public void dropAllTotals(){
        if(weekPresentation){
            for(int i = 1; i < totalWeeks.size(); i++){
                totalWeeks.get(i).selected = false;
            }
        }else {
            for(int i = 1; i < totalMonths.size(); i++){
                totalMonths.get(i).selected = false;
            }
        }
        selectedRowsCount = 0;
        stopRowsSelection(true);
    }


    public void dropOperationsSelection(){

       for(int i = 0; i < operationsSelections.length; i++ ){
           operationsSelections[i] = false;
       }
       selectedRowsCount = 0;
       stopRowsSelection(false);
    }

    public void setOperationUnselected(int pos, boolean total) {
        if(total){
            if(weekPresentation){
                totalWeeks.get(pos).selected = false;
            }else {
                totalMonths.get(pos).selected = false;
            }
        }else{
            operationsSelections[pos] = false;
        }
        selectedRowsCount--;
        if(selectedRowsCount == 0){
            stopRowsSelection(total);
        }

        if(selectedRowsCount == maxSelectedRowsCount -1 ){
            mOperDataCallback.updateSelectAllCancelBtnsState(SELECT_ALL);
        }
    }

    public void stopRowsSelection(boolean total){
        if(total){
            localTotalsSelection = false;
            mOperDataCallback.operTableAdapterDatasetChanged(false);
        }else{
            operationsRowsSelection = false;
            mOperDataCallback.operTableAdapterDatasetChanged(true);
        }
        mOperDataCallback.updateSelectAllCancelBtnsState(HIDDEN);
        mOperDataCallback.setDeleteBtnEnabled(false);
    }


    public void setOperationSelected(int pos, boolean total) {
        if(total){
            if(weekPresentation){
                totalWeeks.get(pos).selected = true;
            }else {
                totalMonths.get(pos).selected = true;
            }
        }else{
            operationsSelections[pos] = true;
        }
        selectedRowsCount++;
        if(selectedRowsCount == 1){
            if(total){
                localTotalsSelection = true;
                mOperDataCallback.operTableAdapterDatasetChanged(false);
                maxSelectedRowsCount = getMaxTotalSelection();
            }else{
                operationsRowsSelection = true;
                maxSelectedRowsCount = mOperations.size();
            }
            mOperDataCallback.updateSelectAllCancelBtnsState(SELECT_ALL);
            mOperDataCallback.setDeleteBtnEnabled(true);
        }

        if(selectedRowsCount == maxSelectedRowsCount){
            mOperDataCallback.updateSelectAllCancelBtnsState(CANCEL);
        }
    }

    public int getMaxTotalSelection(){
        if(weekPresentation){
            return totalWeeks.size() - 1;
        }else {
            return totalMonths.size() - 1;
        }
    }



    /*Labels*/

    public boolean getRowType(int position) {
        if(weekPresentation){
            return totalWeeks.get(position).btr;
        }else {
            return totalMonths.get(position).btr;
        }
    }

    public String getBtrLabel(int position) {
        return sYear + " " + YEAR;
    }

    public String getBtrValue() {
        return longNumToString(overAllTotal);
    }

    public String getTotalLabel(int position) {
        if(weekPresentation){
            return sWeek + " " +Integer.toString(totalWeeks.get(position).period) + ", " + Integer.toString(totalWeeks.get(position).year);
        }else {
            return mMonths[totalMonths.get(position).period] + " " +Integer.toString(totalMonths.get(position).year);
        }
    }

    public String getTotalValue(int position) {
        if(weekPresentation){
            return longNumToString(totalWeeks.get(position).localTotal);
        }else {
            return longNumToString(totalMonths.get(position).localTotal);
        }
    }

    public String getOperationLabel(int position) {
        if(weekPresentation){
            return getWeekLabel(mOperations.get(position));
        }else {
            return getMonthLabel(mOperations.get(position));
        }
    }

    public String getMonthLabel(Operation operation){
        if(operation.day <= 9){
            return "0"+Integer.toString(operation.day) + " - " + mWeekDays[operation.dayWeek -1];
        }else {
            return Integer.toString(operation.day) + " - " + mWeekDays[operation.dayWeek -1];
        }
    }

    public String getWeekLabel(Operation operation){
        if(operation.day <= 9){
            return "0"+Integer.toString(operation.day) + " " + shortMonths[operation.month] + " - " + mWeekDays[operation.dayWeek-1];
        }else {
            return Integer.toString(operation.day) + " " + shortMonths[operation.month] + " - " + mWeekDays[operation.dayWeek-1];
        }
    }

    public String getOperationValue(int position) {
        return longNumToString(mOperations.get(position).value);
    }

    public int getTotalsSize() {
        if(weekPresentation){
            return totalWeeks.size();
        }else {
            return totalMonths.size();
        }
    }

    public boolean isTotalCollapsible(int position){
        long id;
        if(weekPresentation){
            id = totalWeeks.get(position).rowId;
        }else {
            id = totalMonths.get(position).rowId;
        }

        if(id != -1){
            return true;
        }else {
            return false;
        }
    }




    public class LocalTotal{
        int period;
        int year;
        int dateCodeFrom;
        int dateCodeTo;
        long localTotal = 0L;
        int yearTotalIndex = 0;
        boolean isWeek;
        long rowId = -1L;
        boolean btr;
        boolean selected = false;
        long categoryId;

        public LocalTotal(int period, int year,  int dateCodeFrom, int dateCodeTo, boolean isWeek, boolean btr, long categoryId){
            this.period = period;
            this.year = year;
            this.dateCodeFrom = dateCodeFrom;
            this.dateCodeTo = dateCodeTo;
            this.isWeek = isWeek;
            this.btr = btr;
            this.categoryId = categoryId;
        }

        public void setRowId(long rowId) {
            this.rowId = rowId;
        }

        public void setLocalTotal(long localTotal) {
            this.localTotal = localTotal;
        }

        public long getLocalTotal() {
            return localTotal;
        }
    }


    public interface OperDataCallback{
        void updateSelectAllCancelBtnsState(int state);
        void setDeleteBtnEnabled(boolean enabled);
        void operTableAdapterDatasetChanged(boolean withSub);
        void updateOperations(LocalTotal localTotalForOperationsUpdate);
        void lockScreenWhileLoadingOperations();
        void operationClicked(Operation operation);
        void backButtonCollapse(boolean backBtnActionEnabled);
    }


}
