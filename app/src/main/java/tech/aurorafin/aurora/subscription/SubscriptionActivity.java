package tech.aurorafin.aurora.subscription;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.core.content.ContextCompat;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import tech.aurorafin.aurora.R;

public class SubscriptionActivity extends AppCompatActivity implements View.OnClickListener, PurchasesUpdatedListener{
    AppCompatImageButton about_back_btn;

    ProgressBar billing_connect_progress_bar;
    TextView connect_error_txt;
    TextView reconnect_btn;

    LinearLayout connection_loader;
    LinearLayout subscription_options;

    private AppCompatToggleButton options_toggle_btns[];

    TextView activate_btn;
    ProgressBar activate_progress_bar;
    int blueRowColor;

    private BillingClient billingClient;

    private final String oneMosSku = "1month_plan";
    private final String sixMosSku = "6month_plan";
    private final String oneYrSku = "12month_plan";
    private final String foreverSku = "1pmt_plan";
    private String selectedSku;
    private String [] skuArray = new String[]{oneMosSku, sixMosSku, oneYrSku, foreverSku};
    private SkuDetails[] skuDetailsArray = new SkuDetails[4];

    TextView activation_error_message;
    private ObjectAnimator errorAnimator;
    private AnimatorListenerAdapter animatorListenerAdapter;

    TextView subs_management_1, subs_management_6, subs_management_12, promo_codes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        about_back_btn = findViewById(R.id.subscription_back_btn);
        about_back_btn.setOnClickListener(this);

        billing_connect_progress_bar = findViewById(R.id.billing_connect_progress_bar);
        connect_error_txt = findViewById(R.id.connect_error_txt);
        reconnect_btn = findViewById(R.id.reconnect_btn);
        reconnect_btn.setOnClickListener(this);

        connection_loader = findViewById(R.id.connection_loader);
        subscription_options = findViewById(R.id.subscription_options);

        options_toggle_btns = new AppCompatToggleButton[4];
        options_toggle_btns[0] = findViewById(R.id.option_1m);
        options_toggle_btns[1] = findViewById(R.id.option_6m);
        options_toggle_btns[2] = findViewById(R.id.option_1y);
        options_toggle_btns[3] = findViewById(R.id.option_fr);
        options_toggle_btns[0].setOnClickListener(this);
        options_toggle_btns[1].setOnClickListener(this);
        options_toggle_btns[2].setOnClickListener(this);
        options_toggle_btns[3].setOnClickListener(this);

        activate_btn = findViewById(R.id.activate_btn);
        activate_btn.setOnClickListener(this);
        activate_progress_bar = findViewById(R.id.activate_progress_bar);
        blueRowColor = ContextCompat.getColor(this, R.color.blue_row);

        activation_error_message = findViewById(R.id.activation_error_message);
        errorAnimator = ObjectAnimator.ofFloat(activation_error_message, "Alpha", 1, 0);
        errorAnimator.setStartDelay(1500);
        errorAnimator.setDuration(200);
        animatorListenerAdapter = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activateBtnStateUpdate("Enabled");
            }
        };


        subs_management_1 = findViewById(R.id.subs_management_1);
        subs_management_1.setMovementMethod(LinkMovementMethod.getInstance());

        subs_management_6 = findViewById(R.id.subs_management_6);
        subs_management_6.setMovementMethod(LinkMovementMethod.getInstance());

        subs_management_12 = findViewById(R.id.subs_management_12);
        subs_management_12.setMovementMethod(LinkMovementMethod.getInstance());

        promo_codes = findViewById(R.id.promo_codes);
        promo_codes.setMovementMethod(LinkMovementMethod.getInstance());

        InitializeBillingClient();

    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.subscription_back_btn:
                onBackPressed();
                break;
            case R.id.option_1m:
                optionSelectorClick(0);
                break;
            case R.id.option_6m:
                optionSelectorClick(1);
                break;
            case R.id.option_1y:
                optionSelectorClick(2);
                break;
            case R.id.option_fr:
                optionSelectorClick(3);
                break;
            case R.id.reconnect_btn:
                billing_connect_progress_bar.setVisibility(View.VISIBLE);
                connect_error_txt.setVisibility(View.GONE);
                reconnect_btn.setVisibility(View.GONE);
                InitializeBillingClient();
                break;
            case R.id.activate_btn:
                launchThePurchase();
                break;
        }
    }



    private void activateBtnStateUpdate(String state){
        switch (state) {
            case "Enabled":
                activate_btn.setBackground(ContextCompat.getDrawable(this, R.drawable.custom_btn_ripple));
                activate_btn.setTextColor(getResources().getColorStateList(R.color.pressed_txt_color));
                activate_btn.setText(getString(R.string.activate));
                activate_progress_bar.setVisibility(View.GONE);
                activate_btn.setEnabled(true);
                promo_codes.setVisibility(View.VISIBLE);
                break;
            case "Activating":
                activate_btn.setBackground(ContextCompat.getDrawable(this, R.drawable.connected_btn));
                activate_btn.setTextColor(blueRowColor);
                activate_btn.setText(getString(R.string.activate));
                activate_progress_bar.setVisibility(View.VISIBLE);
                activate_btn.setEnabled(false);
                promo_codes.setVisibility(View.GONE);
                break;
            case "Activated":
                activate_btn.setBackground(ContextCompat.getDrawable(this, R.drawable.connected_btn));
                activate_btn.setTextColor(blueRowColor);
                activate_btn.setText(getString(R.string.activated));
                activate_progress_bar.setVisibility(View.GONE);
                activate_btn.setEnabled(false);
                promo_codes.setVisibility(View.GONE);
                break;
        }
    }

    private void optionSelectorClick(int option) {
        for(int i  = 0; i < options_toggle_btns.length; i++){
            if(i == option){
                options_toggle_btns[i].setChecked(true);
                selectedSku = skuArray[i];
            }else {
                options_toggle_btns[i].setChecked(false);
            }
        }
    }

    private void InitializeBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    Log.d("MyTag","BillingResponseCode.OK");
                    clearGooglePlayStoreBillingCacheIfPossible();
                    getSubscriptionOptions();
                } else {
                    handleConnectError(billingResult.getDebugMessage());
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.d("MyTag","onBillingServiceDisconnected()");
                handleConnectError(getString(R.string.billingdisconnect));
            }
        });
    }

    private void clearGooglePlayStoreBillingCacheIfPossible() {
        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, new PurchaseHistoryResponseListener() {
            @Override
            public void onPurchaseHistoryResponse(@NonNull BillingResult billingResult, @Nullable List<PurchaseHistoryRecord> list) {

            }
        });
        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, new PurchaseHistoryResponseListener() {
            @Override
            public void onPurchaseHistoryResponse(@NonNull BillingResult billingResult, @Nullable List<PurchaseHistoryRecord> list) {
                
            }
        });
    }

    private void getSubscriptionOptions() {
        List<String> skuList = new ArrayList<>();
        skuList.add(oneMosSku);
        skuList.add(sixMosSku);
        skuList.add(oneYrSku);
        //skuList.add("1pmt_plan");
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
        billingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        // Process the result.
                        if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                            for(int i = 0; i < skuDetailsList.size(); i++){
                                if(skuDetailsList.get(i).getSku().equals(oneMosSku)){
                                    String txt =getString(R.string.onemonth) + " " + skuDetailsList.get(i).getPrice();
                                    options_toggle_btns[0].setText(txt);
                                    options_toggle_btns[0].setTextOn(txt);
                                    options_toggle_btns[0].setTextOff(txt);
                                    options_toggle_btns[0].setEnabled(true);
                                    skuDetailsArray[0] = skuDetailsList.get(i);
                                }else if(skuDetailsList.get(i).getSku().equals(sixMosSku)){
                                    String txt =getString(R.string.sixmonths) + " " + skuDetailsList.get(i).getPrice();
                                    options_toggle_btns[1].setText(txt);
                                    options_toggle_btns[1].setTextOn(txt);
                                    options_toggle_btns[1].setTextOff(txt);
                                    options_toggle_btns[1].setEnabled(true);
                                    skuDetailsArray[1] = skuDetailsList.get(i);
                                }else{
                                    String txt = getString(R.string.oneyear) + " " +skuDetailsList.get(i).getPrice();
                                    options_toggle_btns[2].setText(txt);
                                    options_toggle_btns[2].setTextOn(txt);
                                    options_toggle_btns[2].setTextOff(txt);
                                    options_toggle_btns[2].setEnabled(true);
                                    skuDetailsArray[2] = skuDetailsList.get(i);
                                }
                            }
                            getFixedOptions();

                        }else {
                            handleConnectError(billingResult.getDebugMessage());
                        }
                    }
                });
    }

    private void getFixedOptions() {
        List<String> skuList = new ArrayList<>();

        skuList.add(foreverSku);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(),
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult,
                                                     List<SkuDetails> skuDetailsList) {
                        // Process the result.
                        if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {
                            for(int i = 0; i < skuDetailsList.size(); i++){
                                if(skuDetailsList.get(i).getSku().equals(foreverSku)){
                                    String txt =getString(R.string.forever) + " " + skuDetailsList.get(i).getPrice();
                                    options_toggle_btns[3].setText(txt);
                                    options_toggle_btns[3].setTextOn(txt);
                                    options_toggle_btns[3].setTextOff(txt);
                                    options_toggle_btns[3].setEnabled(true);
                                    skuDetailsArray[3] = skuDetailsList.get(i);
                                }
                            }
                            checkSubsPurchases();

                        }else {
                            handleConnectError(billingResult.getDebugMessage());
                        }
                    }
                });
    }

    private void checkSubsPurchases() {
        Purchase.PurchasesResult purchasesResult =
                billingClient.queryPurchases(BillingClient.SkuType.SUBS);
        if(purchasesResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK
        &&purchasesResult.getPurchasesList() != null){
            for (Purchase purchase: purchasesResult.getPurchasesList()) {
                if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
                    if (!purchase.isAcknowledged()) {
                        handlePurchase(purchase);
                    }else {
                        makeFinalUiUpdate(purchase.getSku());
                    }
                    return;
                }
            }
            checkInAppPurchases();
        }else {
            handleConnectError(purchasesResult.getBillingResult().getDebugMessage());
        }
    }

    private void checkInAppPurchases() {
        Purchase.PurchasesResult purchasesResult =
                billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        if(purchasesResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK
                &&purchasesResult.getPurchasesList() != null){
            for (Purchase purchase: purchasesResult.getPurchasesList()) {
                if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
                    if (!purchase.isAcknowledged()) {
                        handlePurchase(purchase);
                    }else {
                        makeFinalUiUpdate(purchase.getSku());
                    }
                    return;
                }
            }
            makeFinalUiUpdate(null);
        }else {
            handleConnectError(purchasesResult.getBillingResult().getDebugMessage());
        }
    }

    private void makeFinalUiUpdate(String sku) {
        if(sku != null){
            disableSubsOptions(sku);
            activateBtnStateUpdate("Activated");
            updateSubsStatus(true, this);
            setSubStatusUpdateDate(this);
        }else {
            updateSubsStatus(false, this);
            optionSelectorClick(0);
            activateBtnStateUpdate("Enabled");
        }
        showSubsManagementLink(sku);
        showSubscriptionOptions();
    }

    private void showSubsManagementLink(String sku) {

        if(sku!=null && sku.equals(oneMosSku)){
            subs_management_1.setVisibility(View.VISIBLE);
            subs_management_6.setVisibility(View.GONE);
            subs_management_12.setVisibility(View.GONE);
        }else if(sku!=null && sku.equals(sixMosSku)){
            subs_management_1.setVisibility(View.GONE);
            subs_management_6.setVisibility(View.VISIBLE);
            subs_management_12.setVisibility(View.GONE);
        }else if (sku!=null && sku.equals(oneYrSku)){
            subs_management_1.setVisibility(View.GONE);
            subs_management_6.setVisibility(View.GONE);
            subs_management_12.setVisibility(View.VISIBLE);
        }else {
            subs_management_1.setVisibility(View.GONE);
            subs_management_6.setVisibility(View.GONE);
            subs_management_12.setVisibility(View.GONE);
        }
    }

    private void disableSubsOptions(String sku) {
        switch (sku) {
            case oneMosSku:
                optionSelectorClick(0);
                options_toggle_btns[1].setEnabled(false);
                options_toggle_btns[2].setEnabled(false);
                options_toggle_btns[3].setEnabled(false);

                options_toggle_btns[1].setVisibility(View.GONE);
                options_toggle_btns[0].setVisibility(View.VISIBLE);

                break;
            case sixMosSku:
                optionSelectorClick(1);
                options_toggle_btns[0].setEnabled(false);
                options_toggle_btns[2].setEnabled(false);
                options_toggle_btns[3].setEnabled(false);

                options_toggle_btns[0].setVisibility(View.GONE);
                options_toggle_btns[1].setVisibility(View.VISIBLE);

                break;
            case oneYrSku:
                optionSelectorClick(2);
                options_toggle_btns[1].setEnabled(false);
                options_toggle_btns[0].setEnabled(false);
                options_toggle_btns[3].setEnabled(false);
                break;
            case foreverSku:
                optionSelectorClick(3);
                options_toggle_btns[1].setEnabled(false);
                options_toggle_btns[2].setEnabled(false);
                options_toggle_btns[0].setEnabled(false);
                break;
        }
    }

    private void showSubscriptionOptions() {
        connection_loader.setVisibility(View.GONE);
        subscription_options.setVisibility(View.VISIBLE);
    }

    private void handleConnectError(String errMsg) {
        if(!isOnline()){
            showConnectionError(getString(R.string.network_error));
        }else {

            showConnectionError(errMsg);
        }
    }

    private void showConnectionError(String errorMsg) {
        connection_loader.setVisibility(View.VISIBLE);
        subscription_options.setVisibility(View.GONE);
        billing_connect_progress_bar.setVisibility(View.GONE);
        connect_error_txt.setText(errorMsg);
        connect_error_txt.setVisibility(View.VISIBLE);
        reconnect_btn.setVisibility(View.VISIBLE);
    }

    private boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr != null ? connMgr.getActiveNetworkInfo() : null;
        return (networkInfo != null && networkInfo.isConnected());
    }


    private void launchThePurchase() {
        activateBtnStateUpdate("Activating");
        SkuDetails skuSelected = getSelectedSku();
        if(skuSelected != null){
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuSelected)
                    .build();
            int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
            if(responseCode != BillingClient.BillingResponseCode.OK){
               showActivationError(getString(R.string.unexpected_err));
            }
        }else {
            showActivationError(getString(R.string.unexpected_err));
        }

    }

    private void showActivationError(String error) {
        if (error != null){
            activation_error_message.setText(error);
            activation_error_message.setAlpha(1);
            if(errorAnimator !=null){
                errorAnimator.removeListener(animatorListenerAdapter);
                errorAnimator.cancel();
            }
            errorAnimator.addListener(animatorListenerAdapter);
            errorAnimator.start();
        }
    }

    private SkuDetails getSelectedSku() {
        SkuDetails skuDetails = null;
        for(int i  = 0; i < options_toggle_btns.length; i++){
            if(options_toggle_btns[i].isChecked()){
                 skuDetails = skuDetailsArray[i];
                 return skuDetails;

            }
        }
        return skuDetails;
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
                    if (!purchase.isAcknowledged()) {
                        handlePurchase(purchase);
                    }else {
                        makeFinalUiUpdate(purchase.getSku());
                    }
                    return;
                }
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            showActivationError(getString(R.string.cancel1));
        } else {
            // Handle any other error codes.
            showActivationError(billingResult.getDebugMessage());
        }
    }

    void handlePurchase(final Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                        if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                            makeFinalUiUpdate(purchase.getSku());
                        }else {
                            showActivationError(billingResult.getDebugMessage());
                        }
                    }
                });
            }
        }
    }


    public static boolean isPremiumActive(Context context){
        return true; // FREE APP FOR EVERYONE!!!
      /*  Log.d("MyTag","isPremiumActive");
        boolean hasSubInShared = getSubsStatus(context);
        if(hasSubInShared){
            Date lastUpdateDate = getSubsStatusUpdateDate(context);
            Date today = new Date(System.currentTimeMillis());
            long diff = today.getTime() - lastUpdateDate.getTime();
            float dayCount = (float) diff / (24 * 60 * 60 * 1000);
            if(dayCount > 1){
                initializeSubDateUpdate(context);
                return true;
            }else {
                return true;
            }
        }else {
            return false;
        }*/
    }

    private static void initializeSubDateUpdate(final Context context) {
        Log.d("MyTag","initializeSubDateUpdate");
        boolean async = Looper.myLooper() == Looper.getMainLooper();
        if(async){
            Runnable updater = new Runnable() {
                @Override
                public void run() {
                    Log.d("MyTag","Runnable SsilentSubscriptionStatusUpdater()");
                    silentSubscriptionStatusUpdater(context);
                }
            };
            Handler h = new Handler();
            h.post(updater);
        }else {
            silentSubscriptionStatusUpdater(context);
        }
    }

    private static void silentSubscriptionStatusUpdater(final Context context){
        Log.d("MyTag","SsilentSubscriptionStatusUpdater()");
        final BillingClient bc = BillingClient.newBuilder(context)
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {

                    }
                })
                .enablePendingPurchases()
                .build();

        bc.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK) {

                    // The BillingClient is ready, query subscriptions:.
                    Purchase.PurchasesResult purchasesResult =
                            bc.queryPurchases(BillingClient.SkuType.SUBS);
                    if(purchasesResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK
                            &&purchasesResult.getPurchasesList() != null){
                        for (Purchase purchase: purchasesResult.getPurchasesList()) {
                            if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
                                updateSubsStatus(true, context);
                                setSubStatusUpdateDate(context);
                                return;
                            }
                        }

                    }else{
                        //Log.d("MyTag","return");
                        return;
                    }
                    // The BillingClient is ready, query in app purchases
                    purchasesResult =
                            bc.queryPurchases(BillingClient.SkuType.INAPP);
                    if(purchasesResult.getResponseCode() ==  BillingClient.BillingResponseCode.OK
                            &&purchasesResult.getPurchasesList() != null){
                        for (Purchase purchase: purchasesResult.getPurchasesList()) {

                            // crutch to catch refund state/ doesn't work well
                                    /*int unmaskedState = getUnmaskedPurchaseState(purchase);
                                    Log.d("MyTag","unmaskedState = " + unmaskedState);
                                    if(unmaskedState == Purchase.PurchaseState.UNSPECIFIED_STATE
                                            && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
                                        ConsumeParams consumeParams =
                                                ConsumeParams.newBuilder()
                                                        .setPurchaseToken(purchase.getPurchaseToken())
                                                        .build();

                                        ConsumeResponseListener listener = new ConsumeResponseListener() {
                                            @Override
                                            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                                    // Handle the success of the consume operation.
                                                }
                                            }
                                        };
                                        Log.d("MyTag","bc.consumeAsync(consumeParams, listener);");
                                        bc.consumeAsync(consumeParams, listener);

                                    }else */
                            if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
                                updateSubsStatus(true, context);
                                setSubStatusUpdateDate(context);
                                //Log.d("MyTag"," updateSubsStatus(true, context);");
                                return;
                            }
                        }

                    }else {
                        //Log.d("MyTag","return");
                        return;
                    }
                    //Log.d("MyTag","updateSubsStatus(false, context)");
                    updateSubsStatus(false, context);
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.

            }
        });

    }


    private static int getUnmaskedPurchaseState(Purchase purchase)
    {
        int purchaseState = 1;
        try {
            purchaseState = new JSONObject(purchase.getOriginalJson()).optInt("purchaseState", 1);
        } catch (JSONException e)
        {
            e.printStackTrace();
        }

        return purchaseState;
    }

    public static void updateSubsStatus(boolean hasSub, Context context) {
        SharedPreferences settings =
                context.getSharedPreferences("SUBS_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("HAS_SUB", hasSub);
        editor.apply();
    }

    public static boolean getSubsStatus(Context context){
        SharedPreferences sharedPref =context.getSharedPreferences(
                "SUBS_PREFS", Context.MODE_PRIVATE);
        return sharedPref.getBoolean("HAS_SUB", false);
    }

    public static void setSubStatusUpdateDate(Context context) {
        SharedPreferences settings =
                context.getSharedPreferences("SUBS_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("UPDATE_DATE", new Date(System.currentTimeMillis()).getTime());
        editor.apply();
    }

    public static Date getSubsStatusUpdateDate(Context context){
        SharedPreferences sharedPref =context.getSharedPreferences(
                "SUBS_PREFS", Context.MODE_PRIVATE);
        return new Date(sharedPref.getLong("UPDATE_DATE", 0));
    }
}

