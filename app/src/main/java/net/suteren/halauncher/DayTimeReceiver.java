package net.suteren.halauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static net.suteren.halauncher.PrefsAppAdapter.LAST_RUN_KEY;
import static net.suteren.halauncher.PrefsAppAdapter.TIME_KEY;

public class DayTimeReceiver extends BroadcastReceiver {
    public static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{1,2})(?:(\\d{1,2}))?");
    final SharedPreferences prefs;

    DayTimeReceiver(Context context) {
        prefs = getDefaultSharedPreferences(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
            if (prefs.getBoolean("wakeup", false) && new Date().getTime() > prefs.getLong(LAST_RUN_KEY, 0) + 3600 * 24) {
                if (prefs.getBoolean("wakeup", false) && new Date().getTime() > getPlanedTime(context, prefs).getTimeInMillis()) {
                    //context.startActivity(new Intent(context, MainActivity.class));
                    prefs.edit().putLong(LAST_RUN_KEY, new Date().getTime()).apply();
                }
            }
        }
    }

    @NonNull
    private static Calendar getPlanedTime(Context context, SharedPreferences prefs) {
        Calendar wakeupTime = Calendar.getInstance();
        Matcher m = TIME_PATTERN.matcher(prefs.getString(TIME_KEY, context.getResources().getString(R.string.pref_default_time)));
        wakeupTime.set(Calendar.HOUR, Integer.parseInt(m.group(1)));
        wakeupTime.set(Calendar.MINUTE, Integer.parseInt(m.group(2)));
        wakeupTime.set(Calendar.SECOND, Integer.parseInt(m.group(3)));
        wakeupTime.set(Calendar.MILLISECOND, 0);
        return wakeupTime;
    }
}
