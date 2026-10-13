package com.notetodo.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Wraps AlarmManager. Exact alarms need a user-granted permission from Android 12
 * onward, so this degrades to an inexact alarm rather than failing outright — the
 * reminder may then arrive a few minutes late instead of never.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(taskId: Long, title: String, triggerAtMillis: Long) {
        // A reminder in the past would fire immediately; drop it instead.
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val pendingIntent = buildPendingIntent(taskId, title)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(buildPendingIntent(taskId, title = ""))
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * The task id alone identifies the alarm: [Intent.filterEquals] ignores extras,
     * so cancelling does not need the original title.
     */
    private fun buildPendingIntent(taskId: Long, title: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
