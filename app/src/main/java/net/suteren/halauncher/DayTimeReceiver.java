package net.suteren.halauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class DayTimeReceiver extends BroadcastReceiver {
    final SharedPreferences prefs;

    DayTimeReceiver(Context context) {
        prefs = getDefaultSharedPreferences(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
            try {
                Date time = new SimpleDateFormat("HH:MM:SS", Locale.getDefault()).parse(prefs.getString("time", context.getResources().getString(R.string.pref_default_time)));

                if (new Date().getTime() > time.getTime()) {
                    context.startActivity(new Intent(context, MainActivity.class));
                }

            } catch (ParseException e) {
                Log.e("dashboard receiver", e.getMessage(), e);
            }
        }
    }
}
