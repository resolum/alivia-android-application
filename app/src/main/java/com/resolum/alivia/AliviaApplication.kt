package com.resolum.alivia

import android.app.Application
import com.resolum.alivia.features.notifications.push.PushNotificationChannels
import com.resolum.alivia.features.shared.di.SharedModule

class AliviaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SharedModule.init(this)
        PushNotificationChannels.create(this)
    }
}
