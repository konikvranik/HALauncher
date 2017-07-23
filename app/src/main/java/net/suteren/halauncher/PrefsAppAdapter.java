package net.suteren.halauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by petr on 23.07.2017.
 */

class PrefsAppAdapter extends AppAdapter {

    private Set<String> apps = new HashSet<>();

    PrefsAppAdapter(Context mainActivity) {
        super(mainActivity);
        apps.addAll(prefs.getStringSet(APPS_KEY, Collections.EMPTY_SET));
    }

    @Override
    protected boolean filterApps(ResolveInfo n) {
        return !context.getPackageName().equals(n.activityInfo.packageName);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(context.getApplicationContext(), R.layout.prefs_app_info, null);
        }

        final ResolveInfo appInfo = getItem(position);
        ((ImageView) convertView.findViewById(R.id.icon)).setImageDrawable(appInfo.loadIcon(packageManager));

        ((TextView) convertView.findViewById(R.id.name)).setText(appInfo.loadLabel(packageManager));
        final CheckBox checkbox = ((CheckBox) convertView.findViewById(R.id.checkbox));
        final String name = new ComponentName(appInfo.activityInfo.packageName, appInfo.activityInfo.name).toShortString();
        checkbox.setChecked(apps.contains(name));
        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    apps.add(name);
                } else {
                    apps.remove(name);
                }
                prefs.edit().putStringSet(APPS_KEY, apps).apply();
            }
        });
        View.OnClickListener onClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkbox.toggle();
            }
        };
        convertView.setOnClickListener(onClick);
        return convertView;
    }
}
