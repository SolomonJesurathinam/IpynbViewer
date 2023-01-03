package com.solomonj.ipynbviewer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemSelectedListener {

    private Button button;
    private Spinner spinner;
    private String[] dropValues;
    private ImageView infologo;
    private String render;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.getStarted);
        spinner = findViewById(R.id.selectRender);

        //Render dropdown
        dropValues = getResources().getStringArray(R.array.dropdown);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, dropValues);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        //Get started button click
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), WebviewActivity.class);
                intent.putExtra("render",render);
                startActivity(intent);
            }
        });

        //info logo listener
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


    //Billing
    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {
            //To be implemented in a later section
        }
    };

    private BillingClient billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build();

    private void connectToGooglePlayBilling(){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                connectToGooglePlayBilling();
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if(billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK){
                    getProducctDetails();
                }

            }
        });
    }

    private void getProducctDetails(){
        List<String> products = new ArrayList<String>();


    }
}