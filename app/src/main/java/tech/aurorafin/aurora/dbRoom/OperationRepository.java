package tech.aurorafin.aurora.dbRoom;

import android.content.Context;
import android.os.Handler;

import tech.aurorafin.aurora.PlanData;
import tech.aurorafin.aurora.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


public class OperationRepository {

    Context mContext;
    private OperationDao operationDao;
    private OperationTotalDao operationTotalDao;
    private PlanDao planDao;

    private OperationsUpdateCallBack mOperationsUpdateCallBack;

    public ThreadPoolExecutor mExecutor;
    private Handler handler;

    UpdateNotifier updateNotifier;

    public long tempOperationCategoryId = -1;
    public int tempOperationYear = -1;
    public boolean tempWeekPresentation = false;
    public boolean canCollapseRow = false;
    public int tempCollapsedRow = -1;

    public List<Operation> lastOperations;

    /*Detailed operations for operations subRowAdapter*/
        public List<Operation> operationsForSubRowAdapter;

    /*Blue top bar*/
        public long last7daysFact[], last28daysFact[], last90daysFact[];
        public long last7daysPlan[], last28daysPlan[], last90daysPlan[];
        public boolean REV = true;
        public boolean EXP = false;
        public boolean CAP = false;

    /*Balance*/
        public int minYear;
        public int maxYear;
        public int selectedBalanceYear = -1;
        public List<BalanceRowData> balanceRowData;
        public long totalAssets;
        public long totalEquity;
        String cash;
        String profit;
        public boolean needCollapseBalance = false;
        public int dividerIndex = 0;


    public interface OperationsUpdateCallBack{
        void operationsUpdated();
    }


    public OperationRepository(Context context, ThreadPoolExecutor executor, Handler handler){
        CashFlowDB db = CashFlowDB.getInstance(context);
        this.operationDao = db.operationDao();
        this.operationTotalDao = db.operationTotalDao();
        planDao = db.planDao();
        this.mExecutor = executor;
        this.handler  = handler;
        this.mContext = context;
        lastOperations = new ArrayList<>();
        operationsForSubRowAdapter = new ArrayList<>();
        updateNotifier = new UpdateNotifier();

        /*Blue top bar*/
        last7daysFact = new long[3]; last28daysFact = new long[3]; last90daysFact = new long[3];
        last7daysPlan = new long[3]; last28daysPlan = new long[3]; last90daysPlan = new long[3];
        /*Balance*/
        balanceRowData = new ArrayList<>();
        cash = context.getString(R.string.cash);
        profit = context.getString(R.string.profit);
    }


    public OperationTotal[] getOperationMonthTotals(long categoryId, int year){
        return operationTotalDao.getOperationMonthTotalsOfCategoryByYear(categoryId, year);
    }
    public OperationTotal[] getOperationDayTotals(long categoryId, int year){
        return operationTotalDao.getOperationDayTotalsOfCategoryByYear(categoryId, year);
    }
    public OperationTotal getOperationYearTotal(long categoryId, int year){
        int yearFrom = PlanData.getDayCode(year, 0,1);
        int yearTo = PlanData.getDayCode(year, 11,31);
        return operationTotalDao.getOperationTotalYearById(categoryId, yearFrom, yearTo);
    }



    public void setOperationsUpdateCallBack(OperationsUpdateCallBack operationsUpdateCallBack) {
        this.mOperationsUpdateCallBack = operationsUpdateCallBack;
    }

    public synchronized void updateLastOperations(List<Category> sortedCategories, int todayDateCode,
                                                  int last7startDateCode, int last28startDateCode, int last90startDateCode){
        mExecutor.execute(new BkgLastOperationsUpdater(sortedCategories, todayDateCode,
                last7startDateCode, last28startDateCode, last90startDateCode));
    }

    private class BkgLastOperationsUpdater implements Runnable{
        List<Category> sortedCategories;
        int todayDateCode, last7startDateCode, last28startDateCode, last90startDateCode;
        public BkgLastOperationsUpdater(List<Category> sortedCategories, int todayDateCode,
                                        int last7startDateCode, int last28startDateCode, int last90startDateCode) {
            this.sortedCategories = sortedCategories;
            this.todayDateCode = todayDateCode;
            this.last7startDateCode = last7startDateCode;
            this.last28startDateCode = last28startDateCode;
            this.last90startDateCode = last90startDateCode;
        }
        @Override
        public void run() {
            lastOperations.clear();
            lastOperations = operationDao.getLastOperations();
            updateTopBlueBarSums(sortedCategories, todayDateCode,
                    last7startDateCode, last28startDateCode, last90startDateCode);
            maxYear = operationTotalDao.getMaxYear();
            minYear = operationTotalDao.getMinYear();
            if(mOperationsUpdateCallBack!=null){
                handler.post(updateNotifier);
            }
        }
    }

    private synchronized void updateTopBlueBarSums(List<Category> sortedCategories, int todayDateCode,
                                      int last7startDateCode, int last28startDateCode, int last90startDateCode) {
        Arrays.fill(last7daysFact, 0L); Arrays.fill(last28daysFact, 0L); Arrays.fill(last90daysFact, 0L);
        Arrays.fill(last7daysPlan, 0L); Arrays.fill(last28daysPlan, 0L); Arrays.fill(last90daysPlan, 0L);
        for(int i = 0; i < sortedCategories.size(); i++){
            long categoryId = sortedCategories.get(i).id;
            int type = sortedCategories.get(i).type;
            long temp7Fact = operationTotalDao.getSumOperationTotalDaysOfCategoryByDateCodeRange(categoryId,last7startDateCode, todayDateCode);
            long temp28Fact = operationTotalDao.getSumOperationTotalDaysOfCategoryByDateCodeRange(categoryId,last28startDateCode, todayDateCode);
            long temp90Fact = operationTotalDao.getSumOperationTotalDaysOfCategoryByDateCodeRange(categoryId,last90startDateCode, todayDateCode);
            long temp7Plan = planDao.getSumPlanOfCategoryByDateCodeRange(categoryId,last7startDateCode, todayDateCode);
            long temp28Plan = planDao.getSumPlanOfCategoryByDateCodeRange(categoryId,last28startDateCode, todayDateCode);
            long temp90Plan = planDao.getSumPlanOfCategoryByDateCodeRange(categoryId,last90startDateCode, todayDateCode);
            int index = type - 2;
            if(index>=0 && index<=2){
                last7daysFact[index] = last7daysFact[index] + temp7Fact;
                last28daysFact[index] = last28daysFact[index] + temp28Fact;
                last90daysFact[index] = last90daysFact[index] + temp90Fact;
                last7daysPlan[index] = last7daysPlan[index] + temp7Plan;
                last28daysPlan[index] = last28daysPlan[index] + temp28Plan;
                last90daysPlan[index] = last90daysPlan[index] + temp90Plan;
            }

        }
    }

    private class UpdateNotifier implements Runnable{
        @Override
        public void run() {
            mOperationsUpdateCallBack.operationsUpdated();
        }
    }

    /*Balance*/
    public void getBalance(int selectedBalanceYear, List<Category> sortedCategories) {
        balanceRowData.clear();
        totalAssets = 0;
        totalEquity = 0;
        BalanceRowData cashRow = new BalanceRowData(true,cash,cash, 0);
        BalanceRowData profitRow = new BalanceRowData(false, profit, profit, 0);
        balanceRowData.add(cashRow);
        balanceRowData.add(profitRow);
        List<BalanceRowData> assetsBalanceRowData = new ArrayList<>();
        List<BalanceRowData> equityBalanceRowData = new ArrayList<>();
        /*try {
            Thread.sleep(5000);
        }catch (Exception e){

        }*/
        for (int i = 0; i < sortedCategories.size(); i++){
            long categoryId = sortedCategories.get(i).id;
            int type = sortedCategories.get(i).type;
            boolean active = sortedCategories.get(i).active;
            long value = operationTotalDao.getCategorySumForBalance(categoryId, selectedBalanceYear);

            if(type == CategoriesRepository.ACategory.REVENUE){
                cashRow.value = cashRow.value + value;
                profitRow.value = profitRow.value + value;
                cashRow.appliedSelectedCategories.put(categoryId,true);
                profitRow.appliedSelectedCategories.put(categoryId,true);
                totalAssets = totalAssets + value;
                totalEquity = totalEquity + value;
            }else if(type == CategoriesRepository.ACategory.EXPENSE ||
                    (type == CategoriesRepository.ACategory.CAPITAL && !active) ){
                cashRow.value = cashRow.value - value;
                profitRow.value = profitRow.value - value;
                cashRow.appliedSelectedCategories.put(categoryId,true);
                profitRow.appliedSelectedCategories.put(categoryId,true);
                totalAssets = totalAssets - value;
                totalEquity = totalEquity - value;
            }else if(type == CategoriesRepository.ACategory.CAPITAL && active){
                cashRow.value = cashRow.value - value;
                cashRow.appliedSelectedCategories.put(categoryId,true);
                if(value >= 0){
                    BalanceRowData tempRow = new BalanceRowData(true,sortedCategories.get(i).name, sortedCategories.get(i).nick, categoryId);
                    tempRow.value = value;
                    tempRow.appliedSelectedCategories.put(categoryId,true);
                    assetsBalanceRowData.add(tempRow);
                }else {
                    totalAssets = totalAssets - value;
                    totalEquity = totalEquity - value;
                    BalanceRowData tempRow = new BalanceRowData(false,sortedCategories.get(i).name, sortedCategories.get(i).nick, categoryId);
                    tempRow.value = -value;
                    tempRow.appliedSelectedCategories.put(categoryId,true);
                    equityBalanceRowData.add(tempRow);
                }
            }
            //getCategorySumForBalance
        }

        if(assetsBalanceRowData.size() != 0){
            sortBalanceRows(assetsBalanceRowData);
            balanceRowData.addAll(assetsBalanceRowData);
        }

        if(equityBalanceRowData.size() != 0){
            sortBalanceRows(equityBalanceRowData);
            balanceRowData.addAll(equityBalanceRowData);
        }
    }

    private void sortBalanceRows(List<BalanceRowData> listToSort){
        Collections.sort(listToSort, new Comparator<BalanceRowData>() {
            @Override
            public int compare(BalanceRowData t1, BalanceRowData t2) {
                return Long.compare(t2.getValue(),t1.getValue());
            }
        });
    }

    /*Operations*/
    public Future<?> updateOperationsForSubRowAdapter(long categoryId, int dateCodeFrom, int dateCodeTo){
        return mExecutor.submit(new BkgOperationsUpdater(categoryId, dateCodeFrom, dateCodeTo));
    }

    public void updateOperationsForSubRowAdapterWoCallback(long categoryId, int dateCodeFrom, int dateCodeTo){
        operationsForSubRowAdapter = operationDao.getSortedOperationsOfCategoryByRange(categoryId, dateCodeFrom, dateCodeTo);
    }

    private class BkgOperationsUpdater implements Runnable{
        long categoryId;
        int dateCodeFrom;
        int dateCodeTo;

        public BkgOperationsUpdater(long categoryId, int dateCodeFrom, int dateCodeTo) {
            this.categoryId = categoryId;
            this.dateCodeFrom = dateCodeFrom;
            this.dateCodeTo = dateCodeTo;
        }

        @Override
        public void run() {
            operationsForSubRowAdapter.clear();
            /*try {
                Thread.sleep(5000);
            }catch (Exception e){

            }*/
            operationsForSubRowAdapter = operationDao.getSortedOperationsOfCategoryByRange(categoryId, dateCodeFrom, dateCodeTo);
            if(mOperationsUpdateCallBack!=null){
                handler.post(updateNotifier);
            }
        }
    }


    public static class BalanceRowData{
        public boolean asset;
        public long value = 0L;
        public String name;
        public String nick;
        public HashMap<Long, Boolean> appliedSelectedCategories;
        public long categoryId;

        public BalanceRowData(boolean asset, String name, String nick, long categoryId) {
            this.asset = asset;
            this.name = name;
            this.nick = nick;
            appliedSelectedCategories = new HashMap<>();
            this.categoryId = categoryId;
        }

        public long getValue() {
            return value;
        }
    }


}
