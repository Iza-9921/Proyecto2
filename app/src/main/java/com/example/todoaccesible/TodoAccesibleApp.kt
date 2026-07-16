package com.example.todoaccesible

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TodoAccesibleApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Semillas necesarias para poder usar la app sin backend: catálogo de
        // preguntas del Scorecard v2.6 y una cuenta admin de prueba.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.questionCatalogRepository.ensureSeeded()
            container.userRepository.ensureDefaultAdminSeeded()
        }
    }
}
