package com.abhi.katha;

import android.app.Activity;
import android.app.DownloadManager;
import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://abhishek777f.github.io/katha/";
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int PAGE_BACKGROUND = Color.rgb(245, 239, 249);
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());
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
        settings.setSupportMultipleWindows(false);
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

        if (savedInstanceState == null) webView.loadUrl(HOME_URL); else webView.restoreState(savedInstanceState);
        showWelcomeAnimation();
    }

    private void showWelcomeAnimation() {
        final FrameLayout splash = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(5, 7, 36), Color.rgb(23, 5, 48), Color.rgb(66, 5, 48)}
        );
        splash.setBackground(bg);

        LinearLayout content = new LinearLayout(this);
        content.setGravity(Gravity.CENTER);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(30), dp(24), dp(30), dp(24));
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        );
        splash.addView(content, contentParams);

        FrameLayout logoStage = new FrameLayout(this);
        GradientDrawable halo = new GradientDrawable();
        halo.setShape(GradientDrawable.OVAL);
        halo.setColor(Color.argb(34, 130, 73, 255));
        halo.setStroke(dp(2), Color.argb(100, 92, 188, 255));
        logoStage.setBackground(halo);
        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(dp(190), dp(190));
        stageParams.gravity = Gravity.CENTER;
        stageParams.bottomMargin = dp(34);
        content.addView(logoStage, stageParams);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(dp(142), dp(142), Gravity.CENTER);
        logoStage.addView(logo, logoParams);

        TextView welcome = new TextView(this);
        welcome.setText("WELCOME TO");
        welcome.setTextColor(Color.rgb(100, 205, 255));
        welcome.setTextSize(13);
        welcome.setLetterSpacing(0.32f);
        welcome.setGravity(Gravity.CENTER);
        content.addView(welcome);

        TextView title = new TextView(this);
        title.setText("ABHI KATHA");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setLetterSpacing(0.08f);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(8);
        content.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Your Khata. Always within reach.");
        subtitle.setTextColor(Color.rgb(213, 202, 235));
        subtitle.setTextSize(15);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(12);
        content.addView(subtitle, subtitleParams);

        TextView footer = new TextView(this);
        footer.setText("SMART  •  SIMPLE  •  SECURE");
        footer.setTextColor(Color.rgb(177, 151, 218));
        footer.setTextSize(11);
        footer.setLetterSpacing(0.18f);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        footerParams.topMargin = dp(54);
        content.addView(footer, footerParams);

        root.addView(splash, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ObjectAnimator turn = ObjectAnimator.ofFloat(logo, View.ROTATION_Y, -18f, 18f, -18f);
        turn.setDuration(1900);
        turn.setRepeatCount(ObjectAnimator.INFINITE);
        turn.setInterpolator(new AccelerateDecelerateInterpolator());
        turn.start();

        ObjectAnimator floatLogo = ObjectAnimator.ofFloat(logoStage, View.TRANSLATION_Y, dp(7), -dp(7), dp(7));
        floatLogo.setDuration(2300);
        floatLogo.setRepeatCount(ObjectAnimator.INFINITE);
        floatLogo.setInterpolator(new AccelerateDecelerateInterpolator());
        floatLogo.start();

        splash.setAlpha(0f);
        splash.animate().alpha(1f).setDuration(420).start();
        handler.postDelayed(() -> splash.animate().alpha(0f).setDuration(500)
            .setListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    turn.cancel();
                    floatLogo.cancel();
                    root.removeView(splash);
                }
            }).start(), 2400);
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

    private boolean routeUrl(String value) {
        Uri uri = Uri.parse(value);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (scheme.equals("https") || scheme.equals("http")) {
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if (host.equals("abhishek777f.github.io") || host.endsWith(".github.io")) return false;
            try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (ActivityNotFoundException ignored) { }
            return true;
        }
        if (scheme.equals("about") || scheme.equals("blob") || scheme.equals("data")) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (ActivityNotFoundException e) {
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
        handler.removeCallbacksAndMessages(null);
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (webView != null) { webView.stopLoading(); webView.setWebChromeClient(null); webView.setWebViewClient(null); webView.destroy(); }
        super.onDestroy();
    }
}