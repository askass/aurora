package tech.aurorafin.aurora;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
//import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


import tech.aurorafin.aurora.dbRoom.CashFlowDB;
import tech.aurorafin.aurora.dbRoom.CategoryDao;
import tech.aurorafin.aurora.dbRoom.Operation;
import tech.aurorafin.aurora.dbRoom.OperationDao;
import tech.aurorafin.aurora.dbRoom.OperationTotal;
import tech.aurorafin.aurora.dbRoom.OperationTotalDao;
import tech.aurorafin.aurora.dbRoom.Plan;
import tech.aurorafin.aurora.dbRoom.PlanDao;
import tech.aurorafin.aurora.dbRoom.PlanTotal;
import tech.aurorafin.aurora.dbRoom.PlanTotalDao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public class DbService extends Service {

    public static final String CHANNEL_ID = "DbBindService";
    private static final int ID_SERVICE = 1;
    private static final int ID_TASKS_COMPLETE = 2;

    public static final int PLAN_SAVED = 1;
    public static final int PLAN_ERRORS_SOLVING = 2;
    public static final int OPERATION_SAVED = 3;
    public static final int OPERATION_ERRORS_SOLVING = 4;
    public static final int DONE = 5;

    NotificationManager mNotificationManager;
    private boolean needCopyPrev = false;

    // Binder given to clients
    private final IBinder mBinder = new DbServiceBinder();
    private boolean bound = false;
    DbServiceCallback mDbServiceCallback;
    HashMap<Long, Boolean> mapIdLocked;

    // Threads management
    ThreadPoolExecutor executor;
    public List<Future<?>> futures;
    private Handler handler = new Handler(Looper.getMainLooper());
    Future<?> tasksFinisher;

    //Database
    CashFlowDB db;
    private PlanDao planDao;
    private PlanTotalDao planTotalDao;
    private CategoryDao categoryDao;
    private OperationDao operationDao;
    private OperationTotalDao operationTotalDao;
    Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        db = CashFlowDB.getInstance(this);
        this.planDao = db.planDao();
        this.planTotalDao = db.planTotalDao();
        this.categoryDao = db.categoryDao();
        this.operationDao = db.operationDao();
        this.operationTotalDao = db.operationTotalDao();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        futures = new ArrayList<>();
        mapIdLocked = new HashMap<>();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        CashFlowDB.setLastDbCloseCorrect(this, false);
        //Log.d("MyTag", "Service onCreate()");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel(this);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        /*PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);*/
        Notification notification = serviceNotification(getString(R.string.running));
        mNotificationManager.cancel(ID_TASKS_COMPLETE);
        startForeground(ID_SERVICE, notification);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //Log.d("MyTag", "Service onBind()");
        if(tasksFinisher!=null && !tasksFinisher.isDone()){
            tasksFinisher.cancel(true);
        }
        bound = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        //Log.d("MyTag", "Service onRebind()");
        if(tasksFinisher!=null && !tasksFinisher.isDone()){
            tasksFinisher.cancel(true);
        }
        bound = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //Log.d("MyTag", "Service onUnbind()");
        bound = false;
        tasksFinisher = executor.submit(new Runnable() {
            @Override
            public void run() {
                runTasksFinisher();
            }
        });
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Log.d("MyTag", "Service onDestroy()");
    }



    public class DbServiceBinder extends Binder {
        public DbService getService() {
            return DbService.this;
        }
        public void setDbServiceCallback(DbServiceCallback dbServiceCallback){
            mDbServiceCallback = dbServiceCallback;
        }
    }

    private class ToastMaker implements Runnable{
        int toast;
        public ToastMaker(int toast) {
            this.toast = toast;
        }
        @Override
        public void run() {
            String sToast = null;
            switch (toast){
                case PLAN_SAVED:
                    sToast = getString(R.string.saved);
                    break;
                case PLAN_ERRORS_SOLVING:
                    sToast = getString(R.string.check_errors);
                case OPERATION_SAVED:
                    sToast = getString(R.string.saved);
                    break;
                case OPERATION_ERRORS_SOLVING:
                    sToast = getString(R.string.check_errors);
                    break;
                case DONE:
                    sToast = getString(R.string.done);
                    break;
            }
            if(sToast != null){
                Toast.makeText(mContext, sToast, Toast.LENGTH_SHORT).show();
            }

        }
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification serviceNotification(String text){
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.database_operations_service))
                .setContentText(text)
                .setSmallIcon(R.drawable.notification_icon)
                //.setContentIntent(pendingIntent)
                .build();
    }

    public interface DbServiceCallback {
        void unlockCategory(long categoryId);
        void lockCategory(long categoryId);
    }

    private void futuresRevisor(){
        for (int i = futures.size()-1 ; i>=0 ;i--) {
            if(futures.get(i).isDone()){
                futures.remove(i);
            }
        }
    }

    private void runTasksFinisher(){
        //Log.d("MyTag", "runTasksFinisher");
        Boolean needNotificationTasksCompleted = false;
        for(int i = 0;  i< futures.size(); i++){
            if(!futures.get(i).isDone()){
                mNotificationManager.notify(ID_SERVICE, serviceNotification(getString(R.string.completing_tasks)));
                needNotificationTasksCompleted = true;
                try {
                   futures.get(i).get();
                   if(bound){
                       //Log.d("MyTag", "Bound back;");
                       mNotificationManager.notify(ID_SERVICE, serviceNotification(getString(R.string.running)));
                       needNotificationTasksCompleted = false;
                       break;
                   }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        if(!bound){
            futuresRevisor();
            if(futures.size()!=0){
                runTasksFinisher();
            }else {
                //Log.d("MyTag", "stopSelf();");
                if(needCopyPrev){
                    //db.close();
                    CashFlowDB.copyPrevDB(this);
                }
                CashFlowDB.setLastDbCloseCorrect(this, true);
                if (needNotificationTasksCompleted){
                    mNotificationManager.notify(ID_TASKS_COMPLETE, serviceNotification(getString(R.string.tasks_complete)));
                }
                stopSelf();
            }
        }
    }

    private void lockCategory(long id){
        needCopyPrev = true;
        mapIdLocked.put(id, true);
        if(mDbServiceCallback!=null){
            mDbServiceCallback.lockCategory(id);
        }

    }

    private void unlockCategory(long id){
        mapIdLocked.remove(id);
        categoryDao.selLastCategoryUpdateTime(id, System.currentTimeMillis());
        if(mDbServiceCallback!=null){
            mDbServiceCallback.unlockCategory(id);
        }
    }

    public HashMap<Long, Boolean> getLockedCategories(){
        return mapIdLocked;
    }




    /*PLAN AND OPERATIONS FUNCTIONS*/

    public void clearDeletedCategoryData(long categoryId){
        if(categoryId != -1){
            needCopyPrev = true;
            Future<?> f =  executor.submit(new BkgDeletedCategoryCleaner(categoryId));
            futures.add(f);
            futuresRevisor();
        }

    }
    private class BkgDeletedCategoryCleaner implements Runnable{
        long categoryId;
        public BkgDeletedCategoryCleaner(long categoryId) {
            this.categoryId = categoryId;
        }
        @Override
        public void run() {
            planDao.deleteAllPlanByCategoryId(categoryId);
            planTotalDao.deleteAllPlanTotalsByCategoryId(categoryId);
            operationDao.deleteAllOperationsByCategoryId(categoryId);
            operationTotalDao.deleteAllOperationTotalsByCategoryId(categoryId);
        }
    }


    public void startDeleteOperationsTotals(long categoryId, int operationsYear, List<OperData.LocalTotal> localTotals) {
        if(categoryId!=-1){
            lockCategory(categoryId);
            Future<?> f =  executor.submit(new BkgOperationsTotalDeleter(categoryId, operationsYear, localTotals));
            futures.add(f);
            futuresRevisor();
        }
    }


    private class BkgOperationsTotalDeleter implements Runnable{
        long categoryId;
        int operationsYear;
        List<OperData.LocalTotal> localTotals;
        public BkgOperationsTotalDeleter(long categoryId, int operationsYear, List<OperData.LocalTotal> localTotals) {
            this.categoryId = categoryId;
            this.operationsYear = operationsYear;
            this.localTotals = localTotals;
        }
        @Override
        public void run() {
            deleteOperationsTotals(categoryId, operationsYear, localTotals);
            unlockCategory(categoryId);
        }
    }

    private void deleteOperationsTotals(long categoryId, int operationsYear, List<OperData.LocalTotal> localTotals) {
        Operation tempOperation = null;
        Operation tempOperation_prev = null;


        long totalYearSum = 0L;
        long totalYearCount = 0L;

        for (int z = 0; z < localTotals.size(); z++){

            tempOperation = null;
            tempOperation_prev = null;

            long totalDaySum = 0L;
            long totalMonthSum = 0L;
            long totalDayCount = 0L;
            long totalMonthCount = 0L;

            int dateCodeFrom = localTotals.get(z).dateCodeFrom;
            int dateCodeTo = localTotals.get(z).dateCodeTo;

            List<Operation> operations = operationDao.getSortedOperationsOfCategoryByRange(categoryId, dateCodeFrom, dateCodeTo);

            for(int i = 0; i < operations.size(); i++){
                    tempOperation = operations.get(i);

                    if(tempOperation_prev!=null){
                        if(tempOperation_prev.dateCode != tempOperation.dateCode){
                            if(totalDayCount!=0L){
                                updateTotalDayOperation(tempOperation_prev, -totalDaySum, -totalDayCount);
                                totalDaySum = 0;
                                totalDayCount = 0;
                            }
                        }
                        if(tempOperation_prev.totalMonthFrom != tempOperation.totalMonthFrom){
                            if(totalMonthCount!=0L){
                                updateTotalMonthOperation(tempOperation_prev, -totalMonthSum, -totalMonthCount);
                                totalMonthSum = 0;
                                totalMonthCount = 0;
                            }
                        }
                    }


                    totalYearSum = totalYearSum + tempOperation.value;
                    totalYearCount++;

                    totalDaySum = totalDaySum + tempOperation.value;
                    totalMonthSum = totalMonthSum + tempOperation.value;
                    tempOperation_prev =  tempOperation;

                    totalDayCount++;
                    totalMonthCount++;


                    operationDao.delete(tempOperation);

                }
                /*Last Totals Update*/
            if(tempOperation != null && totalDayCount!=0L){
                    updateTotalDayOperation(tempOperation, -totalDaySum, -totalDayCount);
            }
            if(tempOperation != null && totalMonthCount!=0L){
                updateTotalMonthOperation(tempOperation, -totalMonthSum, -totalMonthCount);
            }
        }

        //Сохранить год!!
        long totalYear;
        if(tempOperation != null && totalYearCount!=0L){
            totalYear = updateTotalYearOperation(tempOperation, -totalYearSum, -totalYearCount);
            if(!operationSaveFinalCheck(categoryId, operationsYear, totalYear)){
                operationsErrorsSolver(categoryId, operationsYear);
            }
        }
        handler.post(new ToastMaker(DONE));
    }


    public void startDeleteOperationsList(long categoryId, int operationsYear, List<Operation> operations) {
        if(categoryId!=-1){
            lockCategory(categoryId);
            Future<?> f =  executor.submit(new BkgOperationsListDeleter(categoryId, operationsYear, operations));
            futures.add(f);
            futuresRevisor();
        }
    }


    private class BkgOperationsListDeleter implements Runnable{
        long categoryId;
        int operationsYear;
        List<Operation> operations;
        public BkgOperationsListDeleter(long categoryId, int operationsYear, List<Operation> operations) {
            this.categoryId = categoryId;
            this.operationsYear = operationsYear;
            this.operations = operations;
        }
        @Override
        public void run() {
            deleteOperationsList(categoryId, operationsYear, operations);
            unlockCategory(categoryId);
        }
    }

    private void deleteOperationsList(long categoryId, int operationsYear, List<Operation> operations){
        Operation tempOperation = null;
        Operation tempOperation_prev = null;

        long totalDaySum = 0L;
        long totalMonthSum = 0L;
        long totalDayCount = 0L;
        long totalMonthCount = 0L;

        long totalYearSum = 0L;
        long totalYearCount = 0L;

        for(int i = 0; i < operations.size(); i++){
            tempOperation = operations.get(i);

            if(tempOperation_prev!=null){
                if(tempOperation_prev.dateCode != tempOperation.dateCode){
                    if(totalDayCount!=0L){
                        updateTotalDayOperation(tempOperation_prev, -totalDaySum, -totalDayCount);
                        totalDaySum = 0;
                        totalDayCount = 0;
                    }
                }
                if(tempOperation_prev.totalMonthFrom != tempOperation.totalMonthFrom){
                    if(totalMonthCount!=0L){
                        updateTotalMonthOperation(tempOperation_prev, -totalMonthSum, -totalMonthCount);
                        totalMonthSum = 0;
                        totalMonthCount = 0;
                    }
                }
            }


            totalYearSum = totalYearSum + tempOperation.value;
            totalYearCount++;

            totalDaySum = totalDaySum + tempOperation.value;
            totalMonthSum = totalMonthSum + tempOperation.value;
            tempOperation_prev =  tempOperation;

            totalDayCount++;
            totalMonthCount++;


            operationDao.delete(tempOperation);

        }
        /*Last Totals Update*/
        if(tempOperation != null && totalDayCount!=0L){
            updateTotalDayOperation(tempOperation, -totalDaySum, -totalDayCount);
        }
        if(tempOperation != null && totalMonthCount!=0L){
            updateTotalMonthOperation(tempOperation, -totalMonthSum, -totalMonthCount);
        }
        //Сохранить год!!
        long totalYear;
        if(tempOperation != null && totalYearCount!=0L){
            totalYear = updateTotalYearOperation(tempOperation, -totalYearSum, -totalYearCount);
            if(!operationSaveFinalCheck(categoryId, operationsYear, totalYear)){
                operationsErrorsSolver(categoryId, operationsYear);
            }
        }
        handler.post(new ToastMaker(DONE));
    }


    public void startDeleteOperation(long operationId, long categoryId) {
        if(categoryId!=-1){
            lockCategory(categoryId);
            Future<?> f =  executor.submit(new BkgOperationDeleter(operationId, categoryId));
            futures.add(f);
            futuresRevisor();
        }
    }

    private class BkgOperationDeleter implements Runnable{
        long operationId;
        long categoryId;;
        public BkgOperationDeleter(long operationId, long categoryId) {
            this.operationId = operationId;
            this.categoryId = categoryId;
        }
        @Override
        public void run() {
            Operation operation = operationDao.getOperationById(operationId);
            clearOperationTotals(operation);
            operationDao.deleteOperationById(operationId);
            unlockCategory(categoryId);
        }
    }


    public void startSavingOperation(Operation operation, long operationId, long initCategoryId){
        lockCategory(operation.categoryId);
        if(operation.categoryId != initCategoryId){
            lockCategory(initCategoryId);
        }
        Future<?> f =  executor.submit(new BkgOperationSaver(operation, operationId, initCategoryId));
        futures.add(f);
        futuresRevisor();
    }

    private class BkgOperationSaver implements Runnable{
        Operation mOperation;
        long operationId;
        long initCategoryId;
        public BkgOperationSaver(Operation operation, long operationId, long initCategoryId) {
            this.mOperation = operation;
            this.operationId = operationId;
            this.initCategoryId = initCategoryId;
        }
        @Override
        public void run() {
            /*try {
                Thread.sleep(5000);
            }catch (Exception e){}*/

            if(operationId == -1){
                saveNewOperation(mOperation);
            }else {
                mOperation.setId(operationId);
                updateExistingOperation(mOperation);
            }
            unlockCategory(mOperation.categoryId);
            if(mOperation.categoryId != initCategoryId){
                unlockCategory(initCategoryId);
            }
        }
    }

    private void updateExistingOperation(Operation operation) {
        long yearTotal;
        Operation operation_prev = operationDao.getOperationById(operation.id);
        if(operation_prev.categoryId != operation.categoryId){
            clearOperationTotals(operation_prev);
            yearTotal = setNewOperationTotals(operation);
        }else {
            yearTotal = runOperationTotalsUpdate(operation_prev, operation);
        }
        operationDao.update(operation);
        if(!operationSaveFinalCheck(operation.categoryId, operation.year, yearTotal)){
            operationsErrorsSolver(operation.categoryId, operation.year);
        }
        handler.post(new ToastMaker(OPERATION_SAVED));
    }

    private long runOperationTotalsUpdate(Operation operation_prev, Operation operation) {

        if(operation_prev.dateCode != operation.dateCode){
            updateTotalDayOperation(operation_prev, -operation_prev.value, -1);
            updateTotalDayOperation(operation, operation.value, 1);
        }else if(operation_prev.value != operation.value) {
            updateTotalDayOperation(operation, operation.value -operation_prev.value, 0);
        }

        if(operation_prev.totalMonthFrom != operation.totalMonthFrom){
            updateTotalMonthOperation(operation_prev, -operation_prev.value, -1);
            updateTotalMonthOperation(operation, operation.value, 1);
        }else if(operation_prev.value != operation.value) {
            updateTotalMonthOperation(operation, operation.value -operation_prev.value, 0);
        }

        if(operation_prev.year != operation.year){
           long prevYearTotal = updateTotalYearOperation(operation_prev, -operation_prev.value, -1);
            if(!operationSaveFinalCheck(operation_prev.categoryId, operation_prev.year, prevYearTotal)){
                operationsErrorsSolver(operation_prev.categoryId, operation_prev.year);
            }
            return updateTotalYearOperation(operation, operation.value, 1);
        }else if(operation_prev.value != operation.value) {
            return updateTotalYearOperation(operation, operation.value -operation_prev.value, 0);
        }else {
            int yearFrom = PlanData.getDayCode(operation.year, 0,1);
            int yearTo = PlanData.getDayCode(operation.year, 11,31);
            OperationTotal totalYear = operationTotalDao.getOperationTotalYearById(operation.categoryId, yearFrom, yearTo);
            return totalYear.value;
        }
    }

    private long setNewOperationTotals(Operation operation) {
        updateTotalDayOperation(operation, operation.value, 1);
        updateTotalMonthOperation(operation, operation.value, 1);
        return updateTotalYearOperation(operation, operation.value, 1);
    }

    private void clearOperationTotals(Operation operation) {
        updateTotalDayOperation(operation, -operation.value, -1);
        updateTotalMonthOperation(operation, -operation.value, -1);
        long yearTotal = updateTotalYearOperation(operation, -operation.value, -1);
        if(!operationSaveFinalCheck(operation.categoryId, operation.year, yearTotal)){
            operationDao.setOperationValueById(operation.id, 0L);
            operationsErrorsSolver(operation.categoryId, operation.year);
        }
    }

    private void saveNewOperation(Operation operation) {
        operationDao.insert(operation);
        updateTotalDayOperation(operation, operation.value, 1);
        updateTotalMonthOperation(operation, operation.value, 1);

        long yearTotal = updateTotalYearOperation(operation, operation.value, 1);
        if(!operationSaveFinalCheck(operation.categoryId, operation.year, yearTotal)){
            operationsErrorsSolver(operation.categoryId, operation.year);
        }

        handler.post(new ToastMaker(OPERATION_SAVED));
    }

    private void operationsErrorsSolver(long categoryId, int year){
        handler.post(new ToastMaker(OPERATION_ERRORS_SOLVING));
        operationTotalDao.deleteAllTotalsOfYearById(categoryId, year);
        Operation[] operations = operationDao.getSortedOperationsOfCategoryByYear(categoryId, year);

        Operation tempOperation = null;
        Operation tempOperation_prev = null;

        long totalDaySum = 0L;
        long totalMonthSum = 0L;
        long totalDayCount = 0L;
        long totalMonthCount = 0L;

        for(int i = 0; i < operations.length; i++){
            tempOperation = operations[i];

            if(tempOperation_prev!=null){
                if(tempOperation_prev.dateCode != tempOperation.dateCode){
                    if(totalDayCount!=0L){
                        operationTotalDao.insert(new OperationTotal(categoryId, false, false, true,
                                tempOperation_prev.dateCode, tempOperation_prev.dateCode, tempOperation_prev.day, tempOperation_prev.year, totalDaySum, totalDayCount));
                        totalDaySum = 0;
                        totalDayCount = 0;
                    }
                }
                if(tempOperation_prev.totalMonthFrom != tempOperation.totalMonthFrom){
                    if(totalMonthCount!=0L){
                        operationTotalDao.insert(new OperationTotal(categoryId, false, true, false,
                                tempOperation_prev.totalMonthFrom, tempOperation_prev.totalMonthTo, tempOperation_prev.month, tempOperation_prev.year, totalMonthSum, totalMonthCount));
                        totalMonthSum = 0;
                        totalMonthCount = 0;
                    }
                }
            }
            totalDaySum = totalDaySum + tempOperation.value;
            totalMonthSum = totalMonthSum + tempOperation.value;
            tempOperation_prev =  tempOperation;

            totalDayCount++;
            totalMonthCount++;


        }
        /*Last Totals Update*/
        if(tempOperation != null && totalDayCount!=0L){
            operationTotalDao.insert(new OperationTotal(categoryId, false, false, true,
                    tempOperation.dateCode, tempOperation.dateCode, tempOperation.day, tempOperation.year, totalDaySum, totalDayCount));
        }
        if(tempOperation != null && totalMonthCount!=0L){
            operationTotalDao.insert(new OperationTotal(categoryId, false, true, false,
                    tempOperation.totalMonthFrom, tempOperation.totalMonthTo, tempOperation.month, tempOperation.year, totalMonthSum, totalMonthCount));
        }
        //Сохранить год!!
        long sumOfMonths = operationTotalDao.sumOfMonthsOfYearById(categoryId, year);
        setNewTotalYearOperation(year, sumOfMonths, categoryId, operations.length);

    }

    private void setNewTotalYearOperation(int year, long sumOfMonths, long categoryId, long count) {
        int yearFrom = PlanData.getDayCode(year, 0,1);
        int yearTo = PlanData.getDayCode(year, 11,31);
        OperationTotal totalYear = operationTotalDao.getOperationTotalYearById(categoryId, yearFrom, yearTo);
        if(totalYear != null){
            if(count == 0L){
                operationTotalDao.delete(totalYear);
            }else {
                totalYear.value = sumOfMonths;
                totalYear.operations_count = count;
                operationTotalDao.update(totalYear);
            }
        }else if (sumOfMonths != 0L){
            totalYear = new OperationTotal(categoryId, true, false, false,
                    yearFrom, yearTo, year, year, sumOfMonths, count);
            operationTotalDao.insert(totalYear);
        }
    }


    private boolean operationSaveFinalCheck(long id, int year, long yearTotal){
        long sumOfDays = operationTotalDao.sumOfDaysOfYearById(id, year);
        long sumOfMonths = operationTotalDao.sumOfMonthsOfYearById(id, year);
        return (yearTotal == sumOfDays)&&(yearTotal == sumOfMonths);
    }

    private void updateTotalDayOperation(Operation operation, long value, long count) {
        OperationTotal dayTotal = operationTotalDao.getOperationTotalDayById(operation.categoryId, operation.dateCode, operation.dateCode);
        if(dayTotal != null){
            if((dayTotal.operations_count + count) == 0L){
                operationTotalDao.delete(dayTotal);
            }else {
                dayTotal.value = dayTotal.value + value;
                dayTotal.operations_count = dayTotal.operations_count + count;
                operationTotalDao.update(dayTotal);
            }
        }else {
            dayTotal = new OperationTotal(operation.categoryId, false, false, true,
                    operation.dateCode, operation.dateCode, operation.day, operation.year, value, 1);
            operationTotalDao.insert(dayTotal);
        }
    }
    private void updateTotalMonthOperation(Operation operation, long value, long count){
        OperationTotal totalMonth = operationTotalDao.getOperationTotalMonthById(operation.categoryId, operation.totalMonthFrom, operation.totalMonthTo);
        if(totalMonth != null){
            if((totalMonth.operations_count + count) == 0L){
                operationTotalDao.delete(totalMonth);
            }else {
                totalMonth.value = totalMonth.value + value;
                totalMonth.operations_count = totalMonth.operations_count + count;
                operationTotalDao.update(totalMonth);
            }
        }else {
            totalMonth = new OperationTotal(operation.categoryId, false, true, false,
                    operation.totalMonthFrom, operation.totalMonthTo, operation.month, operation.year, value, 1);
            operationTotalDao.insert(totalMonth);
        }
    }

    private long updateTotalYearOperation(Operation operation, long value, long count){
        int yearFrom = PlanData.getDayCode(operation.year, 0,1);
        int yearTo = PlanData.getDayCode(operation.year, 11,31);
        OperationTotal totalYear = operationTotalDao.getOperationTotalYearById(operation.categoryId, yearFrom, yearTo);
        if(totalYear != null){
            if((totalYear.operations_count + count) == 0L){
                operationTotalDao.delete(totalYear);
                //Log.d("MyTag"," operationTotalDao.delete(totalYear)");
                return 0L;
            }else {
                totalYear.value = totalYear.value + value;
                totalYear.operations_count = totalYear.operations_count + count;
                operationTotalDao.update(totalYear);
            }
        }else {
            totalYear = new OperationTotal(operation.categoryId, true, false, false,
                    yearFrom, yearTo, operation.year, operation.year, value, 1);
            operationTotalDao.insert(totalYear);
        }
        return totalYear.value;
    }





    /*PLAN SAVE*/

    public void startSavingPlan(List<PlanData.PlanDay> planDays, long overAllTotal, long categoryID, int year){
        lockCategory(categoryID);
        Future<?> f =  executor.submit(new BkgPlanSaver(planDays, overAllTotal, categoryID, year));
        futures.add(f);
        futuresRevisor();
    }

    private class BkgPlanSaver implements Runnable{
        List<PlanData.PlanDay> planDays;
        long overAllTotal;
        long categoryID;
        int year;
        public BkgPlanSaver(List<PlanData.PlanDay> planDays, long overAllTotal, long categoryID, int year) {
            this.planDays = planDays;
            this.overAllTotal = overAllTotal;
            this.categoryID = categoryID;
            this.year = year;
        }
        @Override
        public void run() {
            savePlan(planDays, overAllTotal, categoryID, year);
            unlockCategory(categoryID);
        }
    }

    private void savePlan(List<PlanData.PlanDay> planDays, long overAllTotal, long categoryID, int year){
        try {
            List<PlanData.PlanDay> tempPlanDays = new ArrayList<>(planDays);
            Plan tempPlan = null;
            Plan tempPlan_prev = null;
            long totalMonthDifference = 0L;
            for(int i = 0; i < tempPlanDays.size(); i++){
                PlanData.PlanDay tempPlanDay = tempPlanDays.get(i);
                if(tempPlanDay.getValue() != tempPlanDay.getInitialValue()){
                    tempPlan = tempPlanDays.get(i).getPlan();
                    long difference = tempPlanDay.getValue() - tempPlanDay.getInitialValue();
                    long id = tempPlanDays.get(i).getRowId();
                    //Thread.sleep(1000);
                    //Log.d("MyTag", "Sleeep");
                    if(id == -1){
                        //insert New Plan
                        insertNewPlan(tempPlan);
                    }else{
                        //update existing Plan
                        tempPlan.setId(id);
                        updatePlan(tempPlan);
                    }
                    /*Update Week Month Total*/
                    if(tempPlan_prev!=null){

                        if(tempPlan_prev.totalMonthFrom != tempPlan.totalMonthFrom){
                            if(totalMonthDifference!=0L){
                                updateTotalMonthPlan(tempPlan_prev, totalMonthDifference, tempPlan.categoryId);
                                totalMonthDifference = 0;
                            }
                        }
                    }

                    totalMonthDifference = totalMonthDifference + difference;
                    tempPlan_prev =  tempPlan;
                }
                if(Thread.interrupted()){
                    throw new InterruptedException();
                }
            }
            /*Last Totals Update*/
            if(tempPlan != null && totalMonthDifference!=0L){
                updateTotalMonthPlan(tempPlan, totalMonthDifference, tempPlan.categoryId);
            }

            if(planSaveFinalCheck(categoryID, year, overAllTotal)){
                updateTotalYearPlan(year, overAllTotal, categoryID);
            }else {
                planErrorsSolver(categoryID, year);
            }

            handler.post(new ToastMaker(PLAN_SAVED));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean planSaveFinalCheck(long id, int year, long overAllTotal){
        long sumOfDays = planDao.sumOfDaysOfYearById(id, year);
        long sumOfMonths = planTotalDao.sumOfMonthsOfYearById(id, year);
        return (overAllTotal == sumOfDays)&&(overAllTotal == sumOfMonths);
    }

    private long insertNewPlan(Plan tempPlan){
        return planDao.insert(tempPlan);
    }

    private long updatePlan(Plan tempPlan){
        if(tempPlan.value != 0){
            planDao.update(tempPlan);
            return tempPlan.id;
        }else {
            planDao.delete(tempPlan);
            return -1;
        }
    }



    private void updateTotalMonthPlan(Plan tempPlan, long difference, long categoryId){
        PlanTotal totalMonth = planTotalDao.getPlanTotalMonthById(categoryId, tempPlan.totalMonthFrom, tempPlan.totalMonthTo);
        if(totalMonth != null){
            if((totalMonth.value + difference) == 0L){
                planTotalDao.delete(totalMonth);
            }else {
                totalMonth.value = totalMonth.value + difference;
                planTotalDao.update(totalMonth);
            }
        }else {
            totalMonth = new PlanTotal(categoryId, false, true,
                    tempPlan.totalMonthFrom, tempPlan.totalMonthTo, tempPlan.month, tempPlan.year, difference);
            planTotalDao.insert(totalMonth);
        }
    }

    private void updateTotalYearPlan(int year, long overAllTotal, long categoryId){
        int yearFrom = PlanData.getDayCode(year, 0,1);
        int yearTo = PlanData.getDayCode(year, 11,31);
        PlanTotal totalYear = planTotalDao.getPlanTotalYearById(categoryId, yearFrom, yearTo);
        if(totalYear != null){
            if(overAllTotal == 0L){
                planTotalDao.delete(totalYear);
            }else {
                totalYear.value = overAllTotal;
                planTotalDao.update(totalYear);
            }

        }else if (overAllTotal != 0L){
            totalYear = new PlanTotal(categoryId, true, false,
                    yearFrom, yearTo, year, year, overAllTotal);
            planTotalDao.insert(totalYear);
        }
    }

    private void planErrorsSolver(long categoryId, int year){

        handler.post(new ToastMaker(PLAN_ERRORS_SOLVING));

        planTotalDao.deleteAllTotalsOfYearById(categoryId, year);
        Plan[] plans = planDao.getSortedPlanOfCategoryByYear(categoryId, year);

        Plan tempPlan = null;
        Plan tempPlan_prev = null;

        long totalMonthSum = 0L;

        for(int i = 0; i < plans.length; i++){
            tempPlan = plans[i];

            if(tempPlan_prev!=null){
                if(tempPlan_prev.totalMonthFrom != tempPlan.totalMonthFrom){
                    if(totalMonthSum!=0L){
                        planTotalDao.insert(new PlanTotal(categoryId, false, true,
                                tempPlan_prev.totalMonthFrom, tempPlan_prev.totalMonthTo, tempPlan_prev.month, tempPlan_prev.year, totalMonthSum));
                        totalMonthSum = 0;
                    }
                }
            }
            totalMonthSum = totalMonthSum + tempPlan.value;
            tempPlan_prev =  tempPlan;
        }
        /*Last Totals Update*/

        if(tempPlan != null && totalMonthSum!=0L){
            planTotalDao.insert(new PlanTotal(categoryId, false, true,
                    tempPlan.totalMonthFrom, tempPlan.totalMonthTo, tempPlan.month, tempPlan.year, totalMonthSum));
        }

        //Сохранить год!!
        long sumOfMonths = planTotalDao.sumOfMonthsOfYearById(categoryId, year);
        updateTotalYearPlan(year, sumOfMonths, categoryId);

    }
}
