package com.example.playback

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

// Home screen player widget. The initial/idle state just opens the app;
// MusicPlaybackService pushes live track/state updates while playing.
class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, "High Music")
            views.setTextViewText(R.id.widget_artist, "برای پخش، برنامه را باز کنید")
            views.setImageViewResource(R.id.widget_cover, R.mipmap.ic_launcher)
            views.setImageViewResource(R.id.widget_play_pause, R.drawable.ic_widget_play)

            val openApp = PendingIntent.getActivity(
                context, 13, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_cover, openApp)
            views.setOnClickPendingIntent(R.id.widget_title, openApp)
            views.setOnClickPendingIntent(R.id.widget_play_pause, openApp)
            views.setOnClickPendingIntent(R.id.widget_next, openApp)
            views.setOnClickPendingIntent(R.id.widget_prev, openApp)

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
