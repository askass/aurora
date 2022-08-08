package tech.aurorafin.aurora.backup;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import tech.aurorafin.aurora.CategoriesFragment;
import tech.aurorafin.aurora.DateFormater;
import tech.aurorafin.aurora.MainActivity;
import tech.aurorafin.aurora.PlanData;
import tech.aurorafin.aurora.R;
import tech.aurorafin.aurora.dbRoom.CashFlowDB;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class BackupActivity extends AppCompatActivity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener{

    AppCompatImageButton backup_back_btn;

    /*Account*/
    private static final int ACCOUNT_REQUEST_CODE = 1;
    private static final int DRIVE_REQUEST_CODE = 2;
    private static final int REQUEST_PERMISSION_GET_ACCOUNTS = 3;
    TextView account_select, account_label;
    CategoriesFragment.NewValidatorAnimator categoryValidatorAnimator;
    String accountName = "";
    public static final String[] SCOPES = { DriveScopes.DRIVE_APPDATA };
    public static final String PREF_FILE_KEY = "backupPrefs";
    public static final String PREF_ACCOUNT_NAME = "accountName";
    public static final String PERIODIC_WORK_TAG = "periodicBackup";
    public static final String ACCOUNT_TYPE = "com.google";
    /*Connect*/
    private static final int BTN_ENABLED = 100;
    private static final int BTN_CONNECTING = 102;
    private static final int BTN_CONNECTED = 103;
    ProgressBar connect_progress_bar;
    TextView connect_btn;
    TextView error_message;
    private ObjectAnimator errorAnimator;
    private AnimatorListenerAdapter animatorListenerAdapter;
    int blueRowColor;
    String connectStr;
    String connectedStr;
    boolean connected;

    /*Drive*/
    Drive mService;
    DriveServiceHelper driveServiceHelper;
    GoogleAccountCredential mCredential = null;

    /*Upload*/
    TextView upload_btn;
    WorkInfo.State currentState;
    WorkInfo.State uniqState;
    ImageView backup_image;
    ProgressBar upload_progress_bar;
    TextView backup_info;
    FrameLayout backup_control_lock;
    String dbCopyStr;
    String noDbStr;
    int greyTxtColor;
    int tableTxtColor;
    Observer<List<WorkInfo>> mObserver;
    LiveData<List<WorkInfo>> mWorkInfos;
    TextView upload_error_message;

    Switch auto_backup_switch;
    Observer<List<WorkInfo>> periodicObserver;
    LiveData<List<WorkInfo>> periodicWorkInfos;
    RadioButton days_28_backup, days_7_backup,days_1_backup;
    TextView next_backup;

    /*Download*/
    TextView download_btn;
    AlertDialog downloadDialog;
    TextView startDownLoadBtn;
    TextView cancelTerminateBtn;
    LinearLayout before_download_msg, progress_download_msg;
    TextView after_download_msg;
    boolean closeOnDl = false;
    boolean triedDownLoad = false;
    boolean downloadError = false;
    int formatCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        backup_back_btn = findViewById(R.id.backup_back_btn);
        backup_back_btn.setOnClickListener(this);
        formatCode = DateFormater.getDateFormatKey(this);
        /*Account*/
        account_select = findViewById(R.id.account_select);
        account_label = findViewById(R.id.account_label);
        account_select.setOnClickListener(this);
        int txtRedColor = ContextCompat.getColor(this, R.color.red_txt_color);
        int txtHintColor = ContextCompat.getColor(this, R.color.dark_grey);
        categoryValidatorAnimator = new CategoriesFragment.NewValidatorAnimator(account_select, "HintTextColor", true, txtRedColor, txtHintColor);
        /*Connect*/
        connected = false;
        connect_btn = findViewById(R.id.connect_btn);
        connect_btn.setOnClickListener(this);
        connect_progress_bar = findViewById(R.id.connect_progress_bar);
        blueRowColor = ContextCompat.getColor(this, R.color.blue_row);
        connectStr = getString(R.string.connect);
        connectedStr = getString(R.string.connected);
        error_message = findViewById(R.id.error_message);
        errorAnimator = ObjectAnimator.ofFloat(error_message, "Alpha", 1, 0);
        errorAnimator.setStartDelay(1500);
        errorAnimator.setDuration(200);
        animatorListenerAdapter = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                connectionBtnStateUpdate(BTN_ENABLED);
            }
        };

        /*Drive*/
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(null);

        /*Upload*/
        upload_btn = findViewById(R.id.upload_btn);
        upload_btn.setOnClickListener(this);

        backup_image = findViewById(R.id.backup_image);
        upload_progress_bar = findViewById(R.id.upload_progress_bar);
        backup_info = findViewById(R.id.backup_info);
        backup_control_lock = findViewById(R.id.backup_control_lock);
        dbCopyStr = getString(R.string.database_copy);
        noDbStr = getString(R.string.no_backup_available);
        greyTxtColor= ContextCompat.getColor(this, R.color.grey_txt_color);
        tableTxtColor= ContextCompat.getColor(this, R.color.table_txt_color);
        upload_error_message  = findViewById(R.id.upload_error_message);
        currentState = WorkInfo.State.SUCCEEDED;
        uniqState = currentState;
        mObserver = new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                if(workInfos.size()>0){
                    WorkInfo.State state = workInfos.get(workInfos.size()-1).getState();
                    generalUploadWorkProgressTrack(state, false);
                }else {
                    upload_btn.setEnabled(true);
                    currentState = WorkInfo.State.SUCCEEDED;
                }
            }
        };

        periodicObserver = new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                if(workInfos.size()>0){
                    WorkInfo.State state = workInfos.get(workInfos.size()-1).getState();
                    generalUploadWorkProgressTrack(state, true);
                }else {
                    if(uniqState != WorkInfo.State.RUNNING){
                        upload_btn.setEnabled(true);
                        currentState = WorkInfo.State.SUCCEEDED;
                    }
                }
                checkAutoBackupForMismatches(workInfos);
            }
        };
        days_28_backup  = findViewById(R.id.days_28_backup);
        days_7_backup  = findViewById(R.id.days_7_backup);
        days_1_backup = findViewById(R.id.days_1_backup);
        days_28_backup.setOnClickListener(this);
        days_7_backup.setOnClickListener(this);
        days_1_backup.setOnClickListener(this);
        next_backup = findViewById(R.id.next_backup);
        auto_backup_switch = findViewById(R.id.auto_backup_switch);
        restoreAutoBackupSettings();
        auto_backup_switch.setOnCheckedChangeListener(this);

        /*Download*/
        download_btn = findViewById(R.id.download_btn);
        download_btn.setOnClickListener(this);
        LinearLayout backup_restore_layout = (LinearLayout) getLayoutInflater().inflate(R.layout.backup_restore, null);
        startDownLoadBtn = backup_restore_layout.findViewById(R.id.download_backup);
        startDownLoadBtn.setOnClickListener(this);
        cancelTerminateBtn = backup_restore_layout.findViewById(R.id.cancel_backup);
        cancelTerminateBtn.setOnClickListener(this);
        before_download_msg= backup_restore_layout.findViewById(R.id.before_download_msg);
        progress_download_msg = backup_restore_layout.findViewById(R.id.progress_download_msg);
        after_download_msg = backup_restore_layout.findViewById(R.id.after_download_msg);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(backup_restore_layout);
        builder.setCancelable(false);
        downloadDialog = builder.create();
        triedDownLoad = false;
        downloadError = false;


        autoUiUpdate();
    }

    private void restoreAutoBackupSettings() {
        SharedPreferences sharedPref = this.getSharedPreferences(
                PREF_FILE_KEY, Context.MODE_PRIVATE);
        int repeat_interval = sharedPref.getInt(BackupWorker.REPEAT_INTERVAL_KEY, -1);
        if(repeat_interval == -1){
            auto_backup_switch.setChecked(false);
            updateAutoBackupRadioGroupState(false);
            next_backup.setText("");
        }else {
            auto_backup_switch.setChecked(true);
            updateAutoBackupRadioGroupState(true);
            if(repeat_interval == 1){
                days_1_backup.setChecked(true);
            }else if(repeat_interval == 7){
                days_7_backup.setChecked(true);
            }else {
                days_28_backup.setChecked(true);
            }
            String nextBackup = DateFormater.getDateFromDateCode(sharedPref.getInt(BackupWorker.NEXT_DATE_KEY, 0),formatCode);
            next_backup.setText(String.format("%s %s", getString(R.string.next_backup), nextBackup));
        }
    }


    private void autoUiUpdate() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                PREF_FILE_KEY, Context.MODE_PRIVATE);
        String tempName = sharedPref.getString(PREF_ACCOUNT_NAME, null);
        if(tempName!=null){
            accountName = tempName;
            account_select.setText(accountName);
            mCredential.setSelectedAccount(new Account(accountName, ACCOUNT_TYPE));
            connectDrive();
            connection();
            mWorkInfos = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(accountName);
            mWorkInfos.observe(this, mObserver);
            resetPeriodicObserver();
        }
    }

    @Override
    public void onBackPressed() {
        if(triedDownLoad) {
            System.exit(0);
        }else if(isTaskRoot()){
            Intent in = new Intent(this, MainActivity.class);
            in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(in);
        }else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.backup_back_btn:
                onBackPressed();
                break;
            case R.id.account_select:
                accountSelectDialog();
                break;
            case R.id.connect_btn:
                connection();
                break;
            case R.id.upload_btn:
                uploadBackup();
                break;
            case R.id.days_28_backup:
            case R.id.days_7_backup:
            case R.id.days_1_backup:
                setAutoBackup();
                break;
            case R.id.download_btn:
                closeOnDl = false;
                startDownLoadBtn.setText(R.string.download);
                cancelTerminateBtn.setVisibility(View.VISIBLE);
                before_download_msg.setVisibility(View.VISIBLE);
                progress_download_msg.setVisibility(View.GONE);
                after_download_msg.setVisibility(View.GONE);
                downloadDialog.show();
                break;
            case R.id.download_backup:
                startDownLoadBackup();
                break;
            case R.id.cancel_backup:
                downloadDialog.dismiss();
                break;
        }
    }

    private void startDownLoadBackup() {
        if(!closeOnDl){
            startDownLoadBtn.setEnabled(false);
            downloadError = false;
            if(CashFlowDB.isLastDbCloseCorrect(this)){
                if(driveServiceHelper!=null){
                    tryDownloadDb();
                }else {
                    downloadError = true;
                    makeClosableWithMessage(getString(R.string.download_error));
                }
            }else {
                makeClosableWithMessage(getString(R.string.service_is_running));
            }
        }else {
            if(downloadError){
                readDriveStorage();
            }
            downloadDialog.dismiss();
        }
    }

    private void tryDownloadDb() {
        cancelTerminateBtn.setVisibility(View.INVISIBLE);
        before_download_msg.setVisibility(View.GONE);
        progress_download_msg.setVisibility(View.VISIBLE);
        after_download_msg.setVisibility(View.GONE);
        triedDownLoad = true;
        driveServiceHelper.downloadDrive()
                .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        if(aBoolean == null){
                            downloadError = true;
                            makeClosableWithMessage(getString(R.string.download_error));
                        }else if(aBoolean){
                            downloadError = false;
                            makeClosableWithMessage(getString(R.string.download_success));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        downloadError = true;
                        makeClosableWithMessage(getString(R.string.download_error));

                    }
                });
    }

    private void makeClosableWithMessage(String msg){
        after_download_msg.setText(msg);
        startDownLoadBtn.setEnabled(true);
        startDownLoadBtn.setText(R.string.close);
        closeOnDl = true;
        cancelTerminateBtn.setVisibility(View.INVISIBLE);
        before_download_msg.setVisibility(View.GONE);
        progress_download_msg.setVisibility(View.GONE);
        after_download_msg.setVisibility(View.VISIBLE);
    }

    private void uploadBackup() {
        OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .build();
        WorkManager.getInstance(this).enqueueUniqueWork(accountName, ExistingWorkPolicy.KEEP, uploadRequest);
        if(mWorkInfos != null){
            mWorkInfos.removeObserver(mObserver);
        }
        mWorkInfos = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(accountName);
        mWorkInfos.observe(this, mObserver);
    }

    private void connection() {
        if(mCredential.getSelectedAccountName() == null){
            categoryValidatorAnimator.playValidatorAnim();
        }else {
            connectionBtnStateUpdate(BTN_CONNECTING);
            readDriveStorage();
            if(mWorkInfos != null){
                mWorkInfos.removeObserver(mObserver);
            }
            mWorkInfos = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(accountName);
            mWorkInfos.observe(this, mObserver);
        }
    }

    private void accountSelectDialog() {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{ACCOUNT_TYPE}, null, null, null,
                        null);
                startActivityForResult(intent, ACCOUNT_REQUEST_CODE);
            }else {
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        ACCOUNT_REQUEST_CODE);
            }
    }


    private void connectDrive() {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new Drive.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Aurora")
                .build();
        driveServiceHelper = new DriveServiceHelper(mService, this);
        connectionBtnStateUpdate(BTN_ENABLED);
    }

    private void readDriveStorage() {
        if (driveServiceHelper!=null){
                driveServiceHelper.queryFiles()
                        .addOnSuccessListener(new OnSuccessListener<FileList>() {
                            @Override
                            public void onSuccess(FileList fileList) {
                                saveLastConnectedAccount();
                                connectionBtnStateUpdate(BTN_CONNECTED);
                                updateBackupUI(fileList);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                dropBackupUi();
                                handleException(exception);
                            }
                        });
        }
    }

    private void updateBackupUI(FileList fileList) {
        if(currentState != WorkInfo.State.RUNNING){
            boolean updated = false;
            if(fileList != null){
                for (File file : fileList.getFiles()) {
                    String tmpName = file.getName();
                    if(tmpName.equals(CashFlowDB.cash_flow_db)){
                        long msTime = file.getCreatedTime().getValue();
                        String time = getTimeString(msTime);
                        backup_info.setText(String.format("%s\n%s", dbCopyStr, time));
                        backup_info.setTextColor(tableTxtColor);
                        backup_image.setVisibility(View.VISIBLE);
                        upload_btn.setText(R.string.update);
                        download_btn.setEnabled(true);
                        updated = true;
                    }
                }
            }
            if(!updated){
                backup_info.setText(noDbStr);
                backup_info.setTextColor(greyTxtColor);
                backup_image.setVisibility(View.GONE);
                upload_btn.setText(R.string.upload);
                download_btn.setEnabled(false);
            }
            upload_progress_bar.setVisibility(View.GONE);
            upload_btn.setEnabled(true);
        }
        backup_control_lock.setVisibility(View.GONE);
    }

    private String getTimeString(long msTime){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(msTime);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        return DateFormater.getDateFromDateCode(PlanData.getDayCode(year, month, day), formatCode) + " " + DateFormat.getTimeInstance().format(cal.getTime());

    }

    private void handleException(Exception exception) {
         if (exception instanceof UserRecoverableAuthIOException) {
             startActivityForResult(((UserRecoverableAuthIOException)exception).getIntent(), DRIVE_REQUEST_CODE);
         }else {
             if(isOnline()){
                 showConnectionError(exception.getLocalizedMessage());
             }else {
                 showConnectionError(getString(R.string.network_error));
             }

         }
    }

    private boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr != null ? connMgr.getActiveNetworkInfo() : null;
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void showConnectionError(String error){
        if (error != null){
            error_message.setText(error);
            error_message.setAlpha(1);
            if(errorAnimator !=null){
                errorAnimator.removeListener(animatorListenerAdapter);
                errorAnimator.cancel();
            }
            errorAnimator.addListener(animatorListenerAdapter);
            errorAnimator.start();
        }
    }

    private void connectionBtnStateUpdate(int state){
        switch (state) {
            case BTN_ENABLED:
                connect_btn.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_btn_ripple));
                connect_btn.setTextColor(getResources().getColorStateList(R.color.pressed_txt_color));
                connect_btn.setText(connectStr);
                connect_progress_bar.setVisibility(View.GONE);
                connect_btn.setEnabled(true);
                break;
            case BTN_CONNECTING:
                connect_btn.setBackground(ContextCompat.getDrawable(this, R.drawable.connected_btn));
                connect_btn.setTextColor(blueRowColor);
                connect_btn.setText(connectStr);
                connect_progress_bar.setVisibility(View.VISIBLE);
                connect_btn.setEnabled(false);
                break;
            case BTN_CONNECTED:
                connect_btn.setBackground(ContextCompat.getDrawable(this, R.drawable.connected_btn));
                connect_btn.setTextColor(blueRowColor);
                connect_btn.setText(connectedStr);
                connect_progress_bar.setVisibility(View.GONE);
                connect_btn.setEnabled(false);
                break;
        }
    }


    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACCOUNT_REQUEST_CODE && resultCode == RESULT_OK) {
            String tempName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if(tempName!=null && !accountName.equals(tempName)){
                if(mWorkInfos != null){
                    mWorkInfos.removeObserver(mObserver);
                }
                currentState = WorkInfo.State.SUCCEEDED;
                uniqState = currentState;
                accountName = tempName;
                account_select.setText(accountName);
                mCredential.setSelectedAccount(new Account(accountName, ACCOUNT_TYPE));
                removeLastConnectedAccount();
                dropBackupUi();
                connectDrive();
                auto_backup_switch.setChecked(false);
            }
        }
        if (requestCode == DRIVE_REQUEST_CODE) {
            if(resultCode == RESULT_OK){
                readDriveStorage();
            }else {
                connectionBtnStateUpdate(BTN_ENABLED);
            }
        }
    }

    private void dropBackupUi() {
        upload_progress_bar.setVisibility(View.GONE);
        backup_control_lock.setVisibility(View.VISIBLE);
        backup_image.setVisibility(View.GONE);
        backup_info.setText("");
        //upload_error_message.setText("");
    }

    private void saveLastConnectedAccount(){
        SharedPreferences settings =
                getSharedPreferences(PREF_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREF_ACCOUNT_NAME, accountName);
        editor.apply();
        connected = true;
    }

    private void removeLastConnectedAccount(){
        SharedPreferences settings =
                getSharedPreferences(PREF_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(PREF_ACCOUNT_NAME);
        editor.apply();
        connected = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if(checked){
            updateAutoBackupRadioGroupState(true);
            setAutoBackup();
        }else {
            cancelAutoBackUp();
        }
    }

    private void cancelAutoBackUp() {
        updateAutoBackupSharedPrefs(-1, 0);
        updateAutoBackupRadioGroupState(false);
        next_backup.setText("");
        WorkManager.getInstance(this).cancelUniqueWork(PERIODIC_WORK_TAG);

    }

    private void updateAutoBackupRadioGroupState(boolean enabled) {
        days_28_backup.setEnabled(enabled);
        days_7_backup.setEnabled(enabled);
        days_1_backup.setEnabled(enabled);
    }

    private void setAutoBackup() {
        int repeat_interval = getRepeatInterval();
        int nextBackupDateCode = getNextBackupDateFromToday(repeat_interval);
        String nextBackup = DateFormater.getDateFromDateCode(nextBackupDateCode, formatCode);
        updateAutoBackupSharedPrefs(repeat_interval, nextBackupDateCode);
        next_backup.setText(String.format("%s %s", getString(R.string.next_backup), nextBackup));
        Data data = new Data.Builder()
                .putBoolean(BackupWorker.PERIODIC_BOOLEAN_KEY, true)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest uploadRequest = new PeriodicWorkRequest.Builder(BackupWorker.class, repeat_interval, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInputData(data)
                .setInitialDelay(repeat_interval, TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(PERIODIC_WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE, uploadRequest);
        resetPeriodicObserver();
    }

    private void updateAutoBackupSharedPrefs(int repeat_interval, int nextBackupDc) {
        SharedPreferences settings =
                getSharedPreferences(PREF_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BackupWorker.REPEAT_INTERVAL_KEY, repeat_interval);
        editor.putInt(BackupWorker.NEXT_DATE_KEY, nextBackupDc);
        editor.apply();
    }

    private void checkAutoBackupForMismatches(List<WorkInfo> workInfos) {
        SharedPreferences sharedPref = this.getSharedPreferences(
                PREF_FILE_KEY, Context.MODE_PRIVATE);
        int repeat_interval = sharedPref.getInt(BackupWorker.REPEAT_INTERVAL_KEY, -1);
        boolean isWorkerEnqueued;
        if(workInfos.size() == 0){
            isWorkerEnqueued = false;
        }else if(workInfos.get(workInfos.size()-1).getState() == WorkInfo.State.CANCELLED){
            isWorkerEnqueued = false;
        }else{
            isWorkerEnqueued = true;
        }
        //Log.d("MyTag", "checkAutoBackupForMismatches");
        if((isWorkerEnqueued && repeat_interval == -1)
            ||(repeat_interval != -1 && !isWorkerEnqueued)){
            auto_backup_switch.setChecked(false);
            //Log.d("MyTag", "MismatchesCheck FAILED!!!!");
        }


    }

    public static int getNextBackupDateFromToday(int repeat_interval) {
        if(repeat_interval != -1){
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, repeat_interval);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH);
            int year = cal.get(Calendar.YEAR);
            return PlanData.getDayCode(year, month, day);
        }else {
            return 0;
        }
    }

    private int getRepeatInterval() {
        if(days_7_backup.isChecked()){
            return 7;
        }else if (days_1_backup.isChecked()){
            return 1;
        }else {
            return 28;
        }
    }


    private synchronized void generalUploadWorkProgressTrack(WorkInfo.State state, boolean periodic) {
        if(state != currentState){
            if(state == WorkInfo.State.RUNNING){
                upload_progress_bar.setVisibility(View.VISIBLE);
                backup_image.setVisibility(View.GONE);
                upload_btn.setEnabled(false);
                download_btn.setEnabled(false);
                //lock autoUpdateUi
            }else if(state == WorkInfo.State.FAILED){
                upload_btn.setEnabled(true);
                download_btn.setEnabled(true);
                if(connected){
                    showConnectionError(getString(R.string.upload_error));
                    readDriveStorage();
                }
            }else {
                if(connected){
                    if(!periodic || uniqState != WorkInfo.State.RUNNING)
                    readDriveStorage();
                }
            }
        }
        if(!periodic){
            uniqState = state;
            currentState = state;
        }else{
            if(uniqState != WorkInfo.State.RUNNING){
                currentState = state;
            }
        }
    }

    private void resetPeriodicObserver() {
        if(periodicWorkInfos != null){
            periodicWorkInfos.removeObserver(periodicObserver);
        }
        periodicWorkInfos = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(PERIODIC_WORK_TAG);
        periodicWorkInfos.observe(this, periodicObserver);
    }




}
