package tech.aurorafin.aurora.dbRoom;

import android.content.Context;

import android.os.Handler;


import java.util.concurrent.ThreadPoolExecutor;



public class PlanRepository {

    private PlanDao planDao;
    private PlanTotalDao planTotalDao;
    public ThreadPoolExecutor mExecutor;
    private Handler handler;
    Context mContext;

    public long tempPlannedCategoryId = -1;
    public int tempPlannedYear = -1;
    public boolean tempWeekPresentation = false;
    public boolean canCollapseRow = false;
    public int tempCollapsedRow = -1;


    public PlanRepository(Context context, ThreadPoolExecutor executor, Handler handler){
        CashFlowDB db = CashFlowDB.getInstance(context);
        this.planDao = db.planDao();
        this.planTotalDao = db.planTotalDao();
        this.mExecutor = executor;
        this.handler  = handler;
        this.mContext = context;
        //this.mDbService = dbService;
    }


    public Plan[] getPlan(long categoryId, int year){
        return planDao.getPlanOfCategoryByYear(categoryId, year);
    }



    //--------currently useless




}
