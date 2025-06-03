package com.solomonj.ipynbviewer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private BillingClient billingClient;
    private static final String TAG = MainActivity.class.getName();
    public static final String MyPRE = "adFlagger" ;
    SharedPreferences sharedPref;
    SharedPreferences homePagePref;
    boolean adFlag;
    private AdView mAdView;
    Button getPro;

    //new ui changes
    RadioGroup radioRender;
    Button choosefile, convertOnline;
    private ActivityResultLauncher<String[]> mGetContent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getSharedPreferences(MyPRE, Context.MODE_PRIVATE);
        adFlag = sharedPref.getBoolean("adFlag",false);
        Log.i(TAG, "Flag - Default value from Shared Preferences: "+adFlag);

        //new UI changes
        homePagePref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        homePagePref.getString("renderKey","Real"); //set default radio

        //Adview with flag condition
        if(adFlag == false){
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                }
            });
            requestNewBanner();
        }else{
            Log.i(TAG,"AdFlag is "+adFlag+" so the ads are not loaded");
        }

        //new changes - 1/15/2023
        getPro = findViewById(R.id.getPro);
        getPro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater =(LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
                View popupView = inflater.inflate(R.layout.popup_get_pro, null);

                //Create Popup window
                PopupWindow popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, true);
                popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                //Handle Button
                Button upgrade = popupView.findViewById(R.id.upgradePro);
                Button notNow = popupView.findViewById(R.id.notNow);

                upgrade.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse("market://details?id=com.solomonj.ipynbviewerpro"));
                        try{
                            startActivity(goToMarket);
                        } catch(ActivityNotFoundException e){
                            Toast.makeText(getApplicationContext(), "Unable to find market app \nSearch Ipynb Viewer Pro in Playstore",Toast.LENGTH_LONG).show();
                        }
                    }
                });

                notNow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });
                popupWindow.showAtLocation(findViewById(R.id.homePage), Gravity.CENTER,0,0);
            }
        });

        //new UI Changes
        radioRender = findViewById(R.id.radioRender);
        radiobuttonLogic();

        choosefile = findViewById(R.id.choosefile);
        fileHandling();

        convertOnline = findViewById(R.id.convertOnline);
        convertOnline.setOnClickListener(v->{
            Intent intent = new Intent(getApplicationContext(),Online.class);
            startActivity(intent);
        });

    }

    //new UI Changes
    private void radiobuttonLogic(){
        if(homePagePref.getString("renderKey","Real").equalsIgnoreCase("Real")){
            radioRender.check(R.id.radioReal);
        }else if(homePagePref.getString("renderKey","Real").equalsIgnoreCase("Basic")){
            radioRender.check(R.id.radioBasic);
        }
        radioRender.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = homePagePref.edit();
                if(checkedId == R.id.radioReal){
                    editor.putString("renderKey","Real");
                }else if(checkedId == R.id.radioBasic){
                    editor.putString("renderKey","Basic");
                }
                editor.apply();
                editor.commit();
            }
        });
    }

    public void fileHandling(){
        mGetContent = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                new androidx.activity.result.ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if(uri != null){
                            String fileName = getFilename(getApplicationContext(),uri);
                            if (fileName != null && fileName.endsWith(".ipynb")) {
                                getContentResolver().takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                );
                                //Toast.makeText(getApplicationContext(),uri.getPath(),Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(getApplicationContext(), Webview.class);
                                intent.putExtra("filePath",uri.toString());
                                startActivity(intent);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_up, R.anim.stay);
                                } else {
                                    overridePendingTransition(R.anim.slide_up, R.anim.stay);
                                }
                            } else {
                                Toast.makeText(getApplicationContext(),"Only ipynb files are allowed",Toast.LENGTH_LONG).show();
                            }

                        }
                    }
                });

        choosefile.setOnClickListener(v->{
            mGetContent.launch(new String[]{"application/*"});
        });
    }

    public String getFilename(Context context, Uri uri){
        String fileName = null;
        if(uri.getScheme().equalsIgnoreCase("content")){
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (fileName == null) {
                fileName = uri.getLastPathSegment();
            }
        }else if(uri.getScheme().equalsIgnoreCase("file")){
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    //UI changes end

//    Billing Listener for previous and current purchases
    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
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
                }
            }
        });
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
                                            editSharedPref(true,"FLAG - Value set to true due to Previous Purchase and Acknowledge Complete");
                                            adFlag = sharedPref.getBoolean("adFlag",false);
                                            Log.i(TAG,"FLAG - Previous Purchase and Acknowledge Complete Latest value: "+adFlag);
                                        }
                                    }else{
                                        Log.e(TAG,"Purchase State is not 0");
                                    }
                                }else{
                                    Log.i(TAG,"Purchase History: Have not purchased com.solomonj.ipynbview.ads");
                                    editSharedPref(false,"FLAG - Value set to false as there are no purchases for com.solomonj.ipynbview.ads");
                                    adFlag = sharedPref.getBoolean("adFlag",false);
                                    Log.i(TAG,"FLAG - No purchases for com.solomonj.ipynbview.ads Latest value: "+adFlag);
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
    private void editSharedPref(boolean flagValue, String message){
        Boolean checkFlag = sharedPref.getBoolean("adFlag",false);
        if(flagValue != checkFlag){
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("adFlag",flagValue);
            editor.apply();
            editor.commit();
            Log.w(TAG,message);
        }else{
            Log.w(TAG,"Flag is already "+flagValue+" so not editing");
        }
    }

    //Rectangle Banner Ad
    private void requestNewBanner(){
        //Original id unit - ca-app-pub-4449150732190604/3922551687
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }


    //method to retrieve purchase token to acknowlwdge
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


    //method to acknowlwdge purchase in purchase history
    public void acknowlwdgePendingPurchase(String purchaseToken){
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build();
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                Log.i(TAG,"On Pending Purchases: Purchase Complete and Acknowledged ");
                editSharedPref(true,"FLAG - Value set to true due to Pending Purchase and Acknowledge Complete");
                adFlag = sharedPref.getBoolean("adFlag",false);
                Log.i(TAG,"FLAG - Pending Purchase and Acknowledge Complete Latest Value: "+adFlag);
            }
        });
    }

}