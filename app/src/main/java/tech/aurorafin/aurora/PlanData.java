package tech.aurorafin.aurora;


import tech.aurorafin.aurora.dbRoom.Plan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class PlanData {


    //============Filled from res============================
    String[] mMonths;
    String[] shortMonths;
    String[] mWeekDays;
    String sYear;
    String sWeek;
   //========================================

    List<PlanDay> planDays;
    List<LocalTotal> totalMonths;
    List<LocalTotal> totalWeeks;
    long overAllTotal = 0L;

    HashMap<Integer,Integer> map;

    Calendar start;
    Calendar end;
    private int YEAR;

    boolean weekPresentation = false;

    private InputManager mInputManager;

    //-----UNDO REDO---------------------------------------------------

    List<Action> backActions;
    List<Action> forwardActions;
    Action tempAction;

    public static final int NO_UNDO_NO_REDO = 1;
    public static final int UNDO_REDO = 2;
    public static final int NO_UNDO_REDO = 3;
    public static final int UNDO_NO_REDO = 4;


   // -----SELECTION HANDLE---------------------------------------------
    boolean localTotalsSelection = false;
    boolean planDaySelection = false;
    int selectedRowsCount = 0;
    int maxSelectedRowsCount;
    int totalIndex = -1;

    public static final int HIDDEN = 5;
    public static final int SELECT_ALL = 6;
    public static final int CANCEL = 7;

    // -----COPY PASTE---------------------------------------------
    long[] copiedDays;



    public PlanData(InputManager inputManager) {

        map = new HashMap<>();

        this.mInputManager = inputManager;
        mMonths = mInputManager.getMonths();
        shortMonths = mInputManager.getShortMonths();
        mWeekDays = mInputManager.getWeekDays();
        sYear = mInputManager.getYear();
        sWeek = mInputManager.getWeek();

        backActions = new ArrayList<>();
        forwardActions = new ArrayList<>();

        planDays = new ArrayList<>();
        totalMonths = new ArrayList<>();
        totalWeeks = new ArrayList<>();

        //populateEmptyPlanLists(year);

    }

    private void populateEmptyPlanLists(long categoryId, int year){

        this.YEAR = year;

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

        totalMonths.add(new LocalTotal(1,year, 0,0,false, true));
        totalWeeks.add(new LocalTotal(1,year,0,0,false, true));

        int i = 0;
        int twi = 1;
        int tmi = 1;

        int prev_week = 0;
        int week_from = 0;

        int prev_month = 0;
        int month_from = 0;

        for (Date date = start.getTime(); !start.after(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
            int day = start.get(Calendar.DAY_OF_MONTH);
            int week = start.get(Calendar.WEEK_OF_YEAR);
            int dayWeek = start.get(Calendar.DAY_OF_WEEK);
            int month = start.get(Calendar.MONTH);
            int tyear = start.get(Calendar.YEAR);
            if(i == 0){
                prev_week = week;
                prev_month = month;
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
                //public LocalTotal(int period, int year, int planDays_from, int planDays_to, boolean isWeek, String type)
                LocalTotal lt = new LocalTotal(prev_week, yearToTW, week_from, i - 1,true, false);
                totalWeeks.add(lt);
                week_from = i;
                twi++;
            }
            if (prev_month != month){
                LocalTotal lt = new LocalTotal(prev_month,tyear, month_from, i - 1,false, false);
                totalMonths.add(lt);
                month_from = i;
                tmi++;
            }
            int dayCode = getDayCode(tyear, month, day);
            map.put(dayCode, i);
            planDays.add(new PlanDay(categoryId, day, week, dayWeek, month, tyear, twi, tmi, dayCode));

            if ((month==11 && day ==31)){
                int yearToTW;
                if(week == 1){
                    yearToTW = tyear + 1;
                }else {
                    yearToTW = tyear;
                }
                totalWeeks.add(new LocalTotal(week, yearToTW, week_from, i,true, false));
                totalMonths.add( new LocalTotal(month,tyear, month_from, i,false, false));
            }
            i++;
            prev_week = week;
            prev_month = month;
        }

        for(int day = 0; day< planDays.size(); day++){
            PlanDay tmpDay = planDays.get(day);
            tmpDay.setTotalMonthFrom(planDays.get(totalMonths.get(tmpDay.totalMonthIndex).planDays_from).getDayCode());
            tmpDay.setTotalMonthTo(planDays.get(totalMonths.get(tmpDay.totalMonthIndex).planDays_to).getDayCode());
        }
    }

    public synchronized void resetAndMapSafe(Plan[] tempPlan, long categoryId, int year){
        resetPlanData(categoryId, year);
        mapPlanData(tempPlan);
    }



    private void mapPlanData(Plan[] tempPlan){
        for (Plan plan : tempPlan) {
            int index = map.get(plan.dateCode);
            planDays.get(index).setRowId(plan.id);
            planDays.get(index).setNewValue(index, plan.value, false);
            planDays.get(index).setInitialValue(plan.value);
        }
    }


    private void resetPlanData(long categoryId, int year){
            overAllTotal = 0L;
            planDays.clear();
            totalMonths.clear();
            totalWeeks.clear();
            map.clear();
            populateEmptyPlanLists(categoryId, year);
    }

    public void resetIU(){
        copySelectionClean();
    }


    public void switchPresentationMode(boolean weekPresentation){
        if (this.weekPresentation != weekPresentation){
            copySelectionClean();
            this.weekPresentation = weekPresentation;
        }
    }


    private void copySelectionClean(){
        dropSelection();
        backActions.clear();
        forwardActions.clear();
        copiedDays = null;
        tempAction = null;
        mInputManager.updateUndoRedoBtnsState(getUndoRedoState());
        updateCopyPasteState();
    }

    // -----COPY PASTE-----------------------------------------

    public void pasteAction(){
        if(copiedDays != null){
            if(localTotalsSelection){
                if(weekPresentation){
                    pasteWeekTotal();
                }else {
                    pasteMonthTotal();
                }
            }else if(planDaySelection){
                pasteDays();
            }
            copiedDays = null;
            updateCopyPasteState();
        }
    }

    private void pasteDays(){
        if(copiedDays.length == 1 && totalIndex !=-1) {
            if (tempAction != null) {
                backActions.add(new Action(tempAction.dayIndex, tempAction.val));
                tempAction = null;
            }
            List<Integer> days = new ArrayList<>();
            List<Long> currentVals = new ArrayList<>();
            int dayFrom;
            int dayTo;
            if (weekPresentation) {
                dayFrom = totalWeeks.get(totalIndex).planDays_from;
                dayTo = totalWeeks.get(totalIndex).planDays_to;
            } else {
                dayFrom = totalMonths.get(totalIndex).planDays_from;
                dayTo = totalMonths.get(totalIndex).planDays_to;
            }

            for(int d = dayFrom; d <= dayTo; d++ ){
                if (planDays.get(d).selected){
                    long initialValue = planDays.get(d).value;
                    long  pasteValue = copiedDays[0];
                    if(initialValue != pasteValue){
                        days.add(d);
                        currentVals.add(initialValue);
                        planDays.get(d).setNewValue(d, pasteValue,false);
                    }
                }
            }
            finishAutoAction(days, currentVals);
        }
    }

    private void pasteMonthTotal(){
        if(copiedDays.length == 1 || copiedDays.length == 31){
            if(tempAction != null){
                backActions.add(new Action(tempAction.dayIndex, tempAction.val));
                tempAction = null;
            }
            List<Integer> days = new ArrayList<>();
            List<Long> currentVals = new ArrayList<>();
            for(int i = 1; i < totalMonths.size(); i++){
                if(totalMonths.get(i).selected){
                    int dayFrom;
                    int dayTo;
                    dayFrom = totalMonths.get(i).planDays_from;
                    dayTo = totalMonths.get(i).planDays_to;

                    for(int d = dayFrom; d <= dayTo; d++ ){
                        long initialValue = planDays.get(d).value;
                        long  pasteValue = 0;
                        if(copiedDays.length == 31){
                            pasteValue = copiedDays[planDays.get(d).day-1];
                        }if(copiedDays.length == 1){
                            pasteValue = copiedDays[0];
                        }

                        if(initialValue != pasteValue){
                            days.add(d);
                            currentVals.add(initialValue);
                            planDays.get(d).setNewValue(d, pasteValue,false);
                        }
                    }
                }
            }
            finishAutoAction(days, currentVals);
        }
    }

    private void pasteWeekTotal(){
        if(copiedDays.length == 1 || copiedDays.length == 7){
            if(tempAction != null){
                backActions.add(new Action(tempAction.dayIndex, tempAction.val));
                tempAction = null;
            }
            List<Integer> days = new ArrayList<>();
            List<Long> currentVals = new ArrayList<>();
            for(int i = 1; i < totalWeeks.size(); i++){
                if(totalWeeks.get(i).selected){
                    int dayFrom;
                    int dayTo;
                    dayFrom = totalWeeks.get(i).planDays_from;
                    dayTo = totalWeeks.get(i).planDays_to;

                    for(int d = dayFrom; d <= dayTo; d++ ){
                        long initialValue = planDays.get(d).value;
                        long  pasteValue = 0;
                        if(copiedDays.length == 7){
                            pasteValue = copiedDays[d - dayFrom];
                        }if(copiedDays.length == 1){
                            pasteValue = copiedDays[0];
                        }

                        if(initialValue != pasteValue){
                            days.add(d);
                            currentVals.add(initialValue);
                            planDays.get(d).setNewValue(d, pasteValue,false);
                        }
                    }
                }
            }
            finishAutoAction(days, currentVals);
        }
    }

    public void updateCopyPasteState(){
        boolean canCopy;
        if(selectedRowsCount==1){
            canCopy = true;
        }else{
            canCopy = false;
        }
        mInputManager.setCopyPasteBtnEnabled(canCopy, canPaste());
    }

    public boolean canPaste(){

        int size;
        if(copiedDays == null){
            size = 0;
        }else {
            size = copiedDays.length;
        }
        if(isSelectionMode()&&(size > 0)){
           if(copiedDays.length == 1){
               return true;
           }else if(planDaySelection){
               return false;
           }else {
               return true;
           }
        }else {
            return false;
        }
    }



    public void copyAction(){
        if(selectedRowsCount == 1){
            if(localTotalsSelection){
                if(weekPresentation){
                    copyWeekTotal();
                }else {
                    copyMonthTotal();
                }
            }else if(planDaySelection){
                copyDay();
            }
            updateCopyPasteState();
        }
    }

    private void copyDay(){
        copiedDays = new long[1];
        if (totalIndex !=-1) {
            int dayFrom;
            int dayTo;
            if (weekPresentation) {
                dayFrom = totalWeeks.get(totalIndex).planDays_from;
                dayTo = totalWeeks.get(totalIndex).planDays_to;
            } else {
                dayFrom = totalMonths.get(totalIndex).planDays_from;
                dayTo = totalMonths.get(totalIndex).planDays_to;
            }
            for (int d = dayFrom; d <= dayTo; d++) {
                if(planDays.get(d).selected){
                    copiedDays[0] = planDays.get(d).value;

                    break;
                }
            }
        }
    }

    private void copyMonthTotal(){
        copiedDays = new long[31];
        for(int i = 1; i < totalMonths.size(); i++){
            if(totalMonths.get(i).selected){
                int dayFrom = totalMonths.get(i).planDays_from;
                int dayTo = totalMonths.get(i).planDays_to;
                for(int d = 0; d < copiedDays.length; d++){
                    if((d + dayFrom) <= dayTo){
                        copiedDays[d] = planDays.get(d + dayFrom).value;
                    }else {
                        copiedDays[d] = 0;
                    }
                }

                break;
            }
        }
    }

    private void copyWeekTotal(){
        copiedDays = new long[7];
        for(int i = 1; i < totalWeeks.size(); i++){
            if(totalWeeks.get(i).selected){
                int dayFrom = totalWeeks.get(i).planDays_from;
                int dayTo = totalWeeks.get(i).planDays_to;
                int addition = 0;
                /*
                int firstWeekDay = planDays.get(dayFrom).dayWeek;
                if(firstWeekDay > 1){
                    addition = firstWeekDay - 1;
                }*/
                for(int d = 0; d < copiedDays.length; d++){
                    copiedDays[d] = planDays.get(d + dayFrom).value;
                    /*if(d < addition){
                        copiedDays[d] = 0;
                    }else if((d - addition + dayFrom) <= dayTo){
                        copiedDays[d] = planDays.get(d - addition + dayFrom).value;
                    }else {
                        copiedDays[d] = 0;
                    }*/
                }
                break;
            }
        }
    }


    // -----ERASER---------------------------------------------

    public void eraseAction(){
        if(selectedRowsCount > 0){
            if(tempAction != null){
                backActions.add(new Action(tempAction.dayIndex, tempAction.val));
                tempAction = null;
            }

           if(localTotalsSelection){
               if(weekPresentation){
                   localTotalEraser(totalWeeks);
               }else {
                   localTotalEraser(totalMonths);
               }
           }else if(planDaySelection){
               daysEraser();
           }
        }
    }

    public void daysEraser(){
        if (totalIndex !=-1) {
            int dayFrom;
            int dayTo;

            List<Integer> days = new ArrayList<>();
            List<Long> currentVals = new ArrayList<>();

            if (weekPresentation) {
                dayFrom = totalWeeks.get(totalIndex).planDays_from;
                dayTo = totalWeeks.get(totalIndex).planDays_to;
            } else {
                dayFrom = totalMonths.get(totalIndex).planDays_from;
                dayTo = totalMonths.get(totalIndex).planDays_to;
            }
            for (int d = dayFrom; d <= dayTo; d++) {
               if(planDays.get(d).selected){
                   if(planDays.get(d).value != 0){
                       days.add(d);
                       currentVals.add(planDays.get(d).value);
                       planDays.get(d).setNewValue(d, 0,false);
                   }
               }
            }
            finishAutoAction(days, currentVals);
        }
    }

    public void localTotalEraser(List<LocalTotal> localTotals){
        List<Integer> days = new ArrayList<>();
        List<Long> currentVals = new ArrayList<>();

        for(int i = 1; i < localTotals.size(); i++){
            if(localTotals.get(i).selected){
                int dayFrom;
                int dayTo;
                dayFrom = localTotals.get(i).planDays_from;
                dayTo = localTotals.get(i).planDays_to;
                for(int d = dayFrom; d <= dayTo; d++ ){
                    if(planDays.get(d).value != 0){
                        days.add(d);
                        currentVals.add(planDays.get(d).value);
                        planDays.get(d).setNewValue(d, 0,false);
                    }
                }

            }
        }
        finishAutoAction(days, currentVals);
    }

    public void finishAutoAction(List<Integer> days, List<Long> currentVals ){
        if(days.size() > 0){
            backActions.add(new Action(days, currentVals));
            if(forwardActions.size() != 0){
                forwardActions.clear();
            }

            mInputManager.updateUndoRedoBtnsState(getUndoRedoState());
        }
    }


    // -----SELECTION HANDLE---------------------------------------------

    public int getSelectAllCancelState(){
        return 1;
    }

    public void selectAllCancel(){
        if(maxSelectedRowsCount == selectedRowsCount){
            dropSelection();
        }else {
            selectAll();
            updateCopyPasteState();
        }
    }

    public void dropSelection(){
        if(localTotalsSelection){
            dropAllTotals();
            //mInputManager.planTableAdapterDatasetChanged(false);
        }else if(planDaySelection){
            dropDaySelection();
        }
    }

    public void selectAll(){
        if(localTotalsSelection){
            selectAllTotals();
            mInputManager.planTableAdapterDatasetChanged(false);
        }else if(planDaySelection){
            selecAllDays();
            mInputManager.rebindSubRVAdapter();
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
        mInputManager.updateSelectAllCancelBtnsState(CANCEL);
    }


    public void selecAllDays(){
        if (totalIndex !=-1){
            int dayFrom;
            int dayTo;
            if(weekPresentation){
                dayFrom = totalWeeks.get(totalIndex).planDays_from;
                dayTo = totalWeeks.get(totalIndex).planDays_to;
            }else {
                dayFrom = totalMonths.get(totalIndex).planDays_from;
                dayTo = totalMonths.get(totalIndex).planDays_to;
            }
            for(int i = dayFrom; i <= dayTo; i++ ){
                planDays.get(i).selected = true;
            }
            selectedRowsCount = maxSelectedRowsCount;
            mInputManager.updateSelectAllCancelBtnsState(CANCEL);
        }
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


    public void dropDaySelection(){
        if (totalIndex !=-1){
            int dayFrom;
            int dayTo;
            if(weekPresentation){
                dayFrom = totalWeeks.get(totalIndex).planDays_from;
                dayTo = totalWeeks.get(totalIndex).planDays_to;
            }else {
                dayFrom = totalMonths.get(totalIndex).planDays_from;
                dayTo = totalMonths.get(totalIndex).planDays_to;
            }
            for(int i = dayFrom; i <= dayTo; i++ ){
                planDays.get(i).selected = false;
            }
            selectedRowsCount = 0;
            stopRowsSelection(false);
       }
    }


    public void setRowUnselected(int position, boolean total){
        if(total){
            if(weekPresentation){
                totalWeeks.get(position).selected = false;
            }else {
                totalMonths.get(position).selected = false;
            }
        }else{
            planDays.get(position).selected = false;
        }
        selectedRowsCount--;
        if(selectedRowsCount == 0){
            stopRowsSelection(total);
        }

        if(selectedRowsCount == 1){
            updateCopyPasteState();
        }

        if(selectedRowsCount == maxSelectedRowsCount -1 ){
            mInputManager.updateSelectAllCancelBtnsState(SELECT_ALL);
        }
    }

    public void stopRowsSelection(boolean total){
        if(total){
            localTotalsSelection = false;
            mInputManager.planTableAdapterDatasetChanged(false);
        }else{
            planDaySelection = false;
            totalIndex = -1;
            mInputManager.rebindSubRVAdapter();
        }
        mInputManager.updateSelectAllCancelBtnsState(HIDDEN);
        mInputManager.setEraseBtnEnabled(false);
        updateCopyPasteState();
    }


    public void setRowSelected(int position, boolean total){
        if(total){
            if(weekPresentation){
                totalWeeks.get(position).selected = true;
            }else {
                totalMonths.get(position).selected = true;
            }
        }else{
            planDays.get(position).selected = true;
        }
        selectedRowsCount++;
        if(selectedRowsCount == 1){
            if(total){
                localTotalsSelection = true;
                mInputManager.planTableAdapterDatasetChanged(false);
                maxSelectedRowsCount = getMaxTotalSelection();
            }else{
                planDaySelection = true;
                maxSelectedRowsCount = getMaxDaysSelection(position);
                totalIndex = getTotalIndex(position);
            }
            mInputManager.updateSelectAllCancelBtnsState(SELECT_ALL);
            mInputManager.setEraseBtnEnabled(true);
            updateCopyPasteState();
        }
        if(selectedRowsCount == 2){
            updateCopyPasteState();
        }

        if(selectedRowsCount == maxSelectedRowsCount){
            mInputManager.updateSelectAllCancelBtnsState(CANCEL);
        }
    }




    public boolean isDaySelected(int position){
        return planDays.get(position).selected;
    }

    public void updateSelectedRowsCount(int num){

    }

    public boolean isSelectionMode(){
        if(localTotalsSelection || planDaySelection){
            return true;
        }else {
            return false;
        }
    }

    public boolean isTotalSelected(int position){
        if(weekPresentation){
            return totalWeeks.get(position).selected;
        }else {
            return totalMonths.get(position).selected;
        }
    }
    public int getMaxTotalSelection(){
        if(weekPresentation){
            return totalWeeks.size() - 1;
        }else {
            return totalMonths.size() - 1;
        }
    }

    public int getMaxDaysSelection(int position){
        LocalTotal tempLT;
        if(weekPresentation){
            tempLT = totalWeeks.get(planDays.get(position).totalWeekIndex);
            return tempLT.planDays_to - tempLT.planDays_from + 1;
        }else {
            tempLT = totalMonths.get(planDays.get(position).totalMonthIndex);
            return tempLT.planDays_to - tempLT.planDays_from + 1;
        }
    }

    public int getTotalIndex(int dayPosition){
        if(weekPresentation){
            return planDays.get(dayPosition).totalWeekIndex;
        }else {
            return planDays.get(dayPosition).totalMonthIndex;
        }
    }


    //--------UNDO--REDO-----------------------------

    public void saveAction(int dayIndex, long val){
         if(tempAction == null){
             tempAction = new Action(dayIndex, val);
             if (forwardActions.size() != 0){
                 forwardActions.clear();
             }
             mInputManager.updateUndoRedoBtnsState(getUndoRedoState());
         }else if (tempAction.dayIndex.get(0) != dayIndex) {
             backActions.add(new Action(tempAction.dayIndex.get(0), tempAction.val.get(0)));
             tempAction.dayIndex.set(0, dayIndex);
             tempAction.val.set(0, val);
         }
    }

    public void executeUndoAction(){
        if(tempAction != null){
            backActions.add(new Action(tempAction.dayIndex, tempAction.val));
            tempAction = null;
        }
        int BAsize = backActions.size();
        if(BAsize >= 1){
            List<Integer> days = backActions.get(BAsize-1).dayIndex;
            List<Long> vals = backActions.get(BAsize-1).val;

            int lenght = days.size();
            List<Long> currentVals = new ArrayList<>();

            for(int i = 0; i < lenght; i++){
                currentVals.add(planDays.get(days.get(i)).value);
                planDays.get(days.get(i)).setNewValue(days.get(i), vals.get(i), false);
            }
            forwardActions.add(new Action(days, currentVals));
            backActions.remove(BAsize - 1);
            mInputManager.updateUndoRedoBtnsState(getUndoRedoState());
        }
    }

    public void executeRedoAction(){
        int FAsize = forwardActions.size();
        if(FAsize >= 1){
            List<Integer> days = forwardActions.get(FAsize-1).dayIndex;
            List<Long> vals = forwardActions.get(FAsize-1).val;

            int lenght = days.size();
            List<Long> currentVals = new ArrayList<>();
            for(int i = 0; i < lenght; i++){
                currentVals.add(planDays.get(days.get(i)).value);
                planDays.get(days.get(i)).setNewValue(days.get(i), vals.get(i), false);
            }
            backActions.add(new Action(days, currentVals));
            forwardActions.remove(FAsize - 1);
            mInputManager.updateUndoRedoBtnsState(getUndoRedoState());
        }
    }

    public int getUndoRedoState(){

        int BAsize = backActions.size();
        int FAsize = forwardActions.size();

        if((tempAction != null || BAsize != 0)&& FAsize == 0){
            mInputManager.setSaveButtonEnabled(true);
            return UNDO_NO_REDO;
        } else if((tempAction != null || BAsize != 0)&& FAsize != 0){
            mInputManager.setSaveButtonEnabled(true);
            return UNDO_REDO;
        } else if((tempAction == null && BAsize == 0)&& FAsize != 0){
            mInputManager.setSaveButtonEnabled(true);
            return NO_UNDO_REDO;
        }else {
            return NO_UNDO_NO_REDO;
        }
    }

    //-------CONVERTERS-------------------------------

    public static int getDayCode(int year, int month, int day){
        return year*10000 + (month + 1)*100 + day;
    }

    public String monthDayConverter(int i){
        if(i<=9){
            return "0"+Integer.toString(i);
        }else {
            return Integer.toString(i);
        }
    }


    public static long stringNumToLong(String s){

        int len = s.length();
        Long mutiplier = 100L;
        StringBuilder stringBuilder =  new StringBuilder();
        for(int i = 0; i < len; i++){
            char ch = s.charAt(i);
            if(ch == '.' || ch == ','){
                int digAfterDot = (len - i - 1)<=2?(len - i - 1):2;
                for (int z = 1; z <= digAfterDot; z++){
                    mutiplier = 1L;
                    stringBuilder.append(s.charAt(i + z));
                    if(digAfterDot ==1){
                        stringBuilder.append('0');
                    }
                }
                break;
            }else{
                stringBuilder.append(ch);
            }
        }
        try {
            return Long.parseLong(stringBuilder.toString())*mutiplier;
        }catch (Exception e){
            return 0L;
        }
    }

    public static String longNumToString(long l){

        String rowNumber = Long.toString(l);
        int len = rowNumber.length();

        if((l > 0 && len >= 3) || (l < 0 && len >= 4)) {
            int newLen;
            if (l < 0) {
                newLen = len + 1 + (len - 4) / 3;
            } else {
                newLen = len + 1 + (len - 3) / 3;
            }
            char[] tempCharArray = new char[newLen];
            int j;
            int adj = len - newLen;

            for (j = newLen; j > 0; j--) {
                if (newLen - j == 2) {
                    tempCharArray[j - 1] = '.';
                    adj++;
                } else if (j != 1 && (newLen - 2 - j) % 4 == 0) {
                    tempCharArray[j - 1] = ' ';
                    adj++;
                } else {
                    tempCharArray[j - 1] = rowNumber.charAt(j + adj - 1);
                }
            }
            return new String(tempCharArray);

        }else if(l == 0){
            return "-";
        }
        else {
            return longToStringWithDot(l);
        }

    }

    public static String longToStringWithDot(long l){
        String rowNumber = Long.toString(l);
        int len = rowNumber.length();

        if((l > 0 && len >= 3) || (l < 0 && len >= 4)) {
            int newLen;
            newLen = len + 1;

            char[] tempCharArray = new char[newLen];
            int j;
            int adj = len - newLen;

            for (j = newLen; j > 0; j--) {
                if (newLen - j == 2) {
                    tempCharArray[j - 1] = '.';
                    adj++;
               } else {
                    tempCharArray[j - 1] = rowNumber.charAt(j + adj - 1);
                }
            }
            return new String(tempCharArray);
        }else if(l != 0){
            if(l > 0){
                char[] tempCharArray = {'0','.','0','0'};
                if(len == 1){
                    tempCharArray[3] =  rowNumber.charAt(0);
                }else if (len == 2){
                    tempCharArray[2] =  rowNumber.charAt(0);
                    tempCharArray[3] =  rowNumber.charAt(1);
                }
                return new String(tempCharArray);
            }else {
                char[] tempCharArray = {'-','0','.','0','0'};
                if(len == 2){
                    tempCharArray[4] =  rowNumber.charAt(1);
                }else if (len == 3){
                    tempCharArray[3] =  rowNumber.charAt(1);
                    tempCharArray[4] =  rowNumber.charAt(2);
                }
                return new String(tempCharArray);
            }
        }else {
            return "";
        }
    }

    //------------------------------------------------

    public void setPlanDayString(int d, String s){
          planDays.get(d).setNewValue(d, stringNumToLong(s), true);
    }

    public String getPlanDayDotValue(int position){
        return longToStringWithDot(planDays.get(position).value);
    }

    public String getPlanDayValue(int position){
        return longNumToString(planDays.get(position).value);
    }

    public String getPlanDayLabel(int position){
        if(weekPresentation){
            return planDays.get(position).getWeekLabel();
        }else {
            return planDays.get(position).getMonthLabel();
        }
    }


    public int getDayMapTo(int position){
        if(weekPresentation){
            return totalWeeks.get(position).planDays_to;
        }else {
            return totalMonths.get(position).planDays_to;
        }
    }

    public int getDayMapFrom(int position){
        if(weekPresentation){
            return totalWeeks.get(position).planDays_from;
        }else {
            return totalMonths.get(position).planDays_from;
        }
    }


    public int getTotalsSize(){
        if(weekPresentation){
            return totalWeeks.size();
        }else {
            return totalMonths.size();
        }
    }



    public String getTotalValue(int position){
        if(weekPresentation){
            return longNumToString(totalWeeks.get(position).localTotal);
        }else {
            return longNumToString(totalMonths.get(position).localTotal);
        }
    }


    public String getTotalLabel(int position){
        if(weekPresentation){
            return sWeek + " " +Integer.toString(totalWeeks.get(position).period) + ", " + Integer.toString(totalWeeks.get(position).year);
        }else {
            return mMonths[totalMonths.get(position).period] + " " +Integer.toString(totalMonths.get(position).year);
        }
    }

    public String getBtrValue(){
        return longNumToString(overAllTotal);
    }

    public String getBtrLabel(int position){
        if(weekPresentation){
            return sYear + " " + Integer.toString(totalWeeks.get(position).year);
        }else {
            return sYear + " " + Integer.toString(totalMonths.get(position).year);
        }
    }

    public boolean getRowType(int position){
        if(weekPresentation){
            return totalWeeks.get(position).btr;
        }else {
            return totalMonths.get(position).btr;
        }
    }

    public class PlanDay{
        int day;
        int week;
        int dayWeek;
        int month;
        int year;
        long rowId = -1;
        long categoryId;
        long value = 0L;
        long initialValue = 0L;
        boolean changed = false;
        public boolean selected = false;
        int totalWeekIndex;
        int totalMonthIndex;
        int dayCode;
        int totalMonthFrom;
        int totalMonthTo;



        public PlanDay(long categoryId, int day, int week, int dayWeek, int month, int year, int totalWeekIndex, int totalMonthIndex, int dayCode) {
            this.categoryId = categoryId;
            this.day = day;
            this.week = week;
            this.dayWeek = dayWeek;
            this.month = month;
            this.year = year;
            this.totalWeekIndex = totalWeekIndex;
            this.totalMonthIndex = totalMonthIndex;
            this.dayCode = dayCode;
        }

        public Plan getPlan(){
            Plan tempPlan;
            tempPlan = new Plan(
                    categoryId,
                    day,
                    month,
                    year,
                    dayCode,
                    totalMonthFrom,
                    totalMonthTo,
                    value
            );
            return tempPlan;
        }




        public long getInitialValue() {
            return initialValue;
        }

        public long getRowId() {
            return rowId;
        }

        public long getValue() {
            return value;
        }

        public int getDayCode() {
            return dayCode;
        }

        public void setInitialValue(long initialValue) {
            this.initialValue = initialValue;
        }

        public void setRowId(long rowId) {
            this.rowId = rowId;
        }

        public void setTotalMonthFrom(int totalMonthFrom) {
            this.totalMonthFrom = totalMonthFrom;
        }

        public void setTotalMonthTo(int totalMonthTo) {
            this.totalMonthTo = totalMonthTo;
        }

        public void setNewValue(int d, long v, boolean recordAction){
            if(value != v){
                if(v < 100000000000000L && v > - 100000000000000L ){ // restriction 1 trln for 1 day
                    changed = true;
                    if(recordAction){
                        saveAction(d, value);
                    }
                    long difference = v - value;
                    value = v;
                    totalWeeks.get(totalWeekIndex).updateTotalValue(difference);
                    totalMonths.get(totalMonthIndex).updateTotalValue(difference);
                    overAllTotal = overAllTotal + difference;

                }else {
                    //сделай тост
                }
            }
        }


        public String getMonthLabel(){
            if(day <= 9){
                return "0"+Integer.toString(day) + " - " + mWeekDays[dayWeek -1];
            }else {
                return Integer.toString(day) + " - " + mWeekDays[dayWeek -1];
            }
        }

        public String getWeekLabel(){
            if(day <= 9){
                return "0"+Integer.toString(day) + " " + shortMonths[month] + " - " + mWeekDays[dayWeek-1];
            }else {
                return Integer.toString(day) + " " + shortMonths[month] + " - " + mWeekDays[dayWeek-1];
            }
        }
    }


    //--------SUPPORTING CLASSES---------------------------

    public class LocalTotal{
        int planDays_from;
        int planDays_to;
        int period;
        int year;
        long localTotal = 0L;
        int yearTotalIndex = 0;
        boolean isWeek;
        long rowId = -1L;
        boolean btr;
        boolean selected = false;

        public LocalTotal(int period, int year, int planDays_from, int planDays_to, boolean isWeek, boolean btr){
            this.period = period;
            this.year = year;
            this.planDays_from = planDays_from;
            this.planDays_to = planDays_to;
            this.isWeek = isWeek;
            this.btr = btr;
        }

        public void updateTotalValue(long difference){
            localTotal = localTotal + difference;
        }
    }

    public class Action{
        public List<Integer> dayIndex;
        public List<Long> val;

        public Action(int dayIndex , long val){
            this.dayIndex = new ArrayList<>();
            this.val = new ArrayList<>();
            this.dayIndex.add(dayIndex);
            this.val.add(val);
        }

        public Action(List<Integer> dayIndex , List<Long> val){
            this.dayIndex = new ArrayList<>();
            this.val = new ArrayList<>();
            this.dayIndex = dayIndex;
            this.val = val;
        }

    }

    public interface InputManager{
        void activateInputLayout();
        void deactivateInputLayout();
        void setPlanInputEditDay(int planDay);
        void updateSubRvHeight(int winHeight);
        void setPlanDayString(int planDay, String s);
        void updateUndoRedoBtnsState(int stateKey);
        void updateSelectAllCancelBtnsState(int stateKey);
        void planTableAdapterDatasetChanged(boolean withSub);
        void rebindSubRVAdapter();
        void setEraseBtnEnabled(boolean enabled);
        void setCopyPasteBtnEnabled(boolean copyEnabled, boolean pasteEnabled);
        void makeCopiedToast();
        void lockFilterAndCollapsedRow();
        void setSaveButtonEnabled(boolean enabled);
        String[] getMonths();
        String[] getShortMonths();
        String[] getWeekDays();
        String getWeek();
        String getYear();
        void backButtonCollapse(boolean enabled);
    }

}
