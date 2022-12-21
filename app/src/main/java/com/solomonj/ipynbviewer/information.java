package com.solomonj.ipynbviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class information extends AppCompatActivity {

    private Button experimentalbtn;
    String url = "https://ipynbconverter.streamlit.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        //Experimental button
        experimentalbtn = findViewById(R.id.experimentalbtn);
        experimentalbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    CustomTabsIntent.Builder customIntent = new CustomTabsIntent.Builder();
                    openCustomTab(information.this, customIntent.build(), Uri.parse(url));
                    Toast.makeText(information.this, "Opening external link in Chrome custom tab", Toast.LENGTH_SHORT).show();
                }catch (ActivityNotFoundException e){
                    e.printStackTrace();
                    Toast.makeText(information.this,"Chrome is not found, opening in default browser",Toast.LENGTH_SHORT).show();
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }

            }
        });
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
}