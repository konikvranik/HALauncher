package net.suteren.halauncher.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.suteren.halauncher.R;
import net.suteren.halauncher.activity.MainActivity;
import net.suteren.halauncher.activity.SettingsActivity;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_LAUNCHER;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by petr on 23.07.2017.
 */
public class AppAdapter extends BaseAdapter {

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updateActs();
        }
    };
    protected Context context;
    protected List<ResolveInfo> acts;
    protected final PackageManager packageManager;
    private final Collator collator = Collator.getInstance();
    protected final SharedPreferences prefs;
    public static final String APPS_KEY = "apps";

    public AppAdapter(Context mainActivity) {
        this.context = mainActivity;
        packageManager = mainActivity.getApplicationContext().getPackageManager();
        prefs = getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        updateActs();
    }

    public void updateActs() {
        acts = packageManager.queryIntentActivities(createLauncherIntent(), 0);
        Iterator<ResolveInfo> it = acts.iterator();
        while (it.hasNext()) {
            if (!filterApps(it.next()))
                it.remove();
        }
        Collections.sort(acts, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo o1, ResolveInfo o2) {
                if (o1 == null && o2 == null) return 0;
                if (o1 == null) return -1;
                if (o2 == null) return 1;
                if (context.getPackageName().equals(o1.activityInfo.packageName)) return 1;
                if (context.getPackageName().equals(o2.activityInfo.packageName)) return -1;
                if (isSystem(o1) && isSystem(o2)) {
                    return collator.compare(o1.loadLabel(packageManager), o2.loadLabel(packageManager));
                } else {
                    return Boolean.compare(isSystem(o2), isSystem(o1));
                }
            }
        });
        notifyDataSetInvalidated();
        notifyDataSetChanged();
    }

    protected boolean filterApps(ResolveInfo n) {
        return prefs.getStringSet("apps", Collections.EMPTY_SET).contains(new ComponentName(n.activityInfo.packageName, n.activityInfo.name).toShortString()) || (context.getPackageName().equals(n.activityInfo.packageName) && SettingsActivity.class.getName().equals(n.activityInfo.name));
    }

    private boolean isSystem(ResolveInfo o1) {
        return 0 == (o1.activityInfo.flags & ApplicationInfo.FLAG_SYSTEM);
    }

    @Override
    public int getCount() {
        return acts.size();
    }

    @Override
    public ResolveInfo getItem(int position) {
        return acts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        return !(MainActivity.class.getPackage().equals(getItem(position).activityInfo.packageName) && MainActivity.class.getName().equals(getItem(position).activityInfo.name));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(context.getApplicationContext(), R.layout.app_info, null);
        }

        final ResolveInfo appInfo = getItem(position);
        ((ImageView) convertView.findViewById(R.id.icon)).setImageDrawable(appInfo.loadIcon(packageManager));

        ((TextView) convertView.findViewById(R.id.name)).setText(appInfo.loadLabel(packageManager));

        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launch_intent = createLauncherIntent();
                launch_intent.setComponent(new ComponentName(appInfo.activityInfo.packageName, appInfo.activityInfo.name));
                launch_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launch_intent);
            }
        };
        convertView.setOnClickListener(onClick);
        return convertView;
    }

    @NonNull
    private static Intent createLauncherIntent() {
        Intent intent = new Intent();
        intent.addCategory(CATEGORY_LAUNCHER);
        intent.setAction(ACTION_MAIN);
        return intent;
    }

}
