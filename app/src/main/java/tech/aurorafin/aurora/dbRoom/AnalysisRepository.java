package tech.aurorafin.aurora.dbRoom;

import android.content.Context;
import android.content.SharedPreferences;

import tech.aurorafin.aurora.PlanData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static tech.aurorafin.aurora.MainActivity.RATE_FILE_KEY;
import static tech.aurorafin.aurora.MainActivity.RATE_STATE_KEY;

public class AnalysisRepository {

    /*Filter Saver*/

        private final static String FILTER_STATE_FILE_KEY = "FILTER_STATE_FILE_KEY";
        private final static String FILTER_REV_STATE = "FILTER_REV_STATE";
        private final static String FILTER_EXP_STATE = "FILTER_EXP_STATE";
        private final static String FILTER_CAP_STATE = "FILTER_CAP_STATE";
        private final static String FILTER_DATE_CHIP_STATE = "FILTER_DATE_CHIP_STATE";
        private final static String FILTER_AGGREGATE_STATE = "FILTER_AGGREGATE_STATE";
        private final static String FILTER_PLAN_CHIP_STATE = "FILTER_PLAN_CHIP_STATE";
        private final static String FILTER_FACT_CHIP_STATE = "FILTER_FACT_CHIP_STATE";
        private final static String FILTER_ABS_STATE = "FILTER_ABS_STATE";
        private final static String FILTER_CUMULATIVE_STATE = "FILTER_CUMULATIVE_STATE";
        private final static String FILTER_PLAN_CHART_STATE = "FILTER_PLAN_CHART_STATE";
        private final static String FILTER_FACT_CHART_STATE = "FILTER_FACT_CHART_STATE";


    /*FilterSettings*/
        public boolean valid = false;

        public boolean REV = true;
        public boolean EXP = true;
        public boolean CAP = true;

        public int analysisDateCodeFrom = -1;
        public int analysisDateCodeTo = -1;
        public int analysisDateChip = -1;

        public boolean selectAll = true;
        public HashMap<Long, Boolean> appliedSelectedCategories;

        public boolean aggregate = true;

        public void setAppliedSelectedCategories(HashMap<Long, Boolean> appliedSelectedCategories) {
            this.appliedSelectedCategories = new HashMap<>(appliedSelectedCategories);
        }

        public boolean planChip = false;
        public boolean factChip = false;
        public boolean abs = true;

        public boolean cumulative = true;
        public boolean planChart = true;
        public boolean factChart = true;

    private PlanDao planDao;
    private PlanTotalDao planTotalDao;
    private OperationDao operationDao;
    private OperationTotalDao operationTotalDao;
    public ThreadPoolExecutor mExecutor;

    /*Database loadingMap*/
    int mainAnalysisTotalType;
    public List<AnalysisTotal[]> tempLoadingMap;
    public List<AnalysisTotal> tempDataMap;
    private HashMap<Integer, Integer> dateCodeIndexMap;

    /*DataHolder*/
    public HashMap<Long, AnalysisDataStruct> analysisDataMap;

    Context mContext;

    public AnalysisRepository(Context context, ThreadPoolExecutor executor) {
        mContext = context;
        CashFlowDB db = CashFlowDB.getInstance(context);
        this.planDao = db.planDao();
        this.planTotalDao = db.planTotalDao();
        this.operationDao = db.operationDao();
        this.operationTotalDao = db.operationTotalDao();
        this.mExecutor = executor;

        tempLoadingMap = new ArrayList<>();
        tempDataMap = new ArrayList<>();
        dateCodeIndexMap = new HashMap<>();
        analysisDataMap = new HashMap<>();

        loadFilterState();
    }


    public void saveLastFilterState(){
        SharedPreferences settings =
                mContext.getSharedPreferences(FILTER_STATE_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(FILTER_REV_STATE, REV);
        editor.putBoolean(FILTER_EXP_STATE, EXP);
        editor.putBoolean(FILTER_CAP_STATE, CAP);
        editor.putInt(FILTER_DATE_CHIP_STATE, analysisDateChip);
        editor.putBoolean(FILTER_AGGREGATE_STATE, aggregate);
        editor.putBoolean(FILTER_PLAN_CHIP_STATE, planChip);
        editor.putBoolean(FILTER_FACT_CHIP_STATE, factChip);
        editor.putBoolean(FILTER_ABS_STATE, abs);
        editor.putBoolean(FILTER_CUMULATIVE_STATE, cumulative);
        editor.putBoolean(FILTER_PLAN_CHART_STATE, planChart);
        editor.putBoolean(FILTER_FACT_CHART_STATE, factChart);
        editor.apply();
    }

    private void loadFilterState(){
        SharedPreferences sharedPref = mContext.getSharedPreferences(
                FILTER_STATE_FILE_KEY, Context.MODE_PRIVATE);
        REV = sharedPref.getBoolean(FILTER_REV_STATE, true);
        EXP = sharedPref.getBoolean(FILTER_EXP_STATE, true);
        CAP = sharedPref.getBoolean(FILTER_CAP_STATE, true);
        analysisDateChip = sharedPref.getInt(FILTER_DATE_CHIP_STATE, -1);
        aggregate = sharedPref.getBoolean(FILTER_AGGREGATE_STATE, true);
        planChip = sharedPref.getBoolean(FILTER_PLAN_CHIP_STATE, false);
        factChip = sharedPref.getBoolean(FILTER_FACT_CHIP_STATE, false);
        abs = sharedPref.getBoolean(FILTER_ABS_STATE, true);
        cumulative = sharedPref.getBoolean(FILTER_CUMULATIVE_STATE, true);
        planChart = sharedPref.getBoolean(FILTER_PLAN_CHART_STATE, true);
        factChart = sharedPref.getBoolean(FILTER_FACT_CHART_STATE, true);
    }

    public synchronized void startUpdater(int analysisDateCodeFrom, int analysisDateCodeTo, HashMap<Long, Boolean> appliedSelectedCategories){
        if(!valid || tempDataMap.size() == 0 ||
                tempDataMap.get(0).dateCodeFrom != analysisDateCodeFrom ||
                tempDataMap.get(tempDataMap.size()-1).dateCodeTo != analysisDateCodeTo){
            initializeData(analysisDateCodeFrom, analysisDateCodeTo, appliedSelectedCategories);
            valid = true;
        }else {
            updateData(appliedSelectedCategories);
        }

    }

    /*DATA UPDATER*/
    private void updateData(HashMap<Long, Boolean> appliedSelectedCategories){
        for (Long key : appliedSelectedCategories.keySet()) {
            long categoryId = key;
            if(!analysisDataMap.containsKey(key)){
                analysisDataMap.put(categoryId, dataStructLoader(categoryId));
            }
        }
    }

    /*DATA INITIALIZER*/
    private void initializeData(int analysisDateCodeFrom, int analysisDateCodeTo, HashMap<Long, Boolean> appliedSelectedCategories) {
        loadMapBuilder(analysisDateCodeFrom, analysisDateCodeTo);
        analysisDataMap.clear();
        for (Long key : appliedSelectedCategories.keySet()) {
            long categoryId = key;
            analysisDataMap.put(categoryId, dataStructLoader(categoryId));
        }
    }

    private AnalysisDataStruct dataStructLoader(long categoryId){
        long[] planList = tempListInitializer();

        long planTotal = 0;
        int planCount = 0;

        long[] factList =  tempListInitializer();
        long factTotal = 0;
        int factCount = 0;

        int sequentialDateCodeFrom; int sequentialDateCodeTo;
        /*-----------------*/
        if(tempLoadingMap.get(0)[0].isSequential()){
            sequentialDateCodeFrom = tempLoadingMap.get(0)[0].dateCodeFrom;
        }else {
            sequentialDateCodeFrom = tempLoadingMap.get(1)[0].dateCodeFrom;

            CountAndTotal planCountAndTotal = planPartialLoader(categoryId, 0);
            planList[0] = planCountAndTotal.total;
            planTotal = planTotal + planCountAndTotal.total;
            planCount = planCount + planCountAndTotal.count;

            CountAndTotal factCountAndTotal = factPartialLoader(categoryId, 0);
            factList[0]  = factCountAndTotal.total;
            factTotal = factTotal + factCountAndTotal.total;
            factCount = factCount + factCountAndTotal.count;
        }
        /*-----------------*/
        if(tempLoadingMap.get(tempLoadingMap.size()-1)[0].isSequential()){
            sequentialDateCodeTo = tempLoadingMap.get(tempLoadingMap.size()-1)[0].dateCodeTo;
        }else {
            sequentialDateCodeTo = tempLoadingMap.get(tempLoadingMap.size()-2)[0].dateCodeTo;

            CountAndTotal planCountAndTotal = planPartialLoader(categoryId, tempLoadingMap.size()-1);
            planList[tempLoadingMap.size()-1] = planCountAndTotal.total;
            planTotal = planTotal + planCountAndTotal.total;
            planCount = planCount + planCountAndTotal.count;

            CountAndTotal factCountAndTotal = factPartialLoader(categoryId, tempLoadingMap.size()-1);
            factList[tempLoadingMap.size()-1] = factCountAndTotal.total;
            factTotal = factTotal + factCountAndTotal.total;
            factCount = factCount + factCountAndTotal.count;
        }

        CountAndTotal planCountAndTotal = planSequentialLoader(mainAnalysisTotalType, planList, categoryId, sequentialDateCodeFrom, sequentialDateCodeTo);
        planTotal = planTotal + planCountAndTotal.total;
        planCount = planCount + planCountAndTotal.count;

        CountAndTotal factCountAndTotal = factSequentialLoader(mainAnalysisTotalType, factList, categoryId, sequentialDateCodeFrom, sequentialDateCodeTo);
        factTotal = factTotal + factCountAndTotal.total;
        factCount = factCount + factCountAndTotal.count;

        return new AnalysisDataStruct(categoryId, planList, factList, planTotal, factTotal, planCount, factCount);
    }

    public long[] tempListInitializer(){
        long[] tempList = new long[tempDataMap.size()];
        Arrays.fill(tempList, 0);
        return tempList;
    }

    private CountAndTotal factPartialLoader(long categoryId, int index){
        long partialSum = 0;
        int partialCount = 0;

        for (int i = 0; i<tempLoadingMap.get(index).length; i++){
            int type = tempLoadingMap.get(index)[i].getType();
            int dateCodeFrom = tempLoadingMap.get(index)[i].dateCodeFrom;
            int dateCodeTo = tempLoadingMap.get(index)[i].dateCodeTo;
            long sumFact = 0;
            if(type == AnalysisTotal.DAY_TYPE){
                sumFact = operationTotalDao.getSumOperationTotalDaysOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo);
            }else {
                sumFact = operationTotalDao.getSumOperationTotalMonthsOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo);
            }
            if(sumFact != 0){
                partialSum = partialSum + sumFact;
                partialCount++;
            }
        }
        return  new CountAndTotal(partialSum, partialCount);
    }

    private CountAndTotal factSequentialLoader(int totalType, long[] factList, long categoryId, int dateCodeFrom, int dateCodeTo ){
        CountAndTotal countAndTotal;
        switch(totalType){
            case AnalysisTotal.DAY_TYPE:
                countAndTotal = mapFactListWithTotals(operationTotalDao.getOperationDayTotalsOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo), factList);
                break;
            case AnalysisTotal.MONTH_TYPE:
                countAndTotal = mapFactListWithTotals(operationTotalDao.getOperationMonthTotalsOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo), factList);
                break;
            default:
                countAndTotal = mapFactListWithTotals(operationTotalDao.getOperationYearTotalsOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo), factList);
        }

        return countAndTotal;
    }

    private CountAndTotal mapFactListWithTotals(OperationTotal[] operationDayTotals, long[] factList) {
        int c = 0;
        long t = 0;
        for(int i = 0; i < operationDayTotals.length; i++){
            int index = dateCodeIndexMap.get(operationDayTotals[i].dateCodeFrom*100000000 +operationDayTotals[i].dateCodeTo);
            if(index < factList.length){
                factList[index] = operationDayTotals[i].value;
            }
            c++;
            t = t+ operationDayTotals[i].value;
        }
        return new CountAndTotal(t, c);
    }


    private CountAndTotal planPartialLoader(long categoryId, int index){
        long partialSum = 0;
        int partialCount = 0;

        for (int i = 0; i<tempLoadingMap.get(index).length; i++){
            int type = tempLoadingMap.get(index)[i].getType();
            int dateCodeFrom = tempLoadingMap.get(index)[i].dateCodeFrom;
            int dateCodeTo = tempLoadingMap.get(index)[i].dateCodeTo;
            long sumPlan = 0;
            if(type == AnalysisTotal.DAY_TYPE){
                sumPlan = planDao.getSumPlanOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo);
            }else {
                sumPlan = planTotalDao.getSumPlanTotalMonthsOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo);
            }
            if(sumPlan != 0){
                partialSum = partialSum + sumPlan;
                partialCount++;
            }
        }
        return  new CountAndTotal(partialSum, partialCount);
    }

    private CountAndTotal planSequentialLoader(int totalType, long[] planList, long categoryId, int dateCodeFrom, int dateCodeTo ){
        CountAndTotal countAndTotal;
        switch(totalType){
            case AnalysisTotal.DAY_TYPE:
                countAndTotal = mapPlanListWithDays(planDao.getPlanOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo), planList);
                break;
            case AnalysisTotal.MONTH_TYPE:
                countAndTotal = mapPlanListWithTotals(planTotalDao.getPlanTotalMonthsOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo), planList);
                break;
            default:
                countAndTotal = mapPlanListWithTotals(planTotalDao.getPlanTotalYearsOfCategoryByDateCodeRange(categoryId, dateCodeFrom, dateCodeTo), planList);
        }

        return countAndTotal;
    }

    private CountAndTotal mapPlanListWithTotals(PlanTotal[] planTotalMonths, long[] planList) {
        int c = 0;
        long t = 0;
        for(int i = 0; i < planTotalMonths.length; i++){
            int index = dateCodeIndexMap.get(planTotalMonths[i].dateCodeFrom*100000000 +planTotalMonths[i].dateCodeTo);
            planList[index] = planTotalMonths[i].value;
            c++;
            t = t+ planTotalMonths[i].value;
        }
        return new CountAndTotal(t, c);
    }

    private CountAndTotal mapPlanListWithDays(Plan[] planDays, long[] planList) {
        int c = 0;
        long t = 0;
        for(int i = 0; i < planDays.length; i++){
            int index = dateCodeIndexMap.get(planDays[i].dateCode*100000000 +planDays[i].dateCode);
            planList[index] = planDays[i].value;
            c++;
            t = t+ planDays[i].value;
        }

        return new CountAndTotal(t, c);
    }


    /*MAP BUILDERS*/
    public void loadMapBuilder(int dateCodeFrom, int dateCodeTo){

        int yearFrom = dateCodeFrom / 10000;
        int monthFrom = (dateCodeFrom % 10000) / 100 - 1;
        int dayFrom = dateCodeFrom % 100;

        int yearTo = dateCodeTo / 10000;
        int monthTo = (dateCodeTo % 10000) / 100 - 1;
        int dayTo = dateCodeTo % 100;

        Calendar start = Calendar.getInstance();
        //start.setFirstDayOfWeek(Calendar.SUNDAY);
        start.set(Calendar.YEAR, yearFrom);
        start.set(Calendar.MONTH, monthFrom);
        start.set(Calendar.DAY_OF_MONTH, dayFrom);

        Calendar end = Calendar.getInstance();
        //end.setFirstDayOfWeek(Calendar.SUNDAY);
        end.set(Calendar.YEAR, yearTo);
        end.set(Calendar.MONTH, monthTo);
        end.set(Calendar.DAY_OF_MONTH, dayTo);

        float daysBetween = (float)((end.getTimeInMillis() - start.getTimeInMillis()) / (1000*60*60*24) +1);

        tempLoadingMap.clear();
        tempDataMap.clear();
        dateCodeIndexMap.clear();

        if(daysBetween > 0 && daysBetween < 155){
            mainAnalysisTotalType = AnalysisTotal.DAY_TYPE;
            dayMapBuilder(start, end);
        }else if(daysBetween >= 155 && daysBetween < 1464){
            mainAnalysisTotalType = AnalysisTotal.MONTH_TYPE;
            monthMapBuilder(start, end, dateCodeTo);
        }else {
            mainAnalysisTotalType = AnalysisTotal.YEAR_TYPE;
            yearMapBuilder(start, end, dateCodeFrom, dateCodeTo);
        }
    }

    private void yearMapBuilder(Calendar start, Calendar end, int dateCodeFrom, int dateCodeTo){
        int day = start.get(Calendar.DAY_OF_MONTH);
        int month = start.get(Calendar.MONTH);
        int year = start.get(Calendar.YEAR);
        if (!(month == 0 && day == 1)){

            start.set(Calendar.DAY_OF_MONTH, start.getActualMaximum(Calendar.DAY_OF_MONTH));
            day = start.get(Calendar.DAY_OF_MONTH);
            month = start.get(Calendar.MONTH);
            year = start.get(Calendar.YEAR);
            int spanSize = day - dateCodeFrom % 100;
            AnalysisTotal daysPart = new AnalysisTotal(dateCodeFrom, PlanData.getDayCode(year, month, day), AnalysisTotal.DAY_TYPE, spanSize+1, year, 0, false);
            if(month!=11){
                start.add(Calendar.DATE, 1);
                day = start.get(Calendar.DAY_OF_MONTH);
                month = start.get(Calendar.MONTH);
                year = start.get(Calendar.YEAR);
                spanSize = (12 - month)*31;
                AnalysisTotal monthsPart = new AnalysisTotal(PlanData.getDayCode(year, month, day),
                        PlanData.getDayCode(year, 11, 31) , AnalysisTotal.MONTH_TYPE, spanSize, year, 0, false);
                AnalysisTotal[] at = new AnalysisTotal[2];
                at[0] = daysPart;
                at[1] = monthsPart;
                tempLoadingMap.add(at);

                AnalysisTotal adt = new AnalysisTotal(daysPart.getDateCodeFrom(),
                        monthsPart.getDateCodeTo(), AnalysisTotal.MONTH_TYPE, daysPart.getSpanSize()+monthsPart.getSpanSize(),
                        year, 0, false);
                tempDataMap.add(adt);
            }else {
                AnalysisTotal[] at = new AnalysisTotal[1];
                at[0] = daysPart;
                tempLoadingMap.add(at);
                tempDataMap.add(at[0]);
            }

            start.set(Calendar.YEAR, year +1);
            start.set(Calendar.MONTH, 0);
            start.set(Calendar.DAY_OF_MONTH, 1);
        }

        day = start.get(Calendar.DAY_OF_MONTH);
        month = start.get(Calendar.MONTH);
        year = start.get(Calendar.YEAR);


        for(int y = year; y < end.get(Calendar.YEAR); y++){
            int yearDateCodeFrom = PlanData.getDayCode(y, 0, 1);
            int yearDateCodeTo = PlanData.getDayCode(y, 11, 31);
            AnalysisTotal yearPart = new AnalysisTotal(yearDateCodeFrom, yearDateCodeTo,
                    AnalysisTotal.YEAR_TYPE, 365, y, y, true);
            AnalysisTotal[] at = new AnalysisTotal[1];
            at[0] = yearPart;
            tempLoadingMap.add(at);
            tempDataMap.add(yearPart);
            dateCodeIndexMap.put(yearDateCodeFrom*100000000 + yearDateCodeTo, tempDataMap.size()-1);

        }
        day = end.get(Calendar.DAY_OF_MONTH);
        month = end.get(Calendar.MONTH);
        year = end.get(Calendar.YEAR);

        if(month==11&&day==31){
            AnalysisTotal yearPart = new AnalysisTotal(PlanData.getDayCode(year, 0, 1),
                    PlanData.getDayCode(year, 11, 31) , AnalysisTotal.YEAR_TYPE, 365, year, year, true);
            AnalysisTotal[] at = new AnalysisTotal[1];
            at[0] = yearPart;
            tempLoadingMap.add(at);
            tempDataMap.add(yearPart);
            dateCodeIndexMap.put(yearPart.dateCodeFrom*100000000 + yearPart.dateCodeTo, tempDataMap.size()-1);
        }else {
            AnalysisTotal endDaysPart = new AnalysisTotal(PlanData.getDayCode(year, month, 1),
                    PlanData.getDayCode(year, month, day) , AnalysisTotal.DAY_TYPE, day, year, 0, false);
            if(month != 0){
                end.add(Calendar.MONTH, -1);
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                day = end.get(Calendar.DAY_OF_MONTH);
                month = end.get(Calendar.MONTH);
                year = end.get(Calendar.YEAR);

                int spanSize =   (month +1)*31;
                AnalysisTotal endMonthsPart = new AnalysisTotal(PlanData.getDayCode(year, 0, 1), PlanData.getDayCode(year, month, day),
                        AnalysisTotal.MONTH_TYPE, spanSize, 0,0, false);

                AnalysisTotal[] at = new AnalysisTotal[2];
                at[0] = endMonthsPart;
                at[1] = endDaysPart;
                tempLoadingMap.add(at);

                AnalysisTotal adt = new AnalysisTotal(endMonthsPart.getDateCodeFrom(), endDaysPart.getDateCodeTo(),
                         AnalysisTotal.MONTH_TYPE, endDaysPart.getSpanSize()+endMonthsPart.getSpanSize(),
                        year, 0, false);
                tempDataMap.add(adt);
            }else {
                AnalysisTotal[] at = new AnalysisTotal[1];
                at[0] = endDaysPart;
                tempLoadingMap.add(at);
                tempDataMap.add(endDaysPart);
            }
        }
    }

    private void monthMapBuilder(Calendar start, Calendar end, int dateCodeTo) {
        int i = 0;
        int prev_month = 0;
        int prev_year = 0;
        int prevDateCode = 0;
        int monthDayCodeFrom = 0;

        int spanSize = 0;

        Calendar cal = (Calendar)start.clone();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
        int totalMonthFrom = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        int totalMonthTo = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        int index = 0;
        for (Date date = start.getTime(); !start.after(end); start.add(Calendar.DATE, 1), date = start.getTime()){

            int day = start.get(Calendar.DAY_OF_MONTH);
            //int week = start.get(Calendar.WEEK_OF_YEAR);
            int month = start.get(Calendar.MONTH);
            int year = start.get(Calendar.YEAR);

            if(i == 0){
                prev_month = month;
                prev_year = year;
                monthDayCodeFrom = PlanData.getDayCode(year, month, day);
            }

            if (prev_month != month){
                boolean sequential = (monthDayCodeFrom == totalMonthFrom)&&(prevDateCode == totalMonthTo);
                int type = sequential ? AnalysisTotal.MONTH_TYPE : AnalysisTotal.DAY_TYPE;
                AnalysisTotal[] at = new AnalysisTotal[1];
                at[0] = new AnalysisTotal(monthDayCodeFrom, prevDateCode, type, spanSize, prev_year, prev_month, sequential);
                tempLoadingMap.add(at);
                tempDataMap.add(at[0]);
                dateCodeIndexMap.put(monthDayCodeFrom*100000000 + prevDateCode, index);

                cal = (Calendar)start.clone();
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
                totalMonthFrom = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                totalMonthTo = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                spanSize = 0;
                monthDayCodeFrom = PlanData.getDayCode(year, month, day);
                index++;
            }
            prevDateCode = PlanData.getDayCode(year, month, day);
            spanSize++;
            if(dateCodeTo == prevDateCode && spanSize!=0){

                boolean sequential = (monthDayCodeFrom == totalMonthFrom)&&(prevDateCode == totalMonthTo);
                int type = sequential ? AnalysisTotal.MONTH_TYPE : AnalysisTotal.DAY_TYPE;

                AnalysisTotal[] at = new AnalysisTotal[1];
                at[0] = new AnalysisTotal(monthDayCodeFrom, prevDateCode, type, spanSize+1, year, month, sequential);
                tempLoadingMap.add(at);
                tempDataMap.add(at[0]);
                dateCodeIndexMap.put(monthDayCodeFrom*100000000 + prevDateCode, index);
            }
            i++;

            prev_month = month;
            prev_year = year;
        }
    }

    private void dayMapBuilder(Calendar start, Calendar end){
        int i = 0;
        for (Date date = start.getTime(); !start.after(end); start.add(Calendar.DATE, 1), date = start.getTime()){
            int day = start.get(Calendar.DAY_OF_MONTH);
            int month = start.get(Calendar.MONTH);
            int tyear = start.get(Calendar.YEAR);
            int dateCode = PlanData.getDayCode(tyear, month, day);
            AnalysisTotal[] at = new AnalysisTotal[1];
            at[0] = new AnalysisTotal(dateCode, dateCode, AnalysisTotal.DAY_TYPE, 1, tyear, day, true);
            tempLoadingMap.add(at);
            tempDataMap.add(at[0]);
            dateCodeIndexMap.put(dateCode*100000000 + dateCode, i);
            i++;
        }
    }

    public static class CountAndTotal{
        long total;
        int count;

        public CountAndTotal(long total, int count) {
            this.total = total;
            this.count = count;
        }
    }

    public static class AnalysisDataStruct{
        public long categoryId;
        public long[] planList;
        public long[] factList;
        public long planTotal;
        public long factTotal;
        public int planCount;
        public int factCount;

        public String categoryName;
        public String categoryNick;
        public int categoryType;
        public long multiplier = 1;
        public boolean aggregate = true;
        public boolean total = true;

        public AnalysisDataStruct(long categoryId, long[] planList, long[] factList, long planTotal, long factTotal, int planCount, int factCount) {
            this.categoryId = categoryId;
            this.planList = planList;
            this.factList = factList;
            this.planTotal = planTotal;
            this.factTotal = factTotal;
            this.planCount = planCount;
            this.factCount = factCount;
        }

        public long getMPlanTotal() {
            return planTotal * multiplier;
        }

        public long getMFactTotal() {
            return factTotal * multiplier;
        }

    }

    public static class AnalysisTotal{
        public final static int DAY_TYPE = 1;
        //public final static int WEEK_TYPE = 7;
        public final static int MONTH_TYPE = 31;
        public final static int YEAR_TYPE = 365;

        public Long value = 0L;
        int dateCodeFrom;
        int dateCodeTo;
        int type;
        int spanSize;
        int year;
        int period;
        boolean sequential;

        public AnalysisTotal(int dateCodeFrom, int dateCodeTo, int type, int spanSize, int year, int period, boolean sequential) {
            this.dateCodeFrom = dateCodeFrom;
            this.dateCodeTo = dateCodeTo;
            this.type = type;
            this.spanSize = spanSize;
            this.year = year;
            this.period = period;
            this.sequential = sequential;
        }

        public int getDateCodeFrom() {
            return dateCodeFrom;
        }

        public int getDateCodeTo() {
            return dateCodeTo;
        }

        public int getType() {
            return type;
        }

        public int getSpanSize() {
            return spanSize;
        }

        public int getYear() {
            return year;
        }

        public int getPeriod() {
            return period;
        }

        public boolean isSequential() {
            return sequential;
        }
    }


}
