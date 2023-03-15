package com.solomonj.ipynbviewer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;


public class original_NB extends AppCompatActivity {

    Button experimentalButton;
    String url = "https://ipynbconverter.streamlit.app/";
    SharedPreferences originalShared;
    boolean adFlag;
    private static final String TAG = MainActivity.class.getName();
    private InterstitialAd mOrgInterstitialAd;
    TextView addText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_original_nb);

        experimentalButton = findViewById(R.id.experimentalButton);

        //button for starting nb convert
        experimentalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mOrgInterstitialAd != null){
                    mOrgInterstitialAd.show(original_NB.this);
                    mOrgInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
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
                            mOrgInterstitialAd = null;

                            //opening browser
                            openChrome();

                            //load ad
                            requestNewInterstitialOriginal();


                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            // Called when ad fails to show.
                            Log.e(TAG, "Ad failed to show fullscreen content.");
                            mOrgInterstitialAd = null;

                            //opening browser
                            openChrome();
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

                }else{
                    //opening browser
                    openChrome();
                }
            }
        });


        //get the add fag
        originalShared = getSharedPreferences("adFlagger", Context.MODE_PRIVATE);
        adFlag = originalShared.getBoolean("adFlag",false);

        //text for full screen ad
        addText = findViewById(R.id.addText);

        //Adview with flag condition
        if(adFlag == false){
            requestNewInterstitialOriginal();
            addText.setVisibility(View.VISIBLE);
        }else{
            Log.i(TAG,"AdFlag is "+adFlag+" so the ads are not loaded");
        }

    }

    //Chrome custom tab
    public static void openCustomTab(Activity activity, CustomTabsIntent customTabsIntent, Uri uri) {
        String packageName = "com.android.chrome";
        if (packageName != null) {
            customTabsIntent.intent.setPackage(packageName);
            customTabsIntent.launchUrl(activity, uri);
        } else {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }


    //Interestial ad
    private void requestNewInterstitialOriginal() {
        AdRequest adRequest = new AdRequest.Builder().build();

        //Interestial Ad
        //Test id unit -ca-app-pub-3940256099942544/1033173712
        InterstitialAd.load(this, "ca-app-pub-1715775523919691/7766223033", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.d(TAG, loadAdError.toString());
                mOrgInterstitialAd = null;
            }

            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                super.onAdLoaded(interstitialAd);
                mOrgInterstitialAd = interstitialAd;
                Log.i(TAG, "onAdLoaded");

            }
        });
    }

    public void openChrome(){
        try {
            CustomTabsIntent.Builder customIntent = new CustomTabsIntent.Builder();
            openCustomTab(original_NB.this, customIntent.build(), Uri.parse(url));
            Toast.makeText(original_NB.this, "Opening NB Convert tool in Chrome custom tab", Toast.LENGTH_LONG).show();
        }catch (ActivityNotFoundException e){
            e.printStackTrace();
            Toast.makeText(original_NB.this,"Chrome is not found, Opening NB Convert tool in default browser",Toast.LENGTH_LONG).show();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        }
    }

}