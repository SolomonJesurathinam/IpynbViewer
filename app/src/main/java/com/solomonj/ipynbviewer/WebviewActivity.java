package com.solomonj.ipynbviewer;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.blankj.utilcode.util.PathUtils;
import com.webviewtopdf.PdfView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WebviewActivity extends AppCompatActivity {

    WebView webView;
    public ValueCallback<Uri[]> uploadMessage;
    private Uri uri;
    public String render1 ="file:///android_asset/Render1/ipynbviewer.html";
    public String render2 ="file:///android_asset/Render2/index.html";

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String PermissionDeniedCount = "PermissionDeniedCount";
    SharedPreferences sharedpreferences;
    int counter;
    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        //Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //Get render page intent from Home activity
        Intent intent = getIntent();
        String render = intent.getStringExtra("render");

        //Webview initilization and settings
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT); //fix for render1 loading time
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(true);
        webView.setInitialScale(100);
        if(render.equalsIgnoreCase("Render1")){
            webView.loadUrl(render1);
        }else if(render.equalsIgnoreCase("Render2")){
            webView.loadUrl(render2);
        }


        //File Intent to open file explorer for webview
        webView.setWebViewClient(new xWebViewClient());
        webView.setWebChromeClient(new WebChromeClient()
        {
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
                data = Intent.createChooser(data,"Select a Ipynb file");
                sActivityLauncher.launch(data);
                uploadMessage = filePathCallback;
                return true;
            }
        });


        //Shared Preferences initilization
        sharedpreferences = getSharedPreferences(MyPREFERENCES,Context.MODE_PRIVATE);
        counter = sharedpreferences.getInt("PermissionDeniedCount", 0);


        //Get Write storage permission for saving PDF
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            if(counter < 2){
                Toast.makeText(this, "Please grant storage permission to save pdf automatically", Toast.LENGTH_LONG).show();
            }

        }

    }


    //On getting the permission result - Counter increases on denied
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length >0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    if(counter<8){
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        counter++;
                        editor.putInt(PermissionDeniedCount,counter);
                        editor.commit();
                    }
                }
        }
    }


    //File picker for selecting ipynb file
    ActivityResultLauncher<Intent> sActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        uri = data.getData();
                        try {
                            uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), data));
                            uploadMessage = null;
                        }catch (NullPointerException e){
                            //Exception handling due to low memory issues in browser/webview
                            Toast.makeText(WebviewActivity.this, "Please re-select the file, activity restarted due to low memory", Toast.LENGTH_LONG).show();
                        }
                    }else if(result.getResultCode() == Activity.RESULT_CANCELED){
                        uploadMessage.onReceiveValue(null);
                    }

                }
            });


    //URL Overloading, blocks url in Webview
    private class xWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //view.loadUrl(url);
            return true;
        }
    }


    //Back button callback in webview
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
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


    //Default Print Job method for saving the pdf
    public void createWebPrintJob(android.webkit.WebView webView, Context context, String filename) {
        PrintManager printManager = (PrintManager) context
                .getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(filename);
        printManager.print(filename, printAdapter,
                new PrintAttributes.Builder().build());
    }


    //Method to retrieve file name of a Uri using MediaStore
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


    //Top menu configuration to add values
    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater findMenuItems = getMenuInflater();
        findMenuItems.inflate(R.menu.mainmenu, menu);

        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }


    //File directory based on SDK and third party code
    public File getDirectory(){
        File directory;
        if(Build.VERSION.SDK_INT>= 29){
            directory = new File(PathUtils.getExternalAppDocumentsPath().concat("/IpynbViewer/"));
            Log.d("DATA1",directory.toString());
        }else{
            directory = new File(PathUtils.getExternalDocumentsPath().concat("/IpynbViewer/"));
            Log.d("DATA1",directory.toString());
        }
        return directory;
    }


    //Menu items selected code
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

            case R.id.save:
                if(uri != null){
                    String fname = getFilename(uri);
                    if(fname.endsWith(".ipynb")){
                        fname = fname.replace(".ipynb","");
                        try{
                            saveAutomatically(fname);
                        }catch (Exception e){
                            dialog.dismiss();
                            Toast.makeText(WebviewActivity.this, "Failed to save pdf, opening default Print method", Toast.LENGTH_LONG).show();
                            createWebPrintJob(webView, WebviewActivity.this, fname);
                        }
                        return true;
                    }else{
                        Toast.makeText(this, "Please select correct ipynb file and save", Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(this, "Nothing to save, please select file", Toast.LENGTH_LONG).show();
                }

            case R.id.saveCustomise:
                if(uri != null){
                    String fname = getFilename(uri);
                    if(fname.endsWith(".ipynb")) {
                        fname = fname.replace(".ipynb", "");
                        createWebPrintJob(webView, WebviewActivity.this, fname);
                        return true;
                    }else{
                            Toast.makeText(this, "Please select correct ipynb file and save", Toast.LENGTH_LONG).show();
                        }
                }else{
                    Toast.makeText(this, "Nothing to save, please select file", Toast.LENGTH_LONG).show();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    //method for save
    public void saveAutomatically(String fname){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true); // if you want user to wait for some process to finish,
        builder.setView(R.layout.layout_loading_dialog);
        dialog = builder.create();
        dialog.show();

        PdfView.createWebPrintJob(WebviewActivity.this, webView, getDirectory(), fname+".pdf", new PdfView.Callback() {
            @Override
            public void success(String s) {
                if(Build.VERSION.SDK_INT>= 29){
                    File file = new File(getDirectory(), fname+".pdf");
                    if (file.exists()){
                        Log.d("DATA1","FILE EXISTS");
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
                            Toast.makeText(WebviewActivity.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(WebviewActivity.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                        }
                        file.delete();
                        Toast.makeText(WebviewActivity.this, "PDF Downloaded at Documents/IpynbViewer", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }else{
                        dialog.dismiss();
                        Toast.makeText(WebviewActivity.this,"Something happened, Please download again",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    dialog.dismiss();
                    Toast.makeText(WebviewActivity.this, "PDF Downloaded at Documents/IpynbViewer", Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void failure() {
                dialog.dismiss();
                Toast.makeText(WebviewActivity.this, "Storage access is denied, opening default Print method", Toast.LENGTH_LONG).show();
                createWebPrintJob(webView, WebviewActivity.this, fname);
            }
        });
    }




}