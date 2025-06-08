package com.solomonj.ipynbviewer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.blankj.utilcode.util.PathUtils;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Webview extends AppCompatActivity {

    WebView webView;
    SharedPreferences webPref;
    private ProgressBar progressBar;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private GestureDetector mGestureDetector;
    private AdView viewPageAd;
    SharedPreferences sharedPref;
    public static final String MyPRE = "adFlagger" ;
    boolean adFlag;
    LinearLayout pdfDownload,printPDF,darkmode,viewOverlay;
    boolean isDarkMode = false;
    private boolean isOverlayVisible = true;
    Utilities utilities = new Utilities();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_webview);

        utilities.setupEdgeToEdgeWithConditionalPadding(this, findViewById(R.id.webviewRoot), true, true, true, true);

        sharedPref = getSharedPreferences(MyPRE, Context.MODE_PRIVATE);
        adFlag = sharedPref.getBoolean("adFlag",false);

        //Shared Preferences
        webPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //Locators
        progressBar = findViewById(R.id.progressBar);
        webView = (WebView) findViewById(R.id.webView);

        //Functions
        displayProgressBar();
        extractDataAndDisplay();
        backPressedLogic();

        //hide or show toolbar
        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (isOverlayVisible) {
                hidefootBar();
            }
        });

        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleOverlayVisibility();
                return true;
            }
        });

        loadBannerAd();

        pdfDownload = findViewById(R.id.pdfDownload);
        printPDF = findViewById(R.id.printPDF);
        darkmode = findViewById(R.id.Darkmode);
        viewOverlay = findViewById(R.id.viewOverlay);

        pdfDownload.setOnClickListener(v->{
            if(Build.VERSION.SDK_INT<29){
                if(ContextCompat.checkSelfPermission(Webview.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(Webview.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }else{
                    downloadSteps();
                }
            }else {
                downloadSteps();
            }
        });

        printPDF.setOnClickListener(v->{
            Uri uri = Uri.parse(getIntent().getStringExtra("filePath"));
            if(uri != null){
                String fname = getFilename(getApplicationContext(),uri);
                if(fname.endsWith(".ipynb")){
                    fname = fname.replace(".ipynb","");
                    createWebPrintJob(webView,Webview.this,fname);
                }
            }else{
                Toast.makeText(this, "Nothing to save, please re-select file", Toast.LENGTH_LONG).show();
            }
        });

        darkmode.setOnClickListener(v->{
            if(isDarkMode){
                removeDarkmode();
            }else{
                setDarkmodeMode();
            }

        });
    }

    private void removeDarkmode(){
        String disableDarkMode = "(function() {" +
                "let style = document.getElementById('dark-mode-style');" +
                "if (style) style.remove();" +
                "})();";
        webView.evaluateJavascript(disableDarkMode, null);
        isDarkMode = false;
    }

    private void setDarkmodeMode(){
        String darkModeCss = "(function() {" +
                "let style = document.getElementById('dark-mode-style');" +
                "if (!style) {" +
                "style = document.createElement('style');" +
                "style.id = 'dark-mode-style';" +
                "style.innerHTML = `" +

                "html, body {" +
                "  background-color: #121212 !important;" +
                "  color: #e0e0e0 !important;" +
                "}" +

                "div, span, td, th, pre, code, .output_area, .input_area, .cell, .output, .output_subarea {" +
                "  background-color: #1e1e1e !important;" +
                "  color: #e0e0e0 !important;" +
                "}" +

                "a { color: #8ab4f8 !important; }" +
                "img, svg { filter: invert(1) hue-rotate(180deg); }" +

                "table { background-color: #1f1f1f !important; color: #e0e0e0 !important; }" +
                "tr, td, th { border-color: #444 !important; }" +

                "`;" +
                "document.head.appendChild(style);" +
                "}" +
                "})();";

        webView.evaluateJavascript(darkModeCss, null);
        isDarkMode = true;
    }

    private void toggleOverlayVisibility() {
        runOnUiThread(()->{
            if (isOverlayVisible) {
                viewOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> viewOverlay.setVisibility(View.GONE));
                isOverlayVisible = false;
            } else {
                viewOverlay.setVisibility(View.VISIBLE);
                viewOverlay.animate().alpha(1f).setDuration(200);
                isOverlayVisible = true;
            }
        });
    }

    private boolean isTouchInsideView(MotionEvent event, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float x = event.getRawX();
        float y = event.getRawY();
        return x >= location[0] && x <= (location[0] + view.getWidth()) &&
                y >= location[1] && y <= (location[1] + view.getHeight());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
//        mGestureDetector.onTouchEvent(e);
        if (isTouchInsideView(e, webView) && !isTouchInsideView(e, viewOverlay)) {
            mGestureDetector.onTouchEvent(e);
        }
        return super.dispatchTouchEvent(e); // allow WebView to scroll & interact

    }

    private void hidefootBar() {
        viewOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> viewOverlay.setVisibility(View.GONE));
        isOverlayVisible = false;
    }

    public void displayProgressBar(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    public void backPressedLogic(){
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                closeActivityWithTransition();
            }
        };
    }

    private void closeActivityWithTransition() {
        clearActiveUriState();
        finish();
    }

    //extract data
    public void extractDataAndDisplay(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri uri = Uri.parse(getIntent().getStringExtra("filePath"));
                String fileContent = null;
                try {
                    fileContent = readFileContent(uri);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String finalFileContent = fileContent;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(finalFileContent != null){
                            progressBar.setVisibility(View.GONE);
                            openWebview(finalFileContent);
                        }
                    }
                });
            }
        }).start();
    }

    //open webview
    public void openWebview(String data){
        String render1 ="file:///android_asset/Render1/ipynbviewer.html";
        String render2 ="file:///android_asset/Render2/index.html";
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); //fix for render1 loading time
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        if(getApplication().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            webView.setInitialScale(100);
            webView.getSettings().setUseWideViewPort(false);
        }else if(getApplication().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            webView.setInitialScale(100);
            webView.getSettings().setUseWideViewPort(true);
        }
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidMessage");
        webView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                // Call JavaScript function here
                sendDataToWebView(webView,data);
            }

            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                //view.loadUrl(url);
                return true;
            }
        });

        if(webPref.getString("renderKey","Real").equalsIgnoreCase("Real")){
            webView.loadUrl(render1);
        }
        else if(webPref.getString("renderKey","Real").equalsIgnoreCase("Basic")){
            webView.loadUrl(render2);
        }
    }

    //data in chunks
    private void sendTextDataToWebView(WebView webView1, String data, int chunkSize) {
        for (int i = 0; i < data.length(); i += chunkSize) {
            final String chunk = data.substring(i, Math.min(data.length(), i + chunkSize));
            webView1.post(() -> webView1.evaluateJavascript("javascript:addDataChunk(" + JSONObject.quote(chunk) + ")", null));
        }
        webView1.post(() -> webView1.evaluateJavascript("javascript:processData()", null));
    }

    //send chunks data to webview
    private void sendDataToWebView(WebView webView, String data) {
        int chunkSize = 4000; // Adjust this size as needed
        sendTextDataToWebView(webView, data, chunkSize);
    }

    //read data before splitting to chunks
    private String readFileContent(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        inputStream.close();
        return stringBuilder.toString();
    }

    //URL Overloading, blocks url in Webview
    private class xWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //view.loadUrl(url);
            return true;
        }
    }

    //onDestory close all webview and clear
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeAllViews();
            webView.destroy();
        }
        clearActiveUriState();
        super.onDestroy();
    }


    public void downloadSteps(){
        Uri uri = Uri.parse(getIntent().getStringExtra("filePath"));
        if(uri != null){
            String fname = getFilename(getApplicationContext(),uri);
            if(fname.endsWith(".ipynb")){
                fname = fname.replace(".ipynb","");
                try{
                    saveAutomatically(fname);
                }catch(Exception e){
                    setProgressBar("Gone");
                    Toast.makeText(Webview.this, "Failed to save pdf, opening default Print method", Toast.LENGTH_LONG).show();
                    Log.e("TESTINGG",e.toString());
                    createWebPrintJob(webView, Webview.this, fname);
                }
            }
        }
    }

    //Default Print Job method for saving the pdf
    public void createWebPrintJob(android.webkit.WebView webView, Context context, String filename) {
        PrintManager printManager = (PrintManager) context
                .getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(filename);
        printManager.print(filename, printAdapter,
                new PrintAttributes.Builder().build());
    }

    //File name from uri
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
            if (fileName == null || fileName.contains("/")) {
                String path = uri.getPath();
                if (path != null) {
                    int cut = path.lastIndexOf('/');
                    if (cut != -1) {
                        fileName = path.substring(cut + 1);
                    }
                }
            }
        }else if(uri.getScheme().equalsIgnoreCase("file")){
            fileName = uri.getLastPathSegment();
        }
        //checking if the url is encoded
        try{
            fileName = checkURLEncoded(fileName);
        }catch(Exception e){
            e.printStackTrace();
        }
        return fileName;
    }

    public String checkURLEncoded(String input){
        boolean isEncoded = input.contains("%");
        if(isEncoded){
            try{
                String decodedFilePath = URLDecoder.decode(input.replaceAll("%25", "%"), StandardCharsets.UTF_8.toString());
                String[] pathSegments = decodedFilePath.split("/");
                return pathSegments[pathSegments.length - 1];
            }catch(UnsupportedEncodingException e){
                return input;
            }
        }else{
            return input;
        }
    }

    //Save Automatically (Download logic)
    public void saveAutomatically(String fname) {
        setProgressBar("Visible");
        PdfView.createWebPrintJob(Webview.this, webView, getDirectory(), fname + ".pdf", new PdfView.Callback() {
            @Override
            public void success(String s) {
                if(Build.VERSION.SDK_INT>= 29){ //Targetting Android 10 and above
                    File file = new File(getDirectory(), fname+".pdf");
                    if (file.exists()){
                        ContentResolver resolver = getApplicationContext().getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fname);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + File.separator + "IpynbViewer");

                        Uri uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
                        try {
                            InputStream inputStream = new FileInputStream(file);
                            OutputStream outputStream = getContentResolver().openOutputStream(uri);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                FileUtils.copy(inputStream,outputStream);
                                inputStream.close();
                                outputStream.close();
                            }
                        } catch (FileNotFoundException e) {
                            Toast.makeText(Webview.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                            setProgressBar("Gone");
                        } catch (IOException e) {
                            Toast.makeText(Webview.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                            setProgressBar("Gone");
                        }
                        file.delete();
                        Toast.makeText(Webview.this, "PDF Downloaded at Documents/IpynbViewer", Toast.LENGTH_LONG).show();
                        setProgressBar("Gone");
                    }else{
                        Toast.makeText(Webview.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                        setProgressBar("Gone");
                    }
                }else{//android 9
                    Toast.makeText(Webview.this, "PDF Downloaded at Documents/IpynbViewer", Toast.LENGTH_LONG).show();
                    setProgressBar("Gone");
                }
            }

            @Override
            public void failure() {
                Toast.makeText(Webview.this, "Failed to save pdf, opening default Print method", Toast.LENGTH_LONG).show();
                setProgressBar("Gone");
                createWebPrintJob(webView, Webview.this, fname);
            }
        });
    }

    //Dialog box
    private void showPermissionSettingsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Allow Permission Manually")
                .setMessage("Need Storage Permission to download pdf to devices, please allow manually from settings.")
                .setPositiveButton("App Settings", (dialogInterface, which) -> {
                    // Intent to open app settings
                    openAppSettings();
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                .create();
        dialog.setOnShowListener(dialogInterface -> {
            // Change the positive button color
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.black));

            // Change the negative button color
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.black));
        });
        dialog.show();
    }

    //Open settings to provide manual storage access
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    //handling permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    downloadSteps();
                } else {
                    // Permission denied, show a Toast
                    Toast.makeText(this, "Storage access is required to download files automatically", Toast.LENGTH_SHORT).show();
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        showPermissionSettingsDialog();
                    }
                }
                return;
            }
            // Other 'case' lines to check for other permissions this app might request
        }
    }

    //Get file directory for downloading in all android versions
    public File getDirectory(){
        File directory;
        if(Build.VERSION.SDK_INT>= 29){
            directory = new File(PathUtils.getExternalAppDocumentsPath().concat("/IpynbViewer/"));
        }else{
            directory = new File(PathUtils.getExternalDocumentsPath().concat("/IpynbViewer/"));
        }
        return directory;
    }

    //progress bar hide/show
    public void setProgressBar(String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(status.equalsIgnoreCase("Visible")){
                    progressBar.setVisibility(View.VISIBLE);
                    pdfDownload.setEnabled(false);
                    printPDF.setEnabled(false);
                }else if(status.equalsIgnoreCase("Gone")){
                    progressBar.setVisibility(View.GONE);
                    pdfDownload.setEnabled(true);
                    printPDF.setEnabled(true);
                }

            }
        });
    }

    private void clearActiveUriState() {
        List<UriPermission> uriPermissions = getContentResolver().getPersistedUriPermissions();
        Log.d("URI Permissions", "Before clearing: " + uriPermissions.size());

        // Get the set of saved URIs
        Set<Uri> savedUris = getSavedUris();

        // Retrieve the additional Uri
        String treeUriString = webPref.getString("treeUri", null);
        Uri treeUri = treeUriString != null ? Uri.parse(treeUriString) : null;

        for (UriPermission permission : uriPermissions) {
            Log.e("URI Permissions", permission.toString());
            Log.e("URI Permissions", permission.getUri().toString());

            // Check if the current URI is in the saved URIs set or it is the treeUri
            if (!savedUris.contains(permission.getUri()) && !permission.getUri().equals(treeUri)) {
                getContentResolver().releasePersistableUriPermission(
                        permission.getUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
            } else {
                Log.e("URI Permissions", "Retaining permission for: " + permission.getUri().toString());
            }
        }

        List<UriPermission> uriPermissionsAfter = getContentResolver().getPersistedUriPermissions();
        Log.d("URI Permissions", "After clearing: " + uriPermissionsAfter.size());
    }

    private Set<Uri> getSavedUris() {
        // Retrieve the set of URI strings from SharedPreferences
        Set<String> uriStrings = webPref.getStringSet("selectedUris", new HashSet<>());

        // Create a new set to store the converted URIs
        Set<Uri> savedUris = new HashSet<>();

        // Iterate over the string set and convert each string to a Uri
        for (String uriString : uriStrings) {
            savedUris.add(Uri.parse(uriString));
        }

        return savedUris;
    }

    //Orientation Change configuration to initial scales --> Orientation Save state is handled in Manifest file
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            webView.setInitialScale(100);
            webView.getSettings().setUseWideViewPort(true);
        } else {
            webView.setInitialScale(100);
            webView.getSettings().setUseWideViewPort(false);
        }
    }

    private void loadBannerAd(){
        Log.e("Adflag",String.valueOf(adFlag));
        if(!adFlag){
            MobileAds.initialize(this, initializationStatus -> {});
            viewPageAd = findViewById(R.id.viewPageAd);
            AdRequest adRequest = new AdRequest.Builder().build();
            viewPageAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    viewPageAd.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                }
            });

            viewPageAd.loadAd(adRequest);
        }
    }
}