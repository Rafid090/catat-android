package com.notetodo.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notetodo.app.CatatApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are cleared on reboot, so every future reminder is scheduled again. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val container = (context.applicationContext as CatatApplication).container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.taskDao
                    .getPendingReminders(System.currentTimeMillis())
                    .forEach { task ->
                        task.reminderAt?.let { at ->
                            container.reminderScheduler.schedule(task.id, task.title, at)
                        }
                    }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
