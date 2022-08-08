package tech.aurorafin.aurora.dbRoom;

import android.content.Context;


import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import tech.aurorafin.aurora.R;
import tech.aurorafin.aurora.about.AboutActivity;


@Database(entities = {Aggregator.class, Category.class, Plan.class, PlanTotal.class, Operation.class, OperationTotal.class}, version = 1)
public abstract class CashFlowDB extends RoomDatabase {

    private static CashFlowDB instance;
    private static Context mContext;

    public abstract AggregatorDao aggregatorDao();
    public abstract CategoryDao categoryDao();
    public abstract PlanDao planDao();
    public abstract PlanTotalDao planTotalDao();
    public abstract OperationDao operationDao();
    public abstract OperationTotalDao operationTotalDao();

    public static String cash_flow_db = "cash_flow_db";
    private static String DbCheckFile = "db_check_file.txt";

    public static synchronized CashFlowDB getInstance(Context context){
        if(instance == null){
            mContext = context;
            if(!isLastDbCloseCorrect(context)){
                Log.d("MyTag","Recovery");
                if(isPrevExist(context)){
                    if(restorePrev(context)){
                        showDbRestoredDialog(context);
                    }
                }
            }
            instance = Room.databaseBuilder(context.getApplicationContext(),
                   CashFlowDB.class, cash_flow_db).addCallback(roomCallBack).build();
        }
        return instance;
    }

    private static RoomDatabase.Callback roomCallBack = new RoomDatabase.Callback(){
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Thread t = new Thread(new Runnable() {
                public void run() {
                    AggregatorDao ad = instance.aggregatorDao();
                    CategoryDao cd = instance.categoryDao();
                    ad.insert(new Aggregator(CategoriesRepository.ACategory.EMPTY_AGGREGATOR,"No", "No"));
                    if(mContext!=null){
                        Log.d("MyTag","Coping First Prev");
                        copyPrevDB(mContext);
                    }
                }
            });
            t.start();
            Intent intent1 = new Intent(mContext, AboutActivity.class);
            mContext.startActivity(intent1);
        }
    };

    private static void showDbRestoredDialog(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.database_recovery)
                .setMessage(R.string.database_recovery_message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static synchronized boolean isLastDbCloseCorrect(Context context) {
        boolean correct = false;
        File file = new File(context.getFilesDir(), DbCheckFile);
        try {
            FileInputStream fin = new FileInputStream(file);
            DataInputStream din = new DataInputStream(fin);
            correct = din.readBoolean();
            din.close();
        } catch (IOException e) {
                setLastDbCloseCorrect(context, true);
                correct = true;
        }
        return correct;
    }

    public static synchronized void setLastDbCloseCorrect(Context context, boolean correct){
        File file = new File(context.getFilesDir(), DbCheckFile);
        try {
        if(!file.exists()){
            file.createNewFile();
        }
            FileOutputStream fos = new FileOutputStream(file);
            DataOutputStream dos = new DataOutputStream(fos);
            dos.writeBoolean(correct);
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void copyPrevDB(Context context){
        File prevDir = new File(context.getFilesDir() + "/prev");
        if(!prevDir.exists()){
            prevDir.mkdir();
        }else {
            for (File file: prevDir.listFiles()){
                if (!file.isDirectory()){
                    file.delete();
                }
            }
        }
        File db = context.getDatabasePath(cash_flow_db);
        File dbShm = new File(db.getParent(), cash_flow_db+"-shm");
        File dbWal = new File(db.getParent(), cash_flow_db+"-wal");
        File db2 = new File(context.getFilesDir() + "/prev", cash_flow_db);
        File dbShm2 = new File(db2.getParent(), cash_flow_db+"-shm");
        File dbWal2 = new File(db2.getParent(), cash_flow_db+"-wal");

        try {
            copyFileUsingStream(db, db2);
            copyFileUsingStream(dbShm, dbShm2);
            copyFileUsingStream(dbWal, dbWal2);
        } catch (Exception e) {
        }
    }
    private static boolean restorePrev(Context context){
        try {
            File db = context.getDatabasePath(cash_flow_db);
            File dbDir = new File(db.getParent());
            for (File file: dbDir.listFiles()){
                if (!file.isDirectory()){
                    file.delete();
                }
            }
            File dbShm = new File(db.getParent(), cash_flow_db+"-shm");
            File dbWal = new File(db.getParent(), cash_flow_db+"-wal");

            File db2 = new File(context.getFilesDir() + "/prev", cash_flow_db);
            File dbShm2 = new File(db2.getParent(), cash_flow_db+"-shm");
            File dbWal2 = new File(db2.getParent(), cash_flow_db+"-wal");


            copyFileUsingStream(db2,db);
            copyFileUsingStream(dbShm2, dbShm);
            copyFileUsingStream(dbWal2, dbWal);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        if(source.exists()){
            InputStream is = null;
            OutputStream os = null;
            try {
                Log.d("MyTag","Coping");
                is = new FileInputStream(source);
                os = new FileOutputStream(dest);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
                os.flush();
            } finally {
                is.close();
                os.close();
            }
        }
    }

    private static boolean isPrevExist(Context context){
        File prevDB = new File(context.getFilesDir() + "/prev", cash_flow_db);
        return  prevDB.exists();
    }


}
