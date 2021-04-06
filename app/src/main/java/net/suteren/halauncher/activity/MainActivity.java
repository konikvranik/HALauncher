package net.suteren.halauncher.activity;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import net.suteren.halauncher.DayTimeReceiver;
import net.suteren.halauncher.R;
import net.suteren.halauncher.adapter.AppAdapter;

import static android.net.wifi.WifiManager.WIFI_MODE_FULL;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static android.provider.Settings.Global.WIFI_SLEEP_POLICY;
import static android.provider.Settings.Global.WIFI_SLEEP_POLICY_NEVER;
import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
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
	private WifiManager.WifiLock wl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		partial = acquireWakelock(PARTIAL_WAKE_LOCK);
		wl = acquireWifiLock();

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
		getWindow().addFlags(FLAG_DISMISS_KEYGUARD | FLAG_KEEP_SCREEN_ON | FLAG_TURN_SCREEN_ON | FLAG_SHOW_WHEN_LOCKED);
		setupWebView();
		reloadWeb();
		registerReceiver(new DayTimeReceiver(), new IntentFilter(Intent.ACTION_TIME_TICK));
	}

	private WifiManager.WifiLock acquireWifiLock() {
		Settings.System.putInt(getContentResolver(), WIFI_SLEEP_POLICY, WIFI_SLEEP_POLICY_NEVER);
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
		assert wifiManager != null;
		WifiManager.WifiLock fullWifi = wifiManager.createWifiLock(WIFI_MODE_FULL, "fullWifi");
		fullWifi.acquire();
		return fullWifi;
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
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
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					webView.evaluateJavascript("$('body').css('-webkit-tap-highlight-color', 'rgba(0, 0, 0, 0)')", null);
				}
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

		if (Build.VERSION.SDK_INT >= 19) {
			getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		} else {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
			int flags = View.SYSTEM_UI_FLAG_LOW_PROFILE
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			findViewById(android.R.id.content).setSystemUiVisibility(flags);
			webView.setSystemUiVisibility(flags);
		}

		// Schedule a runnable to remove the status and navigation bar after a delay
	}

	// Called whenever we need to wake up the device
	public void wakeDevice() {
		getWindow().addFlags(FLAG_DISMISS_KEYGUARD | FLAG_KEEP_SCREEN_ON | FLAG_TURN_SCREEN_ON | FLAG_SHOW_WHEN_LOCKED);
		acquireWakelock(SCREEN_BRIGHT_WAKE_LOCK | FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP);
	}

	@SuppressLint("WakelockTimeout")
	private PowerManager.WakeLock acquireWakelock(int levelAndFlags) {
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		assert powerManager != null;
		PowerManager.WakeLock wakeLock = powerManager
			.newWakeLock(levelAndFlags, getClass().getName());
		wakeLock.acquire();
		return wakeLock;
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
