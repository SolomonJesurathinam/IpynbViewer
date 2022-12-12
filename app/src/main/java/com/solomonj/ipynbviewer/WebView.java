package com.solomonj.ipynbviewer;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.widget.Button;

public class WebView extends AppCompatActivity {

    android.webkit.WebView webView;
    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private Uri uri;
    public String url ="file:///android_asset/ipynbviewer.html";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);


        webView = (android.webkit.WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(true);
        webView.setInitialScale(100);
        webView.loadUrl(url);

        webView.setWebViewClient(new xWebViewClient());
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
                        Log.d("DATA1",uri.getPath());
                        Log.d("DATA1",uri.getLastPathSegment());

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

        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private int _getScreenOrientation(){
        return getResources().getConfiguration().orientation;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int nCurrentOrientation = _getScreenOrientation();
        if (nCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            webView.setInitialScale(100);
            webView.getSettings().setUseWideViewPort(true);
        } else {
            webView.setInitialScale(100);
            webView.getSettings().setUseWideViewPort(false);
        }
    }

    public void createWebPrintJob(android.webkit.WebView webView, Context context, String filename) {
        // Get a PrintManager instance
        PrintManager printManager = (PrintManager) context
                .getSystemService(Context.PRINT_SERVICE);

        // Get a print adapter instance
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(filename);

        // Create a print job with name and adapter instance
        String jobName = context.getString(R.string.app_name) + " Document";
        printManager.print(jobName, printAdapter,
                new PrintAttributes.Builder().build());
    }

    public String getFilename(Uri uri){
        String fileName = null;
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        ContentResolver cr = getApplicationContext().getContentResolver();
        Cursor metaCursor = cr.query(uri, projection, null, null, null);
        if (metaCursor != null) {
            try {
                if (metaCursor.moveToFirst()) {
                    fileName = metaCursor.getString(0);
                }
            } finally {
                metaCursor.close();
            }
        }
        return fileName;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater findMenuItems = getMenuInflater();
        findMenuItems.inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.save:
                String fname = getFilename(uri);
                fname = fname.replace(".ipynb","");
                Log.d("DATA1",fname);
                createWebPrintJob(webView,WebView.this,fname);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}