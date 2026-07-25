package com.aura

import android.app.Application
import com.aura.di.AppContainer

class AuraApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
