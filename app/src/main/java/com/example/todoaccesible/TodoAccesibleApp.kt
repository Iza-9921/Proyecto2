package com.example.todoaccesible

import android.app.Application

class TodoAccesibleApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
