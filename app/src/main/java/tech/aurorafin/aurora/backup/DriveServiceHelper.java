package tech.aurorafin.aurora.backup;
import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import tech.aurorafin.aurora.dbRoom.CashFlowDB;


public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;
    Context mContext;

    public DriveServiceHelper(Drive mDriveService, Context context) {
        this.mDriveService = mDriveService;
        this.mContext = context;
    }


    public Task<Boolean>downloadDrive(){
        return Tasks.call(mExecutor, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                String logFileId = getLogFileId(mDriveService);
                if(logFileId!=null){
                    FileList files;
                    boolean correct = BackupWorker.isBackUpCorrect(mDriveService, logFileId);
                    if(!correct){
                        String prevFolderId = BackupWorker.getPrevFolderId(mDriveService);
                        files = mDriveService.files().list()
                                .setQ(String.format("mimeType='application/x-sqlite3' and '%s' in parents", prevFolderId))
                                .setSpaces("appDataFolder")
                                .setFields("files(id, name)")
                                .execute();
                    }else {
                        files = mDriveService.files().list()
                                .setQ("mimeType='application/x-sqlite3' and 'appDataFolder' in parents")
                                .setSpaces("appDataFolder")
                                .setFields("files(id, createdTime, name)")
                                .execute();
                    }
                    CashFlowDB.setLastDbCloseCorrect(mContext, false);
                    java.io.File db = mContext.getDatabasePath(CashFlowDB.cash_flow_db);
                    java.io.File dbDir = new java.io.File(db.getParent());
                    for (java.io.File file: dbDir.listFiles()){
                        if (!file.isDirectory()){
                            file.delete();
                        }
                    }
                    for (File file : files.getFiles()) {
                        java.io.File dest = new java.io.File(db.getParent(), file.getName());
                        OutputStream os = new FileOutputStream(dest);
                        mDriveService.files().get(file.getId())
                                .executeMediaAndDownloadTo(os);
                        os.close();
                    }
                    CashFlowDB.setLastDbCloseCorrect(mContext, true);
                    CashFlowDB.copyPrevDB(mContext);
                    return true;

                }else {
                    return null;
                }

            }
        });
    }


    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                String logFileId = getLogFileId(mDriveService);
                if(logFileId!=null){
                    boolean correct = BackupWorker.isBackUpCorrect(mDriveService, logFileId);

                    if(!correct){
                        String prevFolderId = BackupWorker.getPrevFolderId(mDriveService);
                        return  mDriveService.files().list()
                                .setQ(String.format("'%s' in parents", prevFolderId))
                                .setSpaces("appDataFolder")
                                .setFields("files(id, createdTime, name)")
                                .execute();
                    }
                    return mDriveService.files().list()
                            .setSpaces("appDataFolder")
                            .setQ("'appDataFolder' in parents")
                            .setFields("files(id, createdTime, name)")
                            .execute();

                }else {
                    return null;
                }
            }
        });
    }

    private String getLogFileId(Drive mDriveService) throws Exception {
        FileList result = mDriveService.files().list()
                .setQ(String.format("mimeType='text/plain' and name = '%s'", BackupWorker.DRIVE_LOG_FILE))
                .setSpaces("appDataFolder")
                .setFields("files(id, name)")
                .execute();
        for (File file : result.getFiles()) {
            return  file.getId();
        }
        return null;
    }




}
