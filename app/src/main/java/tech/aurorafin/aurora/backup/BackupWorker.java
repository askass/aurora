package tech.aurorafin.aurora.backup;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import tech.aurorafin.aurora.DbService;
import tech.aurorafin.aurora.R;
import tech.aurorafin.aurora.dbRoom.CashFlowDB;
import tech.aurorafin.aurora.DateFormater;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.util.concurrent.ListenableFuture;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


import static android.content.Context.NOTIFICATION_SERVICE;
import static tech.aurorafin.aurora.backup.BackupActivity.ACCOUNT_TYPE;
import static tech.aurorafin.aurora.backup.BackupActivity.PERIODIC_WORK_TAG;
import static tech.aurorafin.aurora.backup.BackupActivity.PREF_ACCOUNT_NAME;
import static tech.aurorafin.aurora.backup.BackupActivity.PREF_FILE_KEY;
import static tech.aurorafin.aurora.backup.BackupActivity.SCOPES;
import static tech.aurorafin.aurora.backup.BackupActivity.getNextBackupDateFromToday;
import static tech.aurorafin.aurora.subscription.SubscriptionActivity.isPremiumActive;

public class BackupWorker extends Worker {

    public static final String DRIVE_LOG_FILE = "drive_log.txt";
    public static final String DRIVE_PREV_FOLDER = "prev";

    public static final String PERIODIC_BOOLEAN_KEY = "periodic_boolean";
    public static final String REPEAT_INTERVAL_KEY = "repeat_interval";
    public static final String NEXT_DATE_KEY = "next_date";

    private NotificationManager notificationManager;

    int formatCode;

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
    }


    @NonNull
    @Override
    public Result doWork() {
        Future<?> future = setForegroundAsync(createForegroundInfo());
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                PREF_FILE_KEY, Context.MODE_PRIVATE);
        String accountName = sharedPref.getString(PREF_ACCOUNT_NAME, null);
        int repeat_interval = sharedPref.getInt(REPEAT_INTERVAL_KEY, -1);
        formatCode = DateFormater.getDateFormatKey(getApplicationContext());
        boolean periodic = false;

        if(accountName!=null){
            Data inputData = getInputData();
            periodic = inputData.getBoolean(PERIODIC_BOOLEAN_KEY, false);
            if(periodic){
                //Check if this work is currently running
                if(isWorkScheduled(accountName)){
                    return Result.success();
                }
                //No reschedule settings, something wrong
                if(repeat_interval == -1){
                    WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag(PERIODIC_WORK_TAG);
                    showFailBackupNotification(getApplicationContext().getString(R.string.auto_upload_settings_error));
                    return Result.failure();
                }
            }

            //No subscription
            if(!isPremiumActive(getApplicationContext())){
                if(periodic){
                    WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag(PERIODIC_WORK_TAG);
                }
                showFailBackupNotification(getApplicationContext().getString(R.string.auto_upload_subscription_error));
                return Result.failure();
            }

            GoogleAccountCredential  mCredential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(), Arrays.asList(SCOPES))
                    .setBackOff(null);
            mCredential.setSelectedAccount(new Account(accountName, ACCOUNT_TYPE));
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            Drive mDriveService = new Drive.Builder(
                    transport, jsonFactory, mCredential)
                    .setApplicationName("Aurora")
                    .build();
            try {
                String logFileId = getLogFileId(mDriveService);
                boolean correct = isBackUpCorrect(mDriveService, logFileId);
                if(correct){
                    String prevId = getPrevFolderId(mDriveService);
                    clearPrevFolder(mDriveService, prevId);
                    copyToPrev(mDriveService, prevId);
                }
                updateLogFile(mDriveService, logFileId, false);
                clearAppDataFolder(mDriveService);

                java.io.File db = new java.io.File(getApplicationContext().getFilesDir() + "/prev", CashFlowDB.cash_flow_db);
                java.io.File dbShm = new java.io.File(db.getParent(), CashFlowDB.cash_flow_db+"-shm");
                java.io.File dbWal = new java.io.File(db.getParent(), CashFlowDB.cash_flow_db+"-wal");

                copyToDrive(mDriveService, dbShm, CashFlowDB.cash_flow_db+"-shm");
                copyToDrive(mDriveService, dbWal, CashFlowDB.cash_flow_db+"-wal");
                copyToDrive(mDriveService, db, CashFlowDB.cash_flow_db);
                updateLogFile(mDriveService, logFileId, true);

            }catch (Exception e){
                e.printStackTrace();
                if(periodic){
                    showFailBackupNotification(e.getLocalizedMessage());
                    resetNextBackupDate(repeat_interval, sharedPref);
                }
                future.cancel(true);
                return Result.failure();
            }
        }
        if(periodic){
            showSuccessBackupNotification(repeat_interval);
            resetNextBackupDate(repeat_interval, sharedPref);
        }
        future.cancel(true);
        return Result.success();
    }

    private void resetNextBackupDate(int repeat_interval, SharedPreferences sharedPref) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(NEXT_DATE_KEY, getNextBackupDateFromToday(repeat_interval));
        editor.apply();
    }


    private ForegroundInfo createForegroundInfo(){
        Context context = getApplicationContext();
        DbService.createNotificationChannel(context);
        Notification notification = new NotificationCompat.Builder(context, DbService.CHANNEL_ID)
                .setContentTitle(context.getString(R.string.db_backup))
                .setContentText(context.getString(R.string.backup_upload_run))
                .setSmallIcon(R.drawable.notification_icon)
                .setOngoing(true)
                .build();
        return new ForegroundInfo(102, notification);
    }

    private void showSuccessBackupNotification(int repeat_interval) {
        Context context = getApplicationContext();
        String msg = context.getString(R.string.auto_upload_success);
        String nextBackup = context.getString(R.string.next_backup) +" "+ DateFormater.getDateFromDateCode(getNextBackupDateFromToday(repeat_interval), formatCode);
        Notification notification = new NotificationCompat.Builder(context, DbService.CHANNEL_ID)
                .setContentTitle(msg)
                .setContentText(nextBackup)
                .setSmallIcon(R.drawable.notification_icon)
                .build();
        notificationManager.notify(101, notification);
    }

    private void showFailBackupNotification(String msg) {
        Context context = getApplicationContext();
       /* Intent notificationIntent = new Intent(context, BackupActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, 0);*/
        Notification notification = new NotificationCompat.Builder(context, DbService.CHANNEL_ID)
                .setContentTitle(context.getString(R.string.auto_upload_error))
                .setContentText(msg)
                .setSmallIcon(R.drawable.notification_icon)
                //.setContentIntent(pendingIntent)
                .build();
        notificationManager.notify(101, notification);
    }

    private boolean isWorkScheduled(String tag) {
        WorkManager instance = WorkManager.getInstance(getApplicationContext());
        ListenableFuture<List<WorkInfo>> statuses = instance.getWorkInfosForUniqueWork(tag);
        try {
            boolean running = false;
            List<WorkInfo> workInfoList = statuses.get();
            for (WorkInfo workInfo : workInfoList) {
                WorkInfo.State state = workInfo.getState();
                running = state == WorkInfo.State.RUNNING || state == WorkInfo.State.ENQUEUED;
            }
            return running;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void clearPrevFolder(Drive mDriveService, String prevId) throws IOException{
        FileList files = mDriveService.files().list()
                .setQ(String.format("'%s' in parents", prevId))
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute();
        for (File file : files.getFiles()) {
            mDriveService.files().delete(file.getId()).execute();
        }
    }

    public static String getPrevFolderId(Drive mDriveService)throws IOException  {
        FileList result = mDriveService.files().list()
                .setQ(String.format("mimeType='application/vnd.google-apps.folder' and name = '%s'", DRIVE_PREV_FOLDER))
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute();
        for (File file : result.getFiles()) {
           return  file.getId();
        }

        return createPrevFolder(mDriveService);
    }

    private static String createPrevFolder(Drive mDriveService) throws IOException{
        File fileMetadata = new File();
        fileMetadata.setName(DRIVE_PREV_FOLDER);
        fileMetadata.setParents(Collections.singletonList("appDataFolder"));
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        File file = mDriveService.files().create(fileMetadata)
                .setFields("id")
                .execute();

        return file.getId();
    }


    private void copyToPrev(Drive mDriveService, String prevId)throws IOException {
        FileList files = mDriveService.files().list()
                .setQ("mimeType='application/x-sqlite3'")
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute();
        for (File file : files.getFiles()) {
            File copiedFile = new File();
            copiedFile.setName(file.getName());
            copiedFile.setParents(Collections.singletonList(prevId));
            mDriveService.files().copy(file.getId(), copiedFile).execute();
        }

    }

    private String getLogFileId(Drive mDriveService)throws IOException {
        FileList result = mDriveService.files().list()
                .setQ(String.format("mimeType='text/plain' and name = '%s'", DRIVE_LOG_FILE))
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute();
        for (File file : result.getFiles()) {
            return  file.getId();
        }
        return createLogFile(mDriveService);
    }

    private String createLogFile(Drive mDriveService)throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(DRIVE_LOG_FILE);
        fileMetadata.setParents(Collections.singletonList("appDataFolder"));
        FileContent mediaContent = new FileContent("text/plain", generateDriveLogFile(false));
        File file = mDriveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
        return file.getId();
    }


    private void updateLogFile(Drive mDriveService, String logFileId, boolean correct)throws IOException {
        File file = new File();
        FileContent mediaContent = new FileContent("text/plain", generateDriveLogFile(correct));
        mDriveService.files().update(logFileId, file, mediaContent).execute();
    }

    private synchronized java.io.File generateDriveLogFile(boolean correct)throws IOException{
        java.io.File file = new java.io.File(getApplicationContext().getFilesDir(), DRIVE_LOG_FILE);
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            DataOutputStream dos = new DataOutputStream(fos);
            dos.writeBoolean(correct);
            dos.close();
      return file;
    }

    public static void clearAppDataFolder(Drive driveService) throws IOException {
        FileList files = driveService.files().list()
                .setQ("mimeType='application/x-sqlite3' and 'appDataFolder' in parents")
                .setSpaces("appDataFolder")
                .execute();
        for (File file : files.getFiles()) {
            driveService.files().delete(file.getId()).execute();
        }
    }



    public static boolean isBackUpCorrect(Drive mDriveService, String logFileId)throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        mDriveService.files().get(logFileId)
                .executeMediaAndDownloadTo(outputStream);
        InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
        return inStream.read() != 0;
    }



    private void copyToDrive(Drive driveService,  java.io.File source, String name) throws IOException {
        if(source.exists()){
            File fileMetadata = new File();
            fileMetadata.setName(name);
            fileMetadata.setParents(Collections.singletonList("appDataFolder"));
            FileContent mediaContent = new FileContent("application/x-sqlite3", source);
            driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
        }
    }

}
