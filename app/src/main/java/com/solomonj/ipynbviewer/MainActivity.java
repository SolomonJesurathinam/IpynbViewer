package com.solomonj.ipynbviewer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchaseHistoryParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.common.collect.ImmutableList;
import java.util.List;


public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemSelectedListener {

    private Button button;
    private Spinner spinner;
    private String[] dropValues;
    private ImageView infologo,removeAds;
    private String render;
    private BillingClient billingClient;
    private ProductDetails productDetails;
    private Purchase purchase;
    private static final String TAG = MainActivity.class.getName();
    public static final String MyPRE = "adFlagger" ;
    SharedPreferences sharedPref;
    boolean adFlag;
    private InterstitialAd mInterstitialAd;
    private AdView mAdView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getSharedPreferences(MyPRE, Context.MODE_PRIVATE);
        adFlag = sharedPref.getBoolean("adFlag",false);
        Log.i(TAG, "Flag - Default value from Shared Preferences: "+adFlag);


        //Adview with flag condition
        if(adFlag == false){
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                }
            });
            requestNewBanner();
            requestNewInterstitial();
        }else{
            Log.i(TAG,"AdFlag is "+adFlag+" so the ads are not loaded");
        }


        //initialise button and spinner
        button = findViewById(R.id.getStarted);
        spinner = findViewById(R.id.selectRender);


        //Image to remove ads with on click
        removeAds = findViewById(R.id.removeads);
        removeAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    makePurchase();
                }catch (NullPointerException e){
                    Log.e(TAG,"Billing error "+e.toString());
                }

            }
        });


        //Render dropdown values
        dropValues = getResources().getStringArray(R.array.dropdown);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, dropValues);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);


        //Get started - button click
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), WebviewActivity.class);
                intent.putExtra("render",render);
                startActivity(intent);
            }
        });


        //info logo image with on click
        infologo = findViewById(R.id.infologo);
        infologo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                }
                Intent intent = new Intent(getApplicationContext(),information.class);
                startActivity(intent);
            }
        });

    }


    //Dropdown items selected callback
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Toast.makeText(getApplicationContext(), dropValues[i], Toast.LENGTH_SHORT).show();
        render = dropValues[i];
    }
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


    //Billing Listener for previous and current purchases
    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    completePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.w(TAG, "onPurchasesUpdated: Purchase Canceled");
                billingClient.endConnection();
                Log.w(TAG,"On Connection Status: Terminated");
            } else {
                Log.e(TAG, "onPurchasesUpdated: Error");
                billingClient.endConnection();
                Log.w(TAG,"On Connection Status: Terminated");
            }

        }
    };

    //connect to Google Billing
    private void billingClient(){
        billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        connectToGooglePlayBilling();
    }
    private void connectToGooglePlayBilling(){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG,"On Connection Status : Disconnected");
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    Log.i(TAG,"On Connection Status: Connection is successful");
                    purchasesHistory();
                    getProductDetails();
                }
            }
        });
    }


    //Get Product details for Premium Content
    private void getProductDetails(){
        QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                        .setProductList(ImmutableList.of(QueryProductDetailsParams.Product.newBuilder().setProductId("com.solomonj.ipynbview.ads")
                                .setProductType(BillingClient.ProductType.INAPP)
                                                .build()))
                .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                new ProductDetailsResponseListener() {
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> productDetailsList) {
                        if (!productDetailsList.isEmpty()) {
                            productDetails = productDetailsList.get(0);
                            Log.i(TAG,"onProductDetailsResponse : Products: "+productDetails.getName().toString());
                        } else {
                            Log.w(TAG, "onProductDetailsResponse: No products");
                        }
                    }
                }
        );
    }


    //Purchase Flow
    private void makePurchase(){
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(
                ImmutableList.of(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).build())
        ).build();

        billingClient.launchBillingFlow(this,billingFlowParams);
    }


    //Complete Purchase and set the flag
    private void completePurchase(Purchase item) {
        purchase = item;
        if(purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED){
            if(!purchase.isAcknowledged()){
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                        Log.i(TAG,"On Purchases: Purchase Complete and Acknowledged "+purchase.getProducts().toString());
                        editSharedPref(true);
                        adFlag = sharedPref.getBoolean("adFlag",false);
                        Log.i(TAG,"FLAG - Value set to true due to Purchase and Acknowledge Complete: "+adFlag);
                    }
                });
            }else{
                Log.i(TAG,"On Purchases: Purchase Complete "+purchase.getProducts().toString());
                editSharedPref(true);
                adFlag = sharedPref.getBoolean("adFlag",false);
                Log.i(TAG,"FLAG - Value set to true due to Purchase Complete: "+adFlag);
            }
        }
    }


    //Get Purchase History
    private void purchasesHistory(){
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(
                        BillingClient.ProductType.INAPP).build(),
                new PurchasesResponseListener() {
                    public void onQueryPurchasesResponse(BillingResult billingResult, List purchases) {
                        if(!purchases.isEmpty()){
                            for(Object singlePurchase : purchases){
                                Log.i(TAG,"Purchase History "+singlePurchase.toString());
                                if(singlePurchase.toString().contains("com.solomonj.ipynbview.ads")){
                                    if(singlePurchase.toString().contains("\"purchaseState\":0")){
                                        if(singlePurchase.toString().contains("\"acknowledged\":false")){
                                            acknowlwdgePendingPurchase(getPurchaseToken(singlePurchase));
                                        }else{
                                            editSharedPref(true);
                                            adFlag = sharedPref.getBoolean("adFlag",false);
                                            Log.i(TAG,"FLAG - Value set to true due to Previous Purchase and Acknowledge Complete: "+adFlag);
                                        }
                                    }else{
                                        Log.e(TAG,"Purchase State is not 0");
                                    }
                                }else{
                                    Log.i(TAG,"Purchase History: Have not purchased com.solomonj.ipynbview.ads");
                                    editSharedPref(false);
                                    adFlag = sharedPref.getBoolean("adFlag",false);
                                    Log.i(TAG,"FLAG - Value set to false as there are no purchases for com.solomonj.ipynbview.ads: "+adFlag);
                                }
                            }
                        }else{
                            Log.i(TAG,"No Purchases available");
                            adFlag = sharedPref.getBoolean("adFlag",false);
                            Log.i(TAG,"FLAG - Default value when there are no purchases: "+adFlag);
                        }
                    }
                }
        );
    }


    //On Resume method to initialize billing Client
    @Override
    protected void onResume() {
        super.onResume();
        billingClient();
    }


    //On Stop method to close billing Client
    @Override
    protected void onStop() {
        super.onStop();
        billingClient.endConnection();
        Log.w(TAG,"On Connection Status: Terminated");
    }


    //shared preferences editor method
    private void editSharedPref(boolean flagValue){
        Boolean checkFlag = sharedPref.getBoolean("adFlag",false);
        if(flagValue != checkFlag){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("adFlag",flagValue);
            editor.apply();
            editor.commit();
        }else{
            Log.w(TAG,"Flag is already "+flagValue+" so not editing");
        }
    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
    }


    //Rectangle Banner Ad
    private void requestNewBanner(){
        //Original id unit - ca-app-pub-4449150732190604/3922551687
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    //Interestial ad
    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();

        //Interestial Ad
        //Original id unit - ca-app-pub-4449150732190604/3814740471
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.d(TAG, loadAdError.toString());
                mInterstitialAd = null;
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                mInterstitialAd = interstitialAd;
                Log.i(TAG, "onAdLoaded");
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override
                    public void onAdClicked() {
                        // Called when a click is recorded for an ad.
                        Log.d(TAG, "Ad was clicked.");
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when ad is dismissed.
                        // Set the ad reference to null so you don't show the ad a second time.
                        Log.d(TAG, "Ad dismissed fullscreen content.");
                        mInterstitialAd = null;
                        requestNewInterstitial();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        // Called when ad fails to show.
                        Log.e(TAG, "Ad failed to show fullscreen content.");
                        mInterstitialAd = null;
                    }

                    @Override
                    public void onAdImpression() {
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "Ad recorded an impression.");
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when ad is shown.
                        Log.d(TAG, "Ad showed fullscreen content.");
                    }
                });
            }
        });

    }

    public String getPurchaseToken(Object singlePurchase){
        String purchaseToken = "";
        String[] testPurchase = singlePurchase.toString().replaceAll("\"","").split(",");
        for(String test : testPurchase){
            if(test.contains("purchaseToken")){
                purchaseToken = test.substring(14);
            }
        }
        return purchaseToken;
    }

    public void acknowlwdgePendingPurchase(String purchaseToken){
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build();
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                Log.i(TAG,"On Pending Purchases: Purchase Complete and Acknowledged ");
                editSharedPref(true);
                adFlag = sharedPref.getBoolean("adFlag",false);
                Log.i(TAG,"FLAG - Value set to true due to Pending Purchase and Acknowledge Complete: "+adFlag);
            }
        });
    }

}