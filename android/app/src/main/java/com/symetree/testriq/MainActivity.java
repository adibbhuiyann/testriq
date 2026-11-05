package com.symetree.testriq;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Testriq hosts its interface (the same HTML app as the website) inside a WebView.
 * Files are served from the APK under https://appassets.androidplatform.net so the
 * page runs in a secure context (storage, microphone and network work normally).
 */
public class MainActivity extends Activity {
    static final String TAG = "Testriq";
    static final String HOST = "appassets.androidplatform.net";
    static final String START_URL = "https://" + HOST + "/index.html";

    private static final int REQ_FILE = 41;
    static final int REQ_MIC = 42;
    static final int REQ_NOTIFY = 43;

    private static final int LIGHT_BG = 0xFFF4F6FB;
    private static final int DARK_BG = 0xFF0D1221;
    private static final int SPLASH_BG = 0xFF0B1230;

    WebView web;
    private FrameLayout root;
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingWebPermission;
    SpeechBridge speech;
    NativeBridge bridge;
    private volatile String pendingAuth;
    private boolean pageReady;
    private float density = 1f;
    private int insetTop, insetBottom, insetLeft, insetRight;
    private boolean darkTheme;
    volatile boolean resumedNow;
    private Object backCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        setupWindow();

        root = new FrameLayout(this);
        root.setBackgroundColor(SPLASH_BG);
        web = new WebView(this);
        web.setBackgroundColor(SPLASH_BG);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        applySystemBarAppearance(true);
        // the page decides the real colours once its opening screen has finished
        root.setBackgroundColor(SPLASH_BG);
        web.setBackgroundColor(SPLASH_BG);

        configureWebView();
        speech = new SpeechBridge(this);
        bridge = new NativeBridge(this);
        web.addJavascriptInterface(speech, "TestriqSpeech");
        web.addJavascriptInterface(bridge, "TestriqNative");

        setupInsets();
        setupBack();
        handleIntent(getIntent());
        web.loadUrl(START_URL);
    }

    // ---------------------------------------------------------------- window

    private void setupWindow() {
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            // Draw behind the status and navigation bars; the page receives the
            // bar sizes as CSS variables (--sat, --sab, --sal, --sar).
            w.setDecorFitsSystemWindows(false);
            if (Build.VERSION.SDK_INT < 35) {
                w.setStatusBarColor(Color.TRANSPARENT);
                w.setNavigationBarColor(Color.TRANSPARENT);
            }
            w.setNavigationBarContrastEnforced(false);
        } else {
            w.setStatusBarColor(0xFFFFFFFF);
            if (Build.VERSION.SDK_INT >= 27) w.setNavigationBarColor(0xFFFFFFFF);
        }
    }

    void applySystemBarAppearance(boolean dark) {
        try {
            applyBars(dark);
        } catch (RuntimeException e) {
            Log.w(TAG, "System bar appearance not applied", e);
        }
    }

    private void applyBars(boolean dark) {
        darkTheme = dark;
        Window w = getWindow();
        int bg = dark ? DARK_BG : LIGHT_BG;
        if (root != null) root.setBackgroundColor(bg);
        if (web != null) web.setBackgroundColor(bg);
        View decorView = w.getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = decorView.getWindowInsetsController();
            if (c != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                c.setSystemBarsAppearance(dark ? 0 : mask, mask);
            }
        } else {
            int surface = dark ? 0xFF151D31 : 0xFFFFFFFF;
            w.setStatusBarColor(surface);
            if (Build.VERSION.SDK_INT >= 27) w.setNavigationBarColor(surface);
            View decor = w.getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (Build.VERSION.SDK_INT >= 23) {
                flags = dark ? (flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
                        : (flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
            if (Build.VERSION.SDK_INT >= 26) {
                flags = dark ? (flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                        : (flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    void setKeepAwake(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void setupInsets() {
        if (Build.VERSION.SDK_INT < 30) return;
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
            boolean keyboard = ime.bottom > bars.bottom;
            v.setPadding(0, 0, 0, keyboard ? ime.bottom : 0);
            insetTop = bars.top;
            insetLeft = bars.left;
            insetRight = bars.right;
            insetBottom = keyboard ? 0 : bars.bottom;
            pushInsets();
            return WindowInsets.CONSUMED;
        });
        root.requestApplyInsets();
    }

    private void pushInsets() {
        if (!pageReady || web == null) return;
        String js = String.format(Locale.US,
                "(function(d){var s=d.style;s.setProperty('--sat','%.1fpx');s.setProperty('--sab','%.1fpx');"
                        + "s.setProperty('--sal','%.1fpx');s.setProperty('--sar','%.1fpx');d.classList.add('native-app');})(document.documentElement)",
                insetTop / density, insetBottom / density, insetLeft / density, insetRight / density);
        web.evaluateJavascript(js, null);
    }

    // ---------------------------------------------------------------- WebView

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " TestriqApp/" + getString(R.string.app_version));
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        // Native feel: no "Copy / Select all / Web search" menu on long press,
        // except inside text boxes where people type their answers.
        web.setOnLongClickListener(v -> {
            WebView.HitTestResult hit = web.getHitTestResult();
            return hit == null || hit.getType() != WebView.HitTestResult.EDIT_TEXT_TYPE;
        });
        web.setWebViewClient(new AppClient());
        web.setWebChromeClient(new AppChrome());
    }

    private static String mimeFor(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "text/javascript";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".mp3")) return "audio/mpeg";
        if (p.endsWith(".wav")) return "audio/wav";
        if (p.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    private class AppClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            if (!HOST.equals(u.getHost())) return null;
            String path = u.getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) path = "/index.html";
            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "no-cache");
            if (path.contains("..")) {
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", headers,
                        new ByteArrayInputStream(new byte[0]));
            }
            try {
                InputStream in = getAssets().open("www" + path);
                String mime = mimeFor(path);
                boolean text = mime.startsWith("text/") || mime.contains("json") || mime.contains("svg");
                WebResourceResponse r = new WebResourceResponse(mime, text ? "UTF-8" : null, in);
                r.setResponseHeaders(headers);
                return r;
            } catch (IOException e) {
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", headers,
                        new ByteArrayInputStream(new byte[0]));
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            if ("https".equals(u.getScheme()) && HOST.equals(u.getHost())) return false;
            openExternal(u.toString());
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            pageReady = true;
            pushInsets();
            deliverAuth();
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            // The page renderer crashed or was killed to free memory: rebuild the screen.
            Log.w(TAG, "WebView renderer gone; recreating");
            if (root != null) root.removeView(view);
            view.destroy();
            web = null;
            recreate();
            return true;
        }
    }

    private class AppChrome extends WebChromeClient {
        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            runOnUiThread(() -> {
                boolean audio = false;
                for (String r : request.getResources()) {
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) audio = true;
                }
                if (!audio) {
                    request.deny();
                    return;
                }
                if (hasMicPermission()) {
                    request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                } else {
                    if (pendingWebPermission != null) pendingWebPermission.deny();
                    pendingWebPermission = request;
                    requestMic();
                }
            });
        }

        @Override
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            try {
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQ_FILE);
                return true;
            } catch (ActivityNotFoundException e) {
                fileCallback = null;
                callback.onReceiveValue(null);
                return false;
            }
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage m) {
            Log.d(TAG, "JS " + m.messageLevel() + ": " + m.message() + " @" + m.lineNumber());
            return true;
        }
    }

    // ---------------------------------------------------------------- bridge helpers

    void js(final String code) {
        runOnUiThread(() -> {
            if (web != null) web.evaluateJavascript(code, null);
        });
    }

    boolean hasMicPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    void requestMic() {
        if (hasMicPermission()) return;
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
    }

    void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
        } else {
            js("window.testriqPermission&&window.testriqPermission('notifications',true)");
        }
    }

    void openExternal(String url) {
        try {
            Intent i;
            if (url.startsWith("intent:")) {
                i = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                i.addCategory(Intent.CATEGORY_BROWSABLE);
                i.setComponent(null);
                i.setSelector(null);
            } else {
                i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Log.w(TAG, "Cannot open " + url, e);
            js("window.notify&&notify('No app can open this link.')");
        }
    }

    /** Opens a sign-in page in a Chrome Custom Tab (falls back to the default browser). */
    void openAuthTab(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Bundle extras = new Bundle();
        extras.putBinder("android.support.customtabs.extra.SESSION", null);
        i.putExtras(extras);
        i.putExtra("android.support.customtabs.extra.TOOLBAR_COLOR", darkTheme ? 0xFF151D31 : 0xFFFFFFFF);
        i.putExtra("androidx.browser.customtabs.extra.SHARE_STATE", 2);
        i.putExtra("android.support.customtabs.extra.TITLE_VISIBILITY", 1);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            openExternal(url);
        }
    }

    // ---------------------------------------------------------------- sign-in return

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data != null && "testriq".equals(data.getScheme()) && "auth".equals(data.getHost())) {
            pendingAuth = data.toString();
            deliverAuth();
        }
    }

    String takePendingAuth() {
        String a = pendingAuth;
        pendingAuth = null;
        return a == null ? "" : a;
    }

    private void deliverAuth() {
        if (!pageReady || pendingAuth == null) return;
        String a = pendingAuth;
        js("if(window.testriqAuthCallback){window.testriqAuthCallback(" + JSONObject.quote(a)
                + ");TestriqNative.clearPendingAuth();}");
    }

    void clearPendingAuth() {
        pendingAuth = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    // ---------------------------------------------------------------- back

    private void setupBack() {
        if (Build.VERSION.SDK_INT >= 33) {
            OnBackInvokedCallback cb = this::handleBack;
            backCallback = cb;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, cb);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        handleBack();
    }

    private void handleBack() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            moveTaskToBack(true);
        }
    }

    // ---------------------------------------------------------------- results

    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == REQ_MIC) {
            if (pendingWebPermission != null) {
                if (granted) pendingWebPermission.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                else pendingWebPermission.deny();
                pendingWebPermission = null;
            }
            js("window.testriqPermission&&window.testriqPermission('microphone'," + granted + ")");
        } else if (requestCode == REQ_NOTIFY) {
            js("window.testriqPermission&&window.testriqPermission('notifications'," + granted + ")");
        }
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onPause() {
        super.onPause();
        if (speech != null) speech.pauseAll();
        if (web != null) web.onPause();
        resumedNow = false;
        // Reminders only go out while the app is closed.
        Reminders.scheduleNudges(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumedNow = true;
        if (web != null) web.onResume();
        Reminders.cancelNudges(this);
    }

    @Override
    protected void onDestroy() {
        if (speech != null) speech.shutdown();
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((OnBackInvokedCallback) backCallback);
        }
        if (web != null) {
            if (root != null) root.removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
