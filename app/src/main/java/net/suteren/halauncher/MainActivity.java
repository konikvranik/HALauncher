package net.suteren.halauncher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

    public static final String JSCONSOLE = "jsconsole";
//    public static final String URL = "http://hadash.home:5050/kuchyne";
    public static final String URL = "http://hadash.home:5050/loznice?skin=daylight";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        GridView appsView = (GridView) findViewById(R.id.menu);
        appsView.setAdapter(new AppAdapter());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
        });
        setDrawerLeftEdgeSize(this, drawer, 1f);
        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = (WebView) findViewById(R.id.fullscreen_content);
        webView.setBackgroundColor(Color.BLACK);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setDatabaseEnabled(true);
        settings.setDisplayZoomControls(false);
        settings.setDomStorageEnabled(true);
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
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
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

    public static void setDrawerLeftEdgeSize(Activity activity, DrawerLayout drawerLayout, float displayWidthPercentage) {
        if (activity == null || drawerLayout == null)
            return;

        try {
            // find ViewDragHelper and set it accessible
            Field leftDraggerField = drawerLayout.getClass().getDeclaredField("mLeftDragger");
            leftDraggerField.setAccessible(true);
            ViewDragHelper leftDragger = (ViewDragHelper) leftDraggerField.get(drawerLayout);
            // find edgesize and set is accessible
            Field edgeSizeField = leftDragger.getClass().getDeclaredField("mEdgeSize");
            edgeSizeField.setAccessible(true);
            int edgeSize = edgeSizeField.getInt(leftDragger);
            // set new edgesize
            Point displaySize = new Point();
            activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
            edgeSizeField.setInt(leftDragger, Math.max(edgeSize, (int) (displaySize.x * displayWidthPercentage)));
        } catch (NoSuchFieldException e) {
            // ignore
        } catch (IllegalArgumentException e) {
            // ignore
        } catch (IllegalAccessException e) {
            // ignore
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide();
        webView.loadUrl(URL);
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

        // Schedule a runnable to remove the status and navigation bar after a delay
        webView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private class AppAdapter extends BaseAdapter {

        final List<ApplicationInfo> apps;
        final PackageManager packageManager = getApplicationContext().getPackageManager();
        final Collator collator = Collator.getInstance();

        AppAdapter() {
            apps = getApplicationContext().getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            Iterator<ApplicationInfo> it = apps.iterator();
            while (it.hasNext()) {
                if (packageManager.getLaunchIntentForPackage(it.next().packageName) == null) {
                    it.remove();
                }
            }
            Collections.sort(apps, new Comparator<ApplicationInfo>() {
                @Override
                public int compare(ApplicationInfo o1, ApplicationInfo o2) {
                    if (o1 == null && o2 == null) return 0;
                    if (o1 == null) return -1;
                    if (o2 == null) return 1;
                    if (isSystem(o1) && isSystem(o2)) {
                        return collator.compare(packageManager.getApplicationLabel(o1), packageManager.getApplicationLabel(o2));
                    } else {
                        return Boolean.compare(isSystem(o2), isSystem(o1));
                    }
                }
            });
        }

        private boolean isSystem(ApplicationInfo o1) {
            return 0 == (o1.flags & ApplicationInfo.FLAG_SYSTEM);
        }

        @Override
        public int getCount() {
            return apps.size();
        }

        @Override
        public ApplicationInfo getItem(int position) {
            return apps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).uid;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getApplicationContext(), R.layout.app_info, null);
            }

            final ApplicationInfo appInfo = getItem(position);
            try {
                ((ImageView) convertView.findViewById(R.id.icon)).setImageDrawable(packageManager.getApplicationIcon(appInfo.packageName));
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("applist", e);
            }

            ((TextView) convertView.findViewById(R.id.name)).setText(packageManager.getApplicationLabel(appInfo));

            View.OnClickListener onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage(appInfo.packageName);
                    if (launchIntentForPackage == null) {
                        apps.remove(appInfo);
                    } else {
                        startActivity(launchIntentForPackage);
                    }
                }
            };
            convertView.setOnClickListener(onClick);
            return convertView;
        }
    }
}
