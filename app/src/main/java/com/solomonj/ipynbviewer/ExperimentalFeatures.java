package com.solomonj.ipynbviewer;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

public class ExperimentalFeatures extends AppCompatActivity {

    android.webkit.WebView webView;
    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private Uri uri;
    public String url ="https://ipynbconverter.streamlit.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experimental_features);

        webView = (android.webkit.WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDisplayZoomControls(true);
        webView.setInitialScale(130);
        webView.loadUrl(url);

        webView.setWebViewClient(new ExperimentalFeatures.xWebViewClient());
        webView.setWebChromeClient(new WebChromeClient()
        {

            // For Lollipop 5.0+ Devices
            public boolean onShowFileChooser(android.webkit.WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams)
            {
                Intent data;
                if (Build.VERSION.SDK_INT >= 19) {
                    data = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                }
                else {
                    data = new Intent(Intent.ACTION_GET_CONTENT);
                }
                data.setType("*/*");
                data = Intent.createChooser(data,"Choose a file");
                sActivityLauncher.launch(data);
                uploadMessage = filePathCallback;
                return true;
            }

        });

    }

    //File picker
    ActivityResultLauncher<Intent> sActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        uri = data.getData();
                        uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), data));
                        uploadMessage = null;

                    }else if(result.getResultCode() == Activity.RESULT_CANCELED){
                        uploadMessage.onReceiveValue(null);
                    }

                }
            });

    private class xWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(android.webkit.WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK) ) {
            finish(); // finish activity
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    }
