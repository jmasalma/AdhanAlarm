package islam.adhanalarm.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import net.sourceforge.jitl.astro.Location;

import islam.adhanalarm.CONSTANT;
import islam.adhanalarm.MainActivity;
import islam.adhanalarm.R;
import islam.adhanalarm.handler.ScheduleData;
import islam.adhanalarm.handler.ScheduleHandler;

public class NextNotificationWidgetProvider extends AppWidgetProvider {

    private static final int[] labels = new int[]{ R.string.fajr, R.string.sunrise, R.string.dhuhr, R.string.asr, R.string.maghrib, R.string.ishaa, R.string.next_fajr };

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (CONSTANT.ACTION_UPDATE_PRAYER_TIME.equals(intent.getAction())) {
            onUpdate(context);
        }
        super.onReceive(context, intent);
    }

    private void onUpdate(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(),getClass().getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager, int appWidgetId) {
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

        final ScheduleData schedule = ScheduleHandler.calculate(location, calculationMethod, roundingType, offsetMinutes);
        final short nextTimeIndex = schedule.nextTimeIndex;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_next_notification);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_next_notification, pendingIntent);

        views.setTextViewText(R.id.time_name, context.getString(labels[nextTimeIndex]));
        String timeFormat = settings.getString("timeFormat", String.valueOf(CONSTANT.DEFAULT_TIME_FORMAT));
        views.setTextViewText(R.id.next_notification, ScheduleHandler.getFormattedTime(schedule.schedule, schedule.extremes, nextTimeIndex, timeFormat));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}