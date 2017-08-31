package net.suteren.halauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import net.suteren.halauncher.activity.MainActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static net.suteren.halauncher.activity.MainActivity.WAKEUP_ACTION;
import static net.suteren.halauncher.activity.SettingsActivity.LAST_RUN_KEY;
import static net.suteren.halauncher.activity.SettingsActivity.TIME_KEY;
import static net.suteren.halauncher.activity.SettingsActivity.WAKEUP_PREF;

public class DayTimeReceiver extends BroadcastReceiver {
    public static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{1,2})(?::(\\d{1,2}))?");

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
            Log.d("receiver", "Time tick");
            SharedPreferences prefs = getDefaultSharedPreferences(context);
            if (prefs.getBoolean(WAKEUP_PREF, false) && new Date().getTime() > prefs.getLong(LAST_RUN_KEY, 0) + 3600 * 24) {
                Log.d("receiver", "greater than last");
                if (new Date().getTime() > getPlanedTime(context, prefs).getTimeInMillis()) {
                    Log.d("receiver", "Greater than planned");
                    Log.d("receiver", "Wakeup device");
                    context.startActivity(new Intent(WAKEUP_ACTION, null, context, MainActivity.class));
                    prefs.edit().putLong(LAST_RUN_KEY, new Date().getTime()).apply();
                }
            }
            Log.d("receiver", "Time tick DONE");
        }
    }

    @NonNull
    private static Calendar getPlanedTime(Context context, SharedPreferences prefs) {
        Log.d("receiver", "Counting planed");
        Calendar wakeupTime = Calendar.getInstance();
        String defaultTime = context.getResources().getString(R.string.pref_default_time);
        String planed = prefs.getString(TIME_KEY, defaultTime);
        Log.d("receiver", "planed: " + planed);
        Matcher m = TIME_PATTERN.matcher(planed);
        if (m.matches()) {
        } else {
            prefs.edit().remove(TIME_KEY).apply();
            m = TIME_PATTERN.matcher(defaultTime);
        }
        Log.d("receiver", "Matched: " + m.matches());
        wakeupTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(1)));
        wakeupTime.set(Calendar.MINUTE, Integer.parseInt(m.group(2)));
        wakeupTime.set(Calendar.SECOND, Integer.parseInt(m.group(3)));
        wakeupTime.set(Calendar.MILLISECOND, 0);
        Log.d("receiver", "Planed time: " + wakeupTime);
        return wakeupTime;
    }
}
