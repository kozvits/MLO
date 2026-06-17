package com.mlo.app.ui.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Home screen widget showing today's tasks using Glance (Compose-based widgets).
 */
class TodayTasksWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "MLO — Сегодня",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF1565C0))
                    ),
                    modifier = GlanceModifier.padding(bottom = 4.dp)
                )

                // Placeholder — in production would query repository
                Text(
                    text = "3 активных задачи",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF555555)),
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Quick action row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "➕ Быстрая задача",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF1565C0)),
                            fontSize = 14.sp
                        ),
                        modifier = GlanceModifier.clickable(
                            onClick = actionStartActivity(
                                ComponentName(context, com.mlo.app.MainActivity::class.java),
                                actionParametersOf()
                            )
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val WIDGET_CLASS = "com.mlo.app.ui.widgets.TodayTasksWidget"
        const val WIDGET_RECEIVER_CLASS = "com.mlo.app.ui.widgets.TodayTasksWidgetReceiver"
    }
}

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTasksWidget()
}
