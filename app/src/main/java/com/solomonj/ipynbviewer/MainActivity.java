package com.solomonj.ipynbviewer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //initialise button and spinner
        button = findViewById(R.id.getStarted);
        spinner = findViewById(R.id.selectRender);


        //Image to remove ads with on click
        removeAds = findViewById(R.id.removeads);
        removeAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makePurchase();
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
                Log.i(TAG, "onPurchasesUpdated: Purchase Canceled");
                billingClient.endConnection();
                Log.i(TAG,"On Connection Status: Terminated");
            } else {
                Log.i(TAG, "onPurchasesUpdated: Error");
                billingClient.endConnection();
                Log.i(TAG,"On Connection Status: Terminated");
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
            Log.i(TAG,"On Purchases: Purchase Complete "+purchase.getProducts().toString());
        }
    }

    //Get Purchase History
    private void purchasesHistory(){
        billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(
                        BillingClient.ProductType.INAPP).build(),
                new PurchasesResponseListener() {
                    public void onQueryPurchasesResponse(BillingResult billingResult, List purchases) {
                        Log.d(TAG,"Purchase History "+purchases.toString());
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
        Log.i(TAG,"On Connection Status: Terminated");
    }

}