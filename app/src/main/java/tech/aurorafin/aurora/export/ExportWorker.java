package tech.aurorafin.aurora.export;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import tech.aurorafin.aurora.DbService;
import tech.aurorafin.aurora.PlanData;
import tech.aurorafin.aurora.R;
import tech.aurorafin.aurora.dbRoom.CashFlowDB;
import tech.aurorafin.aurora.dbRoom.Operation;
import tech.aurorafin.aurora.dbRoom.OperationDao;
import tech.aurorafin.aurora.dbRoom.OperationTotal;
import tech.aurorafin.aurora.dbRoom.OperationTotalDao;
import tech.aurorafin.aurora.dbRoom.Plan;
import tech.aurorafin.aurora.dbRoom.PlanDao;
import tech.aurorafin.aurora.dbRoom.PlanTotal;
import tech.aurorafin.aurora.dbRoom.PlanTotalDao;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import static android.content.Context.NOTIFICATION_SERVICE;

public class ExportWorker extends Worker {
    private final static int FG_NOTIF_ID = 202;
    private final static int RESULT_NOTIF_ID = 203;

    /*group bey keys*/
    public final static int NO_GROUP = 0;
    public final static int DAY_GROUP = 1;
    public final static int MONTH_GROUP = 2;
    public final static int YEAR_GROUP = 3;

    /*input keys*/
    public final static String DC_FROM = "DC_FROM";
    public final static String DC_TO = "DC_TO";
    public final static String GROUP_BY = "GROUP_BY";
    public final static String PLAN_TABLE = "PLAN_TABLE";
    public final static String FACT_TABLE = "FACT_TABLE";
    public final static String CAT_IDS = "CAT_IDS";
    public final static String CAT_TYPES = "CAT_TYPES";
    public final static String CAT_NAMES = "CAT_NAMES";
    public final static String AGGREGATE_IDS = "AGGREGATE_IDS";
    public final static String AGGREGATE_NAMES = "AGGREGATE_NAMES";
    public final static String COMMA = "COMMA";

    private PlanDao planDao;
    private PlanTotalDao planTotalDao;
    private OperationDao operationDao;
    private OperationTotalDao operationTotalDao;

    String[] shortMonths;
    String[] longTypes;
    String planStr;
    String  factStr;
    String delimiter;
    boolean comma;

    private NotificationManager notificationManager;

    public ExportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);

        CashFlowDB db = CashFlowDB.getInstance(context);
        this.planDao = db.planDao();
        this.planTotalDao = db.planTotalDao();
        this.operationDao = db.operationDao();
        this.operationTotalDao = db.operationTotalDao();
        shortMonths = context.getResources().getStringArray(R.array.short_months);
        longTypes = context.getResources().getStringArray(R.array.category_long_types);
        planStr = context.getString(R.string.plan);
        factStr = context.getString(R.string.operations);
    }


    @NonNull
    @Override
    public Result doWork() {
        notificationManager.cancel(RESULT_NOTIF_ID);
        Future<?> future =  setForegroundAsync(createForegroundInfo(getApplicationContext().getString(R.string.query_running)));

        Data inputData = getInputData();
        int dateCodeFrom = inputData.getInt(DC_FROM, -1);
        int dateCodeTo = inputData.getInt(DC_TO, -1);
        int groupByCode = inputData.getInt(GROUP_BY, -1);
        boolean plan = inputData.getBoolean(PLAN_TABLE, false);
        boolean fact = inputData.getBoolean(FACT_TABLE, false);
        long[] categoriesIds = inputData.getLongArray(CAT_IDS);
        int[] categoriesTypes = inputData.getIntArray(CAT_TYPES);
        String[] categoriesNames = inputData.getStringArray(CAT_NAMES);
        long[] aggregateIds = inputData.getLongArray(AGGREGATE_IDS);
        String[] aggregateNames = inputData.getStringArray(AGGREGATE_NAMES);
        comma = inputData.getBoolean(COMMA, true);

        String header = "";
        if(comma){
           delimiter = ",";
            header = getApplicationContext().getString(R.string.csv_header_comma);
        }else {
            delimiter = ";";
            header = getApplicationContext().getString(R.string.csv_header_semicolon);
        }


        StringBuilder data = new StringBuilder();
        data.append(header);

        String factCsv = "";
        String planCsv = "";
        if(dateCodeFrom != -1 && dateCodeTo != -1){
            List<LoadPoint> loadMap = null;
            if(groupByCode == NO_GROUP){
                loadMap = new ArrayList<>();
                loadMap.add(new LoadPoint(dateCodeFrom, dateCodeTo, true));
                if(fact){factCsv = getRowCsvOperationsData(DAY_GROUP, loadMap.get(0), categoriesIds, categoriesTypes, categoriesNames, aggregateIds, aggregateNames);}
                if(plan){planCsv = getRowCsvPlanData(DAY_GROUP, loadMap.get(0), categoriesIds, categoriesTypes, categoriesNames, aggregateIds, aggregateNames);}
            }else if(groupByCode == DAY_GROUP){
                loadMap = new ArrayList<>();
                loadMap.add(new LoadPoint(dateCodeFrom, dateCodeTo, true));
                if(fact){factCsv = getGroupedCsvOperationsData(DAY_GROUP, loadMap, categoriesIds, categoriesTypes, categoriesNames, aggregateIds, aggregateNames);}
                if(plan){planCsv = getRowCsvPlanData(DAY_GROUP, loadMap.get(0), categoriesIds, categoriesTypes, categoriesNames, aggregateIds, aggregateNames);}
            }else if(groupByCode == MONTH_GROUP){
                loadMap = finalMap(loadMapBuilder(dateCodeFrom, dateCodeTo, false));
                if(fact){factCsv = getGroupedCsvOperationsData(MONTH_GROUP, loadMap, categoriesIds, categoriesTypes, categoriesNames, aggregateIds, aggregateNames);}
                if(plan){planCsv = getGroupedCsvPlanData(MONTH_GROUP, loadMap, categoriesIds, categoriesTypes, categoriesNames, aggregateIds, aggregateNames);}
            }else if(groupByCode == YEAR_GROUP){
                loadMap = finalMap(loadMapBuilder(dateCodeFrom, dateCodeTo, true));
                if(fact){ factCsv = getGroupedCsvOperationsData(YEAR_GROUP, loadMap, categoriesIds, categoriesTypes, categoriesNames, aggregateIds, aggregateNames);}
                if(plan){ planCsv = getGroupedCsvPlanData(YEAR_GROUP, loadMap, categoriesIds, categoriesTypes, categoriesNames, aggregateIds, aggregateNames);}
            }
        }

        data.append(factCsv);
        data.append(planCsv);

        if(isStopped()){
            return Result.failure();
        }

        try {

            File cashData = new File(getApplicationContext().getCacheDir(), "export_data.csv");
            PrintWriter writer = new PrintWriter(cashData);
            writer.write(data.toString());
            writer.close();

            Context context = getApplicationContext();
            Uri path = FileProvider.getUriForFile(context, "tech.aurorafin.aurora.fileprovider", cashData);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Aurora export data");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    0, Intent.createChooser(fileIntent, "Data"), 0);
            Notification notification = new NotificationCompat.Builder(getApplicationContext(), DbService.CHANNEL_ID)
                    .setContentTitle("export_data.csv")
                    .setContentText(context.getString(R.string.tap_to_save))
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentIntent(pendingIntent)
                    .build();
            Thread.sleep(1000);
            notificationManager.notify(RESULT_NOTIF_ID, notification);

            return Result.success();
        }catch (Exception e){
            return Result.failure();
        }

    }

    private String getRowCsvPlanData(int totalGroup, LoadPoint loadPoint,
                                           long[] categoriesIds, int[] categoriesTypes, String[] categoriesNames,
                                           long[] aggregateIds, String[] aggregateNames) {
        if (categoriesIds == null) {
            return "";
        }
        StringBuilder csvData = new StringBuilder();
        for (int i = 0; i < categoriesIds.length; i++){
            if(isStopped()){
                return "";
            }
            Plan[] planDays = planDao.getPlanOfCategoryByDateCodeRange(categoriesIds[i], loadPoint.dateCodeFrom, loadPoint.dateCodeTo);
            if(planDays!=null){
                for (int x = 0; x < planDays.length; x++){
                    if(isStopped()){
                        return "";
                    }
                    String csvRow = getCsvRow(planStr, categoriesTypes[i], categoriesIds[i], categoriesNames[i], aggregateIds[i], aggregateNames[i],
                            planDays[x].dateCode, planDays[x].dateCode, Integer.toString(planDays[x].day), planDays[x].value,
                            totalGroup, "");
                    csvData.append(csvRow);
                }
            }
        }
        return csvData.toString();
    }


    private String getGroupedCsvPlanData(int totalGroup, List<LoadPoint> loadMap,
                                  long[] categoriesIds, int[] categoriesTypes, String[] categoriesNames,
                                  long[] aggregateIds, String[] aggregateNames){
        if(categoriesIds == null){
            return "";
        }
        StringBuilder csvData = new StringBuilder();
        for (int i = 0; i < categoriesIds.length; i++) {
            if(isStopped()){
                return "";
            }
            for (int p = 0; p < loadMap.size(); p++) {
                if(isStopped()){
                    return "";
                }
                if (loadMap.get(p).sequential) {
                    PlanTotal[] planTotal = getRightPlanTotals(categoriesIds[i], loadMap.get(p).dateCodeFrom, loadMap.get(p).dateCodeTo, totalGroup);
                    if(planTotal!=null){
                        for(int x = 0; x < planTotal.length; x++){
                            String csvRow = getCsvRow(planStr, categoriesTypes[i], categoriesIds[i], categoriesNames[i], aggregateIds[i], aggregateNames[i],
                                    planTotal[x].dateCodeFrom, planTotal[x].dateCodeTo, Integer.toString(planTotal[x].period), planTotal[x].value,
                                    totalGroup, "");
                            csvData.append(csvRow);
                        }
                    }
                }else {
                    long sum = planDao.getSumPlanOfCategoryByDateCodeRange(categoriesIds[i], loadMap.get(p).dateCodeFrom, loadMap.get(p).dateCodeTo);
                    if(sum!=0){
                        String period = loadMap.get(p).dateCodeFrom +"-"+ loadMap.get(p).dateCodeTo;
                        String csvRow = getCsvRow(planStr,categoriesTypes[i], categoriesIds[i], categoriesNames[i], aggregateIds[i], aggregateNames[i],
                                loadMap.get(p).dateCodeFrom, loadMap.get(p).dateCodeTo, period, sum,
                                totalGroup, "");
                        csvData.append(csvRow);
                    }

                }
            }
        }
        return csvData.toString();
    }

    private PlanTotal[] getRightPlanTotals(long catId, int dateCodeFrom, int dateCodeTo, int totalGroup) {
        switch (totalGroup){
            case MONTH_GROUP:
                return planTotalDao
                        .getPlanTotalMonthsOfCategoryByDateCodeRange(catId, dateCodeFrom, dateCodeTo);
            case YEAR_GROUP:
                return planTotalDao
                        .getPlanTotalYearsOfCategoryByDateCodeRange(catId, dateCodeFrom, dateCodeTo);
            default:
                return null;
        }
    }

    private String getRowCsvOperationsData(int totalGroup, LoadPoint loadPoint,
                                               long[] categoriesIds, int[] categoriesTypes, String[] categoriesNames,
                                               long[] aggregateIds, String[] aggregateNames) {
        if (categoriesIds == null) {
            return "";
        }
        StringBuilder csvData = new StringBuilder();
        for (int i = 0; i < categoriesIds.length; i++){
            if(isStopped()){
                return "";
            }
            Operation[] operations = operationDao.getOperationsOfCategoryByRange(categoriesIds[i], loadPoint.dateCodeFrom, loadPoint.dateCodeTo);
            if(operations != null){
                for(int o = 0; o < operations.length; o++){
                    String csvRow = getCsvRow(factStr, categoriesTypes[i], categoriesIds[i], categoriesNames[i], aggregateIds[i], aggregateNames[i],
                            operations[o].dateCode, operations[o].dateCode, Integer.toString(operations[o].day), operations[o].value,
                            totalGroup, operations[o].description);
                    csvData.append(csvRow);
                }
            }
        }
        return csvData.toString();
    }


    private String getGroupedCsvOperationsData(int totalGroup, List<LoadPoint> loadMap,
                                            long[] categoriesIds, int[] categoriesTypes, String[] categoriesNames,
                                            long[] aggregateIds, String[] aggregateNames) {
        if(categoriesIds == null){
            return "";
        }
        StringBuilder csvData = new StringBuilder();
        for (int i = 0; i < categoriesIds.length; i++){
            if(isStopped()){
                return "";
            }
            for (int p = 0; p < loadMap.size(); p++){
                if(isStopped()){
                    return "";
                }
                if(loadMap.get(p).sequential){
                    OperationTotal[] operationTotals = getRightOperationTotals(categoriesIds[i], loadMap.get(p).dateCodeFrom, loadMap.get(p).dateCodeTo, totalGroup);
                    if(operationTotals!=null){
                        for (int o = 0; o < operationTotals.length; o++){
                            if(isStopped()){
                                return "";
                            }
                            String csvRow = getCsvRow(factStr, categoriesTypes[i], categoriesIds[i], categoriesNames[i], aggregateIds[i], aggregateNames[i],
                                    operationTotals[o].dateCodeFrom, operationTotals[o].dateCodeTo, Integer.toString(operationTotals[o].period), operationTotals[o].value,
                                    totalGroup, "");
                            csvData.append(csvRow);
                        }
                    }
                }else {
                    long sum = operationTotalDao.getSumOperationTotalDaysOfCategoryByDateCodeRange(categoriesIds[i], loadMap.get(p).dateCodeFrom, loadMap.get(p).dateCodeTo);
                    if(sum!=0){
                        String period = loadMap.get(p).dateCodeFrom +"-"+ loadMap.get(p).dateCodeTo;
                        String csvRow = getCsvRow(factStr,categoriesTypes[i], categoriesIds[i], categoriesNames[i], aggregateIds[i], aggregateNames[i],
                                loadMap.get(p).dateCodeFrom, loadMap.get(p).dateCodeTo, period, sum,
                                totalGroup, "");
                        csvData.append(csvRow);
                    }

                }
            }
        }
        return csvData.toString();
    }

    private String getCsvRow(String table, int categoriesType, long categoriesId, String categoriesName, long aggregateId, String aggregateName,
                             int dateCodeFrom, int dateCodeTo, String period, long value,
                             int totalGroup, String description) {

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append(table); builder.append(delimiter);
        builder.append(longTypes[categoriesType]);builder.append(delimiter);
        builder.append(categoriesId);builder.append(delimiter);
        builder.append("\""); builder.append(categoriesName);builder.append("\"");builder.append(delimiter);
        builder.append(aggregateId);builder.append(delimiter);
        builder.append("\""); builder.append(aggregateName);builder.append("\"");builder.append(delimiter);
        builder.append(getDates(dateCodeFrom));builder.append(delimiter);
        builder.append(getDates(dateCodeTo));builder.append(delimiter);
        builder.append(period);builder.append(delimiter);
        builder.append(getPeriod(totalGroup, dateCodeTo));builder.append(delimiter);
        builder.append(PlanData.longToStringWithDot(value));builder.append(delimiter);
        builder.append("\""); builder.append(description);builder.append("\"");
        return builder.toString();

        /*return "\n" + table +delimiter + longTypes[categoriesType]+delimiter + categoriesId+delimiter +"\""+categoriesName+"\""+delimiter + aggregateId+delimiter + "\""+aggregateName+"\""+delimiter
                +getDates(dateCodeFrom)+delimiter + getDates(dateCodeTo)+delimiter + period+delimiter + getPeriod(totalGroup, dateCodeTo)+delimiter + longToStringWithDot(value)+delimiter + "\""+"Описание"+"\"";
    */
    }

    private String getPeriod(int totalGroup, int dateCode){
        if(totalGroup == NO_GROUP || totalGroup == DAY_GROUP){
            return Integer.toString(dateCode);
        }else if(totalGroup == MONTH_GROUP){
            int year = dateCode / 10000;
            int month = (dateCode % 10000) / 100 - 1;
            return shortMonths[month] + " " +Integer.toString(year);
        }else {
            int year = dateCode / 10000;
            return Integer.toString(year);
        }
    }

    private String getDates(int dateCode ){
        int year = dateCode / 10000;
        int month = (dateCode % 10000) / 100;
        int day = dateCode % 100;
        return year+delimiter + month+delimiter + day;
    }



    private OperationTotal[] getRightOperationTotals(long catId, int dateCodeFrom, int dateCodeTo, int totalGroup){
        switch (totalGroup){
            case DAY_GROUP:
                return operationTotalDao
                        .getOperationDayTotalsOfCategoryByDateCodeRange(catId, dateCodeFrom, dateCodeTo);
            case MONTH_GROUP:
                return operationTotalDao
                        .getOperationMonthTotalsOfCategoryByDateCodeRange(catId, dateCodeFrom, dateCodeTo);
            case YEAR_GROUP:
                return operationTotalDao
                        .getOperationYearTotalsOfCategoryByDateCodeRange(catId, dateCodeFrom, dateCodeTo);
            default:
                return null;
        }
    }

    private ForegroundInfo createForegroundInfo(String text){
        Context context = getApplicationContext();
        DbService.createNotificationChannel(context);
        Notification notification = serviceNotification(text);
        return new ForegroundInfo(FG_NOTIF_ID, notification);
    }

    private Notification serviceNotification(String text){
        Context context = getApplicationContext();
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());
        return new NotificationCompat.Builder(context, DbService.CHANNEL_ID)
                //.setContentTitle(context.getString(R.string.export_query))
                .setContentText(text)
                .setSmallIcon(R.drawable.notification_icon)
                .addAction(android.R.drawable.ic_delete, context.getString(R.string.cancel1), intent)
                .build();
    }

    private List<LoadPoint> loadMapBuilder(int dateCodeFrom, int dateCodeTo, boolean yearF){
        List<LoadPoint> temLoadPoints = new ArrayList<>();


        int yearFrom = dateCodeFrom / 10000;
        int monthFrom = (dateCodeFrom % 10000) / 100 - 1;
        int dayFrom = dateCodeFrom % 100;
        int yearTo = dateCodeTo / 10000;
        int monthTo = (dateCodeTo % 10000) / 100 - 1;
        int dayTo = dateCodeTo % 100;

        Calendar start = Calendar.getInstance();
        start.set(Calendar.YEAR, yearFrom);
        start.set(Calendar.MONTH, monthFrom);
        start.set(Calendar.DAY_OF_MONTH, dayFrom);

        Calendar end = Calendar.getInstance();
        end.set(Calendar.YEAR, yearTo);
        end.set(Calendar.MONTH, monthTo);
        end.set(Calendar.DAY_OF_MONTH, dayTo);

        int i = 0;

        int localDcFrom = dateCodeFrom;
        int prevDateCode= dateCodeFrom;
        int checkPeriod;
        int sequentMax;
        int sequentMin;


        Calendar cal;
        cal = (Calendar)start.clone();
        if(yearF){
            checkPeriod = yearFrom;
            sequentMin = PlanData.getDayCode(yearFrom, 0, 1);
            sequentMax = PlanData.getDayCode(yearFrom, 11, 31);
        }else {
            checkPeriod = monthFrom;
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
            sequentMin = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            sequentMax = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }



        for (Date date = start.getTime(); !start.after(end); start.add(Calendar.DATE, 1), date = start.getTime()){
            int day = start.get(Calendar.DAY_OF_MONTH);
            int month = start.get(Calendar.MONTH);
            int year = start.get(Calendar.YEAR);
            int currentDateCode = PlanData.getDayCode(year, month, day);
            int currentPeriod;
            if(yearF){
                currentPeriod = year;
            }else {
                currentPeriod = month;
            }
            if(checkPeriod !=currentPeriod){
                boolean seq = localDcFrom==sequentMin&&prevDateCode == sequentMax;
                LoadPoint temp = new LoadPoint(localDcFrom, prevDateCode, seq);
                temLoadPoints.add(temp);
                localDcFrom = currentDateCode;
                if(yearF){
                    checkPeriod = currentPeriod;
                    sequentMin = PlanData.getDayCode(year, 0, 1);
                    sequentMax = PlanData.getDayCode(year, 11, 31);
                }else {
                    checkPeriod = currentPeriod;
                    cal = (Calendar)start.clone();
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMinimum(Calendar.DAY_OF_MONTH));
                    sequentMin = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    sequentMax = PlanData.getDayCode(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                }
            }
            if(currentDateCode == dateCodeTo){
                boolean seq = localDcFrom==sequentMin&&currentDateCode == sequentMax;
                LoadPoint temp = new LoadPoint(localDcFrom, currentDateCode, seq);
                temLoadPoints.add(temp);
            }
            prevDateCode = currentDateCode;
        }
        return temLoadPoints;
    }

    private List<LoadPoint> finalMap(List<LoadPoint> tempMap){

        if(tempMap.size()==1){
            return tempMap;
        }else {
            List<LoadPoint> finalLoadPoints = new ArrayList<>();
            int singleSeqDcFrom = -1;
            int singleSeqDcTo = -1;
            for (int i = 0; i < tempMap.size(); i++) {
                if (tempMap.get(i).sequential) {
                    if (singleSeqDcFrom == -1) {
                        singleSeqDcFrom = tempMap.get(i).dateCodeFrom;
                    }
                    singleSeqDcTo = tempMap.get(i).dateCodeTo;
                }
            }

            if (tempMap.get(0).dateCodeFrom != singleSeqDcFrom) {
                finalLoadPoints.add(tempMap.get(0));
            }
            if (singleSeqDcFrom != -1) {
                finalLoadPoints.add(new LoadPoint(singleSeqDcFrom, singleSeqDcTo, true));
            }
            if (tempMap.get(tempMap.size() - 1).dateCodeTo != singleSeqDcTo) {
                finalLoadPoints.add(tempMap.get(tempMap.size() - 1));
            }
            return finalLoadPoints;
        }
    }

    private static class LoadPoint{
        int dateCodeFrom;
        int dateCodeTo;
        boolean sequential;

        public LoadPoint(int dateCodeFrom, int dateCodeTo, boolean sequential) {
            this.dateCodeFrom = dateCodeFrom;
            this.dateCodeTo = dateCodeTo;
            this.sequential = sequential;
        }
    }
}
