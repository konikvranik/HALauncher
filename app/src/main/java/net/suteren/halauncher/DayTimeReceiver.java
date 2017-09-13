package net.suteren.halauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import net.suteren.halauncher.activity.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
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
            if (isAllowed(context, prefs)) {
                Log.d("receiver", "Greater than planned");
                Log.d("receiver", "Wakeup device");
                context.startActivity(new Intent(WAKEUP_ACTION, null, context, MainActivity.class));
                prefs.edit().putLong(LAST_RUN_KEY, new Date().getTime()).apply();
            }
            Log.d("receiver", "Time tick DONE");
        }
    }

    private boolean isAllowed(Context context, SharedPreferences prefs) {
        if (!prefs.getBoolean(WAKEUP_PREF, false)) return false;
        Calendar now = Calendar.getInstance();
        return isMatch(context, prefs, now);
    }

    private boolean isMatch(Context context, SharedPreferences prefs, Calendar now) {
        String nowString = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now.getTime());
        return nowString != null && nowString.equals(prefs.getString(TIME_KEY, context.getResources().getString(R.string.pref_default_time)));
    }


    private boolean isNextDay(SharedPreferences prefs, Calendar now) {
        Calendar nextDay = Calendar.getInstance();
        nextDay.setTimeInMillis(prefs.getLong(LAST_RUN_KEY, 0));
        nextDay.set(HOUR_OF_DAY, 0);
        nextDay.set(MINUTE, 0);
        nextDay.set(SECOND, 0);
        nextDay.set(MILLISECOND, 0);
        nextDay.add(DAY_OF_MONTH, 1);
        return now.after(nextDay);
    }

    private boolean isAfterPlaned(Context context, SharedPreferences prefs, Calendar now) {
        Calendar wakeupTime = Calendar.getInstance();
        String defaultTime = context.getResources().getString(R.string.pref_default_time);
        String planed = prefs.getString(TIME_KEY, defaultTime);
        Matcher m = TIME_PATTERN.matcher(planed);
        if (!m.matches()) {
            prefs.edit().remove(TIME_KEY).apply();
            m = TIME_PATTERN.matcher(defaultTime);
        }
        wakeupTime.set(HOUR_OF_DAY, Integer.parseInt(m.group(1)));
        wakeupTime.set(MINUTE, Integer.parseInt(m.group(2)));
        wakeupTime.set(SECOND, Integer.parseInt(m.group(3)));
        wakeupTime.set(MILLISECOND, 0);
        return now.after(wakeupTime);
    }
}
