package com.solomonj.ipynbviewer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_SCREEN_TIME_OUT;
    static {
        if (android.os.Build.VERSION.SDK_INT >= 36) {
            SPLASH_SCREEN_TIME_OUT = 1500;
        } else {
            SPLASH_SCREEN_TIME_OUT = 3000;
        }
    }
    private Handler handler = new Handler(Looper.getMainLooper());
    private InterstitialAd mInterstitialAd;
    private boolean isAdLoaded = false;
    boolean adFlag;
    SharedPreferences sharedPref;
    public static final String MyPRE = "adFlagger" ;
    Utilities utilities = new Utilities();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen.installSplashScreen(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        utilities.setupEdgeToEdgeWithConditionalPadding(this, findViewById(R.id.splashRoot), true, true, true, true);

        sharedPref = getSharedPreferences(MyPRE, Context.MODE_PRIVATE);
        adFlag = sharedPref.getBoolean("adFlag",false);
        Log.e("adFlag",String.valueOf(adFlag));

        if(!adFlag){
            loadInterstitialAd();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdLoaded) {
                mInterstitialAd.show(SplashActivity.this);
            } else {
                splashScreenLogic();
            }
        }, SPLASH_SCREEN_TIME_OUT);

    }

    public void splashScreenLogic(){
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            Uri fileUri = getIntent().getData();
            if (fileUri != null) {
                if (!isFinishing()) {
                    Log.e("TESTINGG",fileUri.getScheme().toString());
                    try {
                        getContentResolver().takePersistableUriPermission(
                                fileUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException e) {
                        // Ignore cases where the URI is not persistable
                    }
                    // Start WebActivity and pass the file URI
                    Intent webIntent = new Intent(this, Webview.class);
                    webIntent.setData(fileUri);
                    webIntent.putExtra("filePath", fileUri.toString());
                    webIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(webIntent);
                    finish(); // Close the SplashActivity
                }
            }
        }else{
            if (!isFinishing()) { // Add this check
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
                finish();
            }
        }
    }

    private boolean hasPersistableUriPermission(Uri uri) {
        List<UriPermission> uriPermissions = getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : uriPermissions) {
            if (permission.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // This will cancel the scheduled Runnable
    }

    private void loadInterstitialAd(){
        AdRequest adRequest = new AdRequest.Builder().build();
        //DUMMY - ca-app-pub-3940256099942544/1033173712
        InterstitialAd.load(this, "ca-app-pub-1715775523919691/7766223033", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                isAdLoaded = true;

                // Set callback to continue action after ad is closed
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        splashScreenLogic();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                        splashScreenLogic();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                isAdLoaded = false;
                splashScreenLogic();
            }
        });
    }
}