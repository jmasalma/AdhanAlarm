package islam.adhanalarm.receiver;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import androidx.core.app.NotificationCompat;
import net.sourceforge.jitl.astro.Location;
import java.util.Calendar;
import islam.adhanalarm.App;
import islam.adhanalarm.CONSTANT;
import islam.adhanalarm.MainActivity;
import islam.adhanalarm.R;
import islam.adhanalarm.handler.ScheduleData;
import islam.adhanalarm.handler.ScheduleHandler;
import islam.adhanalarm.util.NotificationHelper;

public class StartNotificationReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1651;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(CONSTANT.ACTION_NOTIFY_PRAYER_TIME)) {
            showNotification(context, intent);
        }
        setNextAlarm(context);
    }

    private void showNotification(Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        short timeIndex = intent.getShortExtra("timeIndex", (short) -1);

        final short notificationType;
        if (timeIndex != -1) {
            short queryTimeIndex = timeIndex;
            if (queryTimeIndex == CONSTANT.NEXT_FAJR) {
                queryTimeIndex = CONSTANT.FAJR;
            }

            String defaultValue;
            switch (queryTimeIndex) {
                case CONSTANT.FAJR:
                case CONSTANT.MAGHRIB:
                    defaultValue = String.valueOf(CONSTANT.NOTIFICATION_PLAY);
                    break;
                case CONSTANT.SUNRISE:
                    defaultValue = String.valueOf(CONSTANT.NOTIFICATION_NONE);
                    break;
                default:
                    defaultValue = String.valueOf(CONSTANT.NOTIFICATION_DEFAULT);
                    break;
            }
            notificationType = Short.parseShort(settings.getString("notificationMethod" + queryTimeIndex, defaultValue));
        } else {
            notificationType = CONSTANT.NOTIFICATION_NONE;
        }

        if (notificationType != CONSTANT.NOTIFICATION_NONE) {
            if (timeIndex == CONSTANT.NEXT_FAJR) {
                timeIndex = CONSTANT.FAJR;
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                    .setSmallIcon(R.drawable.icon_notification)
                    .setContentTitle((timeIndex != CONSTANT.SUNRISE ? context.getString(R.string.allahu_akbar) + ": " : "") + context.getString(R.string.time_for) + " " + context.getString(CONSTANT.TIME_NAMES[timeIndex]).toLowerCase())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(PendingIntent.getBroadcast(context, 0, new Intent(CONSTANT.ACTION_NOTIFICATION_CLICKED, null, context, HandleNotificationReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE))
                    .setDeleteIntent(PendingIntent.getBroadcast(context, 0, new Intent(CONSTANT.ACTION_NOTIFICATION_DELETED, null, context, HandleNotificationReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE));

            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (audioManager != null && telephonyManager != null) {
                int ringerMode = audioManager.getRingerMode();
                int callState = telephonyManager.getCallState();

                if (ringerMode != AudioManager.RINGER_MODE_SILENT && ringerMode != AudioManager.RINGER_MODE_VIBRATE && callState == TelephonyManager.CALL_STATE_IDLE) {
                    if (notificationType == CONSTANT.NOTIFICATION_PLAY || notificationType == CONSTANT.NOTIFICATION_BEEP) {
                        int resid = R.raw.beep;
                        if (notificationType == CONSTANT.NOTIFICATION_PLAY) {
                            resid = timeIndex == CONSTANT.FAJR ? R.raw.adhan_fajr : R.raw.adhan;
                        }
                        builder.addAction(android.R.drawable.stat_notify_call_mute, context.getString(R.string.stop), PendingIntent.getBroadcast(context, 0, new Intent(CONSTANT.ACTION_NOTIFICATION_STOPPED, null, context, HandleNotificationReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE));
                        App.startMedia(resid);
                    }
                }
            }

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        }
        App.broadcastPrayerTimeUpdate();
    }

    private static void setNextAlarm(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        Location location = ScheduleHandler.getLocation(
                settings.getString("latitude", "0"),
                settings.getString("longitude", "0"),
                settings.getString("altitude", "0"),
                settings.getString("pressure", "1010"),
                settings.getString("temperature", "10")
        );
        String calculationMethod = settings.getString("calculationMethod", String.valueOf(CONSTANT.DEFAULT_CALCULATION_METHOD));
        String roundingType = settings.getString("rounding", String.valueOf(CONSTANT.DEFAULT_ROUNDING_TYPE));
        int offsetMinutes = Integer.parseInt(settings.getString("offsetMinutes", "0"));

        ScheduleData schedule = ScheduleHandler.calculate(location, calculationMethod, roundingType, offsetMinutes);
        short timeIndex = schedule.nextTimeIndex;
        Calendar actualTime = schedule.schedule[timeIndex];

        if (Calendar.getInstance().after(actualTime)) return; // Somehow current time is greater than the prayer time

        Intent intent = new Intent(context, StartNotificationReceiver.class);
        intent.setAction(CONSTANT.ACTION_NOTIFY_PRAYER_TIME);
        intent.putExtra("timeIndex", timeIndex);
        intent.putExtra("actualTime", actualTime.getTimeInMillis());

        Intent infoIntent = new Intent(context, MainActivity.class);
        infoIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingInfoIntent = PendingIntent.getActivity(context, 0, infoIntent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long triggerAtMillis = actualTime.getTimeInMillis();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (am != null) {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAtMillis, pendingInfoIntent), pendingIntent);
        }
    }
}