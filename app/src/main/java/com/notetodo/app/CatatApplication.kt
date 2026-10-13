package com.notetodo.app

import android.app.Application
import com.notetodo.app.data.AppContainer
import com.notetodo.app.reminder.createReminderChannel

class CatatApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createReminderChannel(this)
    }
}
