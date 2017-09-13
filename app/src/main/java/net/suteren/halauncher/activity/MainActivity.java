package net.suteren.halauncher.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.GridView;

import net.suteren.halauncher.DayTimeReceiver;
import net.suteren.halauncher.R;
import net.suteren.halauncher.adapter.AppAdapter;

import java.util.Locale;

import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

    public static final String JSCONSOLE = "jsconsole";
    public static final String JALERT = "jalert";
    public static final String RELOAD_ACTION = "reload";
    public static final String WAKEUP_ACTION = "wakeup";
    private WebView webView;
    private PowerManager.WakeLock partial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        partial = ((PowerManager) getSystemService(POWER_SERVICE))
                .newWakeLock(PARTIAL_WAKE_LOCK, getClass().getName());

        final GridView appsView = (GridView) findViewById(R.id.menu);
        AppAdapter adapter = new AppAdapter(this);
        appsView.setAdapter(adapter);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                ((AppAdapter) appsView.getAdapter()).updateActs();
            }
        });
        //setDrawerLeftEdgeSize(this, drawer, 1f);
        getWindow().addFlags(FLAG_DISMISS_KEYGUARD | FLAG_KEEP_SCREEN_ON | FLAG_TURN_SCREEN_ON);
        setupWebView();
        reloadWeb();
        registerReceiver(new DayTimeReceiver(), new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = (WebView) findViewById(R.id.fullscreen_content);
        webView.setBackgroundColor(Color.BLACK);
        //webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDatabaseEnabled(true);
        settings.setDisplayZoomControls(false);
        settings.setDomStorageEnabled(true);
        //settings.setLayoutAlgorithm(SINGLE_COLUMN);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String msg = String.format(Locale.getDefault(), "js (%s:%d): %s", consoleMessage.sourceId(), consoleMessage.lineNumber(), consoleMessage.message());
                switch (consoleMessage.messageLevel()) {
                    case DEBUG:
                        Log.d(JSCONSOLE, msg);
                        break;
                    case ERROR:
                        Log.e(JSCONSOLE, msg);
                        break;
                    case TIP:
                        Log.v(JSCONSOLE, msg);
                        break;
                    case WARNING:
                        Log.w(JSCONSOLE, msg);
                        break;
                    case LOG:
                        Log.i(JSCONSOLE, msg);
                        break;
                }
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                result.cancel();
                Log.w(JALERT, url + "|" + message);
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.evaluateJavascript("$('body').css('-webkit-tap-highlight-color', 'rgba(0, 0, 0, 0)')", null);
                super.onPageFinished(view, url);
                Log.i(JSCONSOLE, "Page finished");
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                Log.i(JSCONSOLE, "Resource loaded: " + url);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        delayedHide();
        webView.resumeTimers();
        partial.acquire();
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Schedule a runnable to remove the status and navigation bar after a delay
        webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }


    // Called implicitly when device is about to sleep or application is backgrounded
    protected void onPause() {
        super.onPause();
        if (partial != null) {
            partial.release();
        }
    }

    // Called whenever we need to wake up the device
    public void wakeDevice() {
        PowerManager.WakeLock wakelock = ((PowerManager) getSystemService(POWER_SERVICE))
                .newWakeLock(SCREEN_BRIGHT_WAKE_LOCK | FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP, getClass().getName());
        try {
            wakelock.acquire();
            Thread.sleep(700);
        } catch (InterruptedException e) {
        } finally {
            wakelock.release();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("main", "Action: " + intent.getAction());
        Log.d("main", "Data: " + intent.getData());
        if (WAKEUP_ACTION.equals(intent.getAction())) {
            wakeDevice();
        } else if (RELOAD_ACTION.equals(intent.getAction())) {
            reloadWeb();
        }
    }

    private void reloadWeb() {
        webView.clearCache(true);
        webView.clearFormData();
        webView.clearHistory();
        webView.clearMatches();
        webView.loadUrl(getDefaultSharedPreferences(this).getString("url", getResources().getString(R.string.pref_default_url)));
    }
}
