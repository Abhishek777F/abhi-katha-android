package com.abhi.katha;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.JavascriptInterface;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://abhishek777f.github.io/katha/";
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int PAGE_BACKGROUND = Color.rgb(245, 239, 249);
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private FrameLayout root;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PAGE_BACKGROUND);
        getWindow().setNavigationBarColor(PAGE_BACKGROUND);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        root = new FrameLayout(this);
        webView = new WebView(this);
        webView.setBackgroundColor(PAGE_BACKGROUND);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return routeUrl(request.getUrl().toString());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeUrl(url);
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                suppressInstallPromotion();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) Toast.makeText(MainActivity.this, "Unable to load page. Check your internet connection and try again.", Toast.LENGTH_LONG).show();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                final WebView popup = new WebView(MainActivity.this);
                WebSettings popupSettings = popup.getSettings();
                popupSettings.setJavaScriptEnabled(true);
                popupSettings.setDomStorageEnabled(true);
                popup.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView child, WebResourceRequest request) {
                        handlePopupUrl(request.getUrl().toString(), popup);
                        return true;
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView child, String url) {
                        handlePopupUrl(url, popup);
                        return true;
                    }
                });
                root.addView(popup, new FrameLayout.LayoutParams(1, 1));
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }

            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "No file picker available.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) request.addRequestHeader("Cookie", cookie);
                request.addRequestHeader("User-Agent", userAgent);
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.setDescription("Downloading from Abhi Katha");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Download could not be started.", Toast.LENGTH_LONG).show();
            }
        });

        webView.addJavascriptInterface(new WelcomeBridge(), "AndroidWelcome");
        if (savedInstanceState == null) {
            hideSystemBarsForWelcome();
            loadWelcomeScreen();
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void hideSystemBarsForWelcome() {
        getWindow().setStatusBarColor(Color.rgb(4, 2, 26));
        getWindow().setNavigationBarColor(Color.rgb(4, 2, 26));
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private void restoreSystemBarsAfterWelcome() {
        runOnUiThread(() -> {
            getWindow().setStatusBarColor(PAGE_BACKGROUND);
            getWindow().setNavigationBarColor(PAGE_BACKGROUND);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        });
    }

    private final class WelcomeBridge {
        @JavascriptInterface
        public void onDone() {
            restoreSystemBarsAfterWelcome();
        }
    }

    private void loadWelcomeScreen() {
        try (java.io.InputStream input = getAssets().open("welcome-splash.html");
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            String splashHtml = new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            webView.loadDataWithBaseURL(HOME_URL, splashHtml, "text/html", "UTF-8", null);
        } catch (Exception error) {
            webView.loadUrl(HOME_URL);
        }
    }

    private void suppressInstallPromotion() {
        String script = "(function(){"
            + "function hideNativeInstallCard(){"
            + "var nodes=document.querySelectorAll('body *');"
            + "for(var i=0;i<nodes.length;i++){"
            + "var el=nodes[i];"
            + "if(el.children.length>0)continue;"
            + "var txt=(el.textContent||'').trim().toLowerCase();"
            + "if(txt!=='your khata, always within reach.')continue;"
            + "var p=el;"
            + "for(var j=0;j<7&&p&&p!==document.body;j++,p=p.parentElement){"
            + "var t=(p.innerText||'').toLowerCase();"
            + "if(t.indexOf('install app')!==-1&&t.indexOf('my katha')!==-1){"
            + "p.style.setProperty('display','none','important');"
            + "p.setAttribute('data-abhi-katha-install-hidden','true');"
            + "break;"
            + "}"
            + "}"
            + "}"
            + "}"
            + "hideNativeInstallCard();"
            + "if(!window.__abhiKathaInstallObserver){"
            + "window.__abhiKathaInstallObserver=new MutationObserver(hideNativeInstallCard);"
            + "window.__abhiKathaInstallObserver.observe(document.documentElement,{childList:true,subtree:true});"
            + "}"
            + "})();";
        webView.evaluateJavascript(script, null);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void handlePopupUrl(String value, WebView popup) {
        if (value == null || value.equals("about:blank")) return;
        if (!routeUrl(value) && webView != null) webView.loadUrl(value);
        root.removeView(popup);
        popup.stopLoading();
        popup.destroy();
    }

    private boolean openWhatsApp(Uri uri) {
        String[] packages = {"com.whatsapp", "com.whatsapp.w4b"};
        for (String packageName : packages) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage(packageName);
                startActivity(intent);
                return true;
            } catch (ActivityNotFoundException ignored) { }
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
            return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "WhatsApp is not installed. Please install WhatsApp and try again.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private boolean routeUrl(String value) {
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();

        if (scheme.equals("whatsapp")) return openWhatsApp(uri);

        if (scheme.equals("intent")) {
            try {
                Intent intent = Intent.parseUri(value, Intent.URI_INTENT_SCHEME);
                String fallback = intent.getStringExtra("browser_fallback_url");
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    if (fallback != null && !fallback.isEmpty()) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallback)));
                    } else {
                        Toast.makeText(this, "No app found to open this link.", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "This link could not be opened.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (scheme.equals("https") || scheme.equals("http")) {
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if (host.equals("wa.me") || host.equals("api.whatsapp.com") || host.equals("web.whatsapp.com")) {
                return openWhatsApp(uri);
            }
            if (host.equals("abhishek777f.github.io") || host.endsWith(".github.io")) return false;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No app found to open this link.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (scheme.equals("about") || scheme.equals("blob") || scheme.equals("data")) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to open this link.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (webView != null) { webView.stopLoading(); webView.setWebChromeClient(null); webView.setWebViewClient(null); webView.destroy(); }
        super.onDestroy();
    }
}