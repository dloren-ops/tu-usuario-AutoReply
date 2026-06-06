package com.autoreply.bot

import android.app.Application
import com.autoreply.bot.di.AppContainer

class AutoReplyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
