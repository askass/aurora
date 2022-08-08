package tech.aurorafin.aurora;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import tech.aurorafin.aurora.about.AboutActivity;
import tech.aurorafin.aurora.dbRoom.CategoriesRepository;
import tech.aurorafin.aurora.dbRoom.OperationRepository;
import tech.aurorafin.aurora.dbRoom.PlanRepository;
import tech.aurorafin.aurora.backup.BackupActivity;
import tech.aurorafin.aurora.dbRoom.AnalysisRepository;
import tech.aurorafin.aurora.export.ExportActivity;
import tech.aurorafin.aurora.subscription.SubscriptionActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.whinc.widget.ratingbar.RatingBar;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static tech.aurorafin.aurora.subscription.SubscriptionActivity.isPremiumActive;


public class MainActivity extends AppCompatActivity implements MainActivityCommunication, DbService.DbServiceCallback {

    PlanFragment planFragment;
    AnalysisFragment analysisFragment;
    BalanceFragment balanceFragment;
    OperationsFragment operationsFragment;
    CategoriesFragment categoriesFragment;

    private static final int PLAN_FRAGMENT = 1;
    private static final int DASH_FRAGMENT = 2;
    private static final int BALANCE_FRAGMENT = 3;
    private static final int OPERATIONS_FRAGMENT = 4;
    private static final int CATEGORIES_FRAGMENT = 5;
    private int currentFragment = 3;
    private static final String currentFragmentKey = "current_fragment_key";

    Context mContext;
    ThreadPoolExecutor executor;
    private Handler handler = new Handler(Looper.getMainLooper());

    TextView appBarText;
    BottomNavigationView bottomNavigationView;

    /*DB service*/
    CategoriesRepository categoriesRepository;
    PlanRepository planRepository;
    OperationRepository operationRepository;
    AnalysisRepository analysisRepository;
    DbService mDbService;
    boolean mBound = false;

    public static final int DB_SERVICE_IS_NULL = 0;

    /*ANIMATORS*/
    private ObjectAnimator alpha_BNV;
    private AnimatorListenerAdapter goneBottomNav;
    private AnimatorListenerAdapter visibleBottomNav;

    /*DateFormat*/
    DateFormatSelector dateFormatSelector;
    AlertDialog dateFormatDialog;
    int Dp300;

    /*Rate US*/
    AlertDialog rateUsDialog;
    RatingBar ratingBar;
    public static final String RATE_FILE_KEY = "RATE_FILE_KEY";
    public static final String RATE_STATE_KEY = "RATE_STATE_KEY";
    TextView rate_btn;
    /*Feedback Us*/
    AlertDialog feedBackDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(null);

        if(savedInstanceState!= null){
             currentFragment = savedInstanceState.getInt(currentFragmentKey, 3);
        }
        setContentView(R.layout.activity_main);
        mContext = this;

        /*Thread POOL*/
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

        /*Databases REPOS*/
        categoriesRepository = new CategoriesRepository(this, executor, handler);
        planRepository = new PlanRepository(this, executor, handler);
        operationRepository = new OperationRepository(this, executor, handler);
        analysisRepository = new AnalysisRepository(this, executor);

        /*Set ToolBar*/
        Toolbar toolbar = findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        appBarText = findViewById(R.id.app_bar_text);

        /*SET BOTTOM_NAVIGATION_VIEW*/
        bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        bottomNavigationView.setSaveEnabled(false);
        bottomNavigationView.setItemIconTintList(null);

        /*Create fragments and manage them */
        planFragment = new PlanFragment (this, bottomNavigationView, categoriesRepository, planRepository, this);
        analysisFragment = new AnalysisFragment(this, categoriesRepository, analysisRepository, this);
        operationsFragment = new OperationsFragment(this, categoriesRepository, operationRepository, this);
        categoriesFragment = new CategoriesFragment(this, categoriesRepository, this);
        balanceFragment = new BalanceFragment(this, categoriesRepository, operationRepository, this);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                boolean result = false;
                switch (id) {
                    case R.id.planBtn:
                        setFragment(planFragment);
                        currentFragment = PLAN_FRAGMENT;
                        result = true;
                        break;
                    case R.id.dashBtn:
                        setFragment(analysisFragment);
                        appBarText.setText(getText(R.string.analysis));
                        currentFragment = DASH_FRAGMENT;
                        result = true;
                        break;
                    case R.id.balanceBtn:
                        setFragment(balanceFragment);
                        appBarText.setText(getText(R.string.balance));
                        currentFragment = BALANCE_FRAGMENT;
                        result = true;
                        break;
                    case R.id.operationsBtn:
                        setFragment(operationsFragment);
                        currentFragment = OPERATIONS_FRAGMENT;
                        result = true;
                        break;
                    case R.id.categoriesBtn:
                        setFragment(categoriesFragment);
                        appBarText.setText(getText(R.string.categories));
                        currentFragment = CATEGORIES_FRAGMENT;
                        result = true;
                        break;
                }
                return result;
            }
        });

        switch (currentFragment) {
            case PLAN_FRAGMENT:
                setFragment(planFragment);
                bottomNavigationView.setSelectedItemId(R.id.planBtn);
                break;
            case DASH_FRAGMENT:
                setFragment(analysisFragment);
                bottomNavigationView.setSelectedItemId(R.id.dashBtn);
                break;
            case BALANCE_FRAGMENT:
                setFragment(balanceFragment);
                appBarText.setText(getText(R.string.balance));
                bottomNavigationView.setSelectedItemId(R.id.balanceBtn);
                break;
            case OPERATIONS_FRAGMENT:
                setFragment(operationsFragment);
                bottomNavigationView.setSelectedItemId(R.id.operationsBtn);
                break;
            case CATEGORIES_FRAGMENT:
                setFragment(categoriesFragment);
                appBarText.setText(getText(R.string.categories));
                bottomNavigationView.setSelectedItemId(R.id.categoriesBtn);
                break;
        }

         /*ANIMATIONS*/
            goneBottomNav = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    bottomNavigationView.setVisibility(View.GONE);
                }
            };
            visibleBottomNav = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }
            };

        /*DateFormat*/
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        dateFormatSelector = new DateFormatSelector(this);
        builder.setTitle(R.string.dateformat);
        builder.setView(dateFormatSelector);
       /* builder.setNegativeButton(R.string.cancel1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });*/
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                DateFormater.updateDateFormatKey(dateFormatSelector.formatCode, MainActivity.this);
                dateFormatSelector.updateRbCheck();
            }
        });
        dateFormatDialog = builder.create();
        Dp300 = dateFormatSelector.Dp300;
        //dateFormatDialog

        /*RATE US*/
        AlertDialog.Builder rateBuilder = new AlertDialog.Builder(mContext);
        LinearLayout ll = (LinearLayout) getLayoutInflater().inflate(R.layout.rate_us_dialog, null);
        rateBuilder.setTitle(R.string.rate_us_head);
        ratingBar = ll.findViewById(R.id.rating_bar);
        rate_btn = ll.findViewById(R.id.rate_btn);
        rateBuilder.setView(ll);
        rate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRating();
            }
        });

        rateUsDialog = rateBuilder.create();

        /*FEEDBACK*/

        AlertDialog.Builder feedBuilder = new AlertDialog.Builder(mContext);
        LinearLayout ll2 = (LinearLayout) getLayoutInflater().inflate(R.layout.feedback_dialog, null);


        feedBuilder.setTitle(R.string.feedback);
        feedBuilder.setView(ll2);
        feedBuilder.setNegativeButton(R.string.cancel1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        feedBuilder.setPositiveButton(R.string.feedback_send, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                sendFeedbackToSupport();
            }
        });
        feedBackDialog = feedBuilder.create();

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(currentFragmentKey, currentFragment);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.app_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_backup) {
            Intent intent1;
            if(isPremiumActive(this)){
                intent1 = new Intent(this, BackupActivity.class);
            }else {
                intent1 = new Intent(this, SubscriptionActivity.class);
            }
            this.startActivity(intent1);
            return true;
        }else if (id == R.id.action_export) {
            Intent intent1;
            if(isPremiumActive(this)){
                intent1 = new Intent(this, ExportActivity.class);
            }else {
                intent1 = new Intent(this, SubscriptionActivity.class);
            }
            this.startActivity(intent1);
            return true;
        }else if(id == R.id.action_date_format){
            dateFormatSelector.resetRbCheck();
            dateFormatDialog.show();
            dateFormatDialog.getWindow().setLayout(Dp300, ViewGroup.LayoutParams.WRAP_CONTENT);
            return true;
        }else if(id == R.id.action_about){
            Intent intent1 = new Intent(this, AboutActivity.class);
            this.startActivity(intent1);
            return true;
        }else if(id == R.id.action_subscription){
            Intent intent1 = new Intent(this, SubscriptionActivity.class);
            this.startActivity(intent1);
            return true;
        }else if(id == R.id.action_rate){
            setRateViewed();
            rateUsDialog.show();
            //rateUsDialog.getWindow().setLayout(Dp300, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return super.onOptionsItemSelected(item);
    }


    /*Rating handler*/
    private void handleRating(){
        rateUsDialog.cancel();
        if(ratingBar.getCount() < 4){
            feedBackDialog.show();
        }else {
            final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        }
    }

    private void sendFeedbackToSupport(){
        feedBackDialog.cancel();
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        String [] s = new String [] {"support@aurorafin.tech"};
        intent.putExtra(Intent.EXTRA_EMAIL, s);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback));
        startActivity(intent);
    }

    private void setRateViewed() {
        SharedPreferences settings =
                getSharedPreferences(RATE_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(RATE_STATE_KEY, 1);
        editor.apply();
    }

    /*Fragment Setter */
    private void setFragment(Fragment fragment){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentHolder, fragment);
        fragmentTransaction.commit();
    }


    public void setAppBarText(String txt) {
        this.appBarText.setText(txt);
    }


    //-----DataBase Service------------------------------------

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d("MyTag", "Activity onResume()");
        startDbService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Log.d("MyTag", "Activity onStop()");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private void startDbService(){
        Intent serviceIntent = new Intent(this, DbService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService();
    }
    private void bindService(){
        Intent serviceIntent = new Intent(this, DbService.class);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            DbService.DbServiceBinder binder = (DbService.DbServiceBinder) service;
            mDbService = binder.getService();
            binder.setDbServiceCallback(MainActivity.this);
            mBound = true;
            updateConnectionPointers(mDbService);
            categoriesRepository.updateLockedCategories(mDbService.getLockedCategories());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mDbService = null;
            mBound = false;
            updateConnectionPointers(null);
        }
    };

    private void updateConnectionPointers(DbService dbService){
        planFragment.setmDbService(dbService);
        balanceFragment.setmDbService(dbService);
        operationsFragment.setmDbService(dbService);
        categoriesFragment.setmDbService(dbService);
    }

    @Override
    public void unlockCategory(long categoryId) {
        this.categoriesRepository.unlockCategory(categoryId);
    }
    @Override
    public void lockCategory(long categoryId) {
        this.categoriesRepository.lockCategory(categoryId);
    }



    //-----MainActivityCommunication------------------------------------

    @Override
    public void setAppbarText(String s) {
        this.appBarText.setText(s);
    }

    @Override
    public void makeToast(int toast) {
       handler.post(new ToastMaker(toast));
    }

    @Override
    public int getBottomNavigationHeight() {
        return bottomNavigationView.getHeight();
    }

    @Override
    public void hideBottomNavigation() {
        bottomNavigationView.setEnabled(false);
        alpha_BNV = ObjectAnimator.ofFloat(bottomNavigationView, "Alpha", 1f, 0f);
        alpha_BNV.setDuration(20);
        alpha_BNV.removeListener(visibleBottomNav);
        alpha_BNV.addListener(goneBottomNav);
        alpha_BNV.setStartDelay(80);
        alpha_BNV.start();
    }

    @Override
    public void showBottomNavigation() {
        bottomNavigationView.setEnabled(true);
        alpha_BNV = ObjectAnimator.ofFloat(bottomNavigationView, "Alpha", 0f, 2f);
        alpha_BNV.setDuration(120);
        alpha_BNV.removeListener(goneBottomNav);
        alpha_BNV.addListener(visibleBottomNav);
        alpha_BNV.setStartDelay(110);
        alpha_BNV.start();
    }

    @Override
    public void transitToAnalysis(int dateCodeFrom, int dateCodeTo, boolean rev, boolean exp, boolean cap, boolean factChip, boolean selectAll) {
        analysisRepository.REV = rev;
        analysisRepository.EXP = exp;
        analysisRepository.CAP = cap;
        analysisRepository.analysisDateCodeFrom = dateCodeFrom;
        analysisRepository.analysisDateCodeTo = dateCodeTo;
        analysisRepository.selectAll = selectAll;
        analysisRepository.aggregate = true;
        analysisRepository.planChip = false;
        analysisRepository.factChip = factChip;
        analysisRepository.abs = false;
        analysisRepository.cumulative = true;
        analysisRepository.planChart = true;
        analysisRepository.factChart = true;
        setFragment(analysisFragment);
        appBarText.setText(getText(R.string.analysis));
        bottomNavigationView.setSelectedItemId(R.id.dashBtn);
        currentFragment = DASH_FRAGMENT;

    }

    @Override
    public void transitionToOperations(long categoryId, int year){
        operationRepository.tempOperationCategoryId = categoryId;
        operationRepository.tempOperationYear = year;
        setFragment(operationsFragment);
        bottomNavigationView.setSelectedItemId(R.id.operationsBtn);
        currentFragment = OPERATIONS_FRAGMENT;
    }

    @Override
    public void setAnalysisSelectedCategories(HashMap<Long, Boolean> appliedSelectedCategories) {
        analysisRepository.setAppliedSelectedCategories(appliedSelectedCategories);
        analysisRepository.selectAll = false;
    }

    @Override
    public void askForFeedback() {
        setRateViewed();
        rateUsDialog.show();
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
                case DB_SERVICE_IS_NULL:
                    sToast = getString(R.string.DB_SERVICE_IS_NULL);
                    break;
            }
            if(sToast != null){
                Toast.makeText(mContext, sToast, Toast.LENGTH_SHORT).show();
            }

        }
    }




}
