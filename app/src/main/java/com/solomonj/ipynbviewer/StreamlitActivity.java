package com.solomonj.ipynbviewer;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StreamlitActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> uploadMessage;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private ProgressBar progressBar;
    Utilities utilities = new Utilities();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_streamlit);

        utilities.setupEdgeToEdgeWithConditionalPadding(this, findViewById(R.id.streamlitRoot), true, true, true, true);

        List<String> urls = Arrays.asList(
                "https://ipynbconverter.streamlit.app/",
                "https://nbtopdf-copy.streamlit.app/"
        );


        boolean dark = (getApplicationContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        Random random = new Random();
        String url = dark ? urls.get(random.nextInt(urls.size())) : "https://nbtopdf.streamlit.app/";

        progressBar = findViewById(R.id.progressBar);
        webView = (WebView) findViewById(R.id.webview);

        progresssBarDisplay("visible");
        webViewSettings();
        activityLauncherCode();
        fileSelection();
        download();
        webView.loadUrl(url);
    }

    public void webViewSettings(){
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // Force software rendering to bypass AdrenoVK / Vulkan shader compile errors on specific devices
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Configure Cookie Manager to accept cookies & third-party cookies (essential for API authorization)
        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Remove WebView signature from User-Agent to make it act as a standard mobile Chrome browser
        String originalUA = webView.getSettings().getUserAgentString();
        if (originalUA != null) {
            String cleanUA = originalUA.replace("Version/4.0 ", "").replace("wv", "");
            webView.getSettings().setUserAgentString(cleanUA);
        }

        webView.setWebViewClient(new WebViewClient(){
            public void onPageFinished(WebView view, String url) {
                progresssBarDisplay("gone");
            }
        });
    }

    public void activityLauncherCode(){
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    android.util.Log.d("STREAMLIT_UPLOAD", "Activity result received. ResultCode: " + result.getResultCode());
                    if (uploadMessage == null) {
                        android.util.Log.e("STREAMLIT_UPLOAD", "uploadMessage callback is null!");
                        return;
                    }

                    Uri[] results = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
                    if (results != null) {
                        for (Uri uri : results) {
                            android.util.Log.d("STREAMLIT_UPLOAD", "Selected URI: " + (uri != null ? uri.toString() : "null"));
                        }
                    } else {
                        android.util.Log.w("STREAMLIT_UPLOAD", "parseResult returned null URIs!");
                    }
                    uploadMessage.onReceiveValue(results);
                    uploadMessage = null;
                });
    }

    public void fileSelection(){
        webView.setWebChromeClient(new WebChromeClient() {
            // For Lollipop 5.0+ Devices
            @Override
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                android.util.Log.d("STREAMLIT_UPLOAD", "onShowFileChooser triggered");
                uploadMessage = filePathCallback;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    android.util.Log.d("STREAMLIT_UPLOAD", "Created chooser intent: " + intent.toString());
                    activityResultLauncher.launch(intent);
                } catch (Exception e) {
                    android.util.Log.e("STREAMLIT_UPLOAD", "Failed to launch intent from WebView params", e);
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                    return false;
                }
                return true;
            }

            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                android.util.Log.d("WEBVIEW_CONSOLE", consoleMessage.message() + " -- From line "
                        + consoleMessage.lineNumber() + " of "
                        + consoleMessage.sourceId());
                return true;
            }
        });
    }

    public void download(){
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                // Correct MIME type if necessary
                if (mimetype.equalsIgnoreCase("application/octet-stream") || mimetype.equalsIgnoreCase("bin")) {
                    mimetype = "application/pdf"; // Assuming the file is a PDF
                }
                request.setMimeType(mimetype);

                // Get cookies from WebView
                String cookies = CookieManager.getInstance().getCookie(url);

                // Add cookie and User-Agent to request
                request.addRequestHeader("Cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);

                // Use the DownloadManager to download the file
                request.setDescription("Downloading file...");
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                request.setTitle(fileName);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                // Specify the destination directory and file name
                String directoryPath = "IpynbViewer/" + fileName;
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOCUMENTS, directoryPath);

                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
            }
        });
    }

    public void progresssBarDisplay(String display){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(display.equalsIgnoreCase("visible")){
                    progressBar.setVisibility(View.VISIBLE);
                }else if(display.equalsIgnoreCase("gone")){
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}