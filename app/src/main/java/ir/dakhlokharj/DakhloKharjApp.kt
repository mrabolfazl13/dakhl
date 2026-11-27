package ir.dakhlokharj

import android.app.Application
import ir.dakhlokharj.di.AppContainer
import ir.dakhlokharj.notifications.NotificationHelper

class DakhloKharjApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
    }
}
