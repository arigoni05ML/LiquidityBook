package com.arigoni.liquiditybook;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import org.json.JSONObject;

/**
 * Home-screen widget showing today's P&L and the worst account's drawdown room.
 * The web layer writes a JSON blob to SharedPreferences ("CapacitorStorage",
 * key "widgetData") via the Capacitor Preferences plugin; this provider reads it.
 * Refreshes every 30 minutes (Android's minimum for updatePeriodMillis) and on placement.
 */
public class LiquidityBookWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context));
        }
    }

    static RemoteViews buildViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.lb_widget);
        String today = "\u2014";
        String dd = "";
        try {
            SharedPreferences prefs = context.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
            String raw = prefs.getString("widgetData", null);
            if (raw != null) {
                JSONObject o = new JSONObject(raw);
                double net = o.optDouble("today", 0);
                today = (net < 0 ? "-$" : (net > 0 ? "+$" : "$")) + String.format("%,.2f", Math.abs(net));
                String name = o.optString("name", "");
                if (name != null && !name.isEmpty()) {
                    double buf = o.optDouble("buffer", 0);
                    dd = name + " \u00b7 DD room $" + String.format("%,.0f", buf);
                }
            }
        } catch (Exception e) {
            // leave placeholders
        }

        views.setTextViewText(R.id.widget_today, today);
        views.setTextViewText(R.id.widget_dd, dd.isEmpty() ? "Open Liquidity Book to log a trade" : dd);

        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) launch = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, pi);
        return views;
    }

    public static void refresh(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, LiquidityBookWidget.class));
        for (int id : ids) {
            mgr.updateAppWidget(id, buildViews(context));
        }
    }
}
