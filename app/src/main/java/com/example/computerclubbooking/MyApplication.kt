package com.example.computerclubbooking

import android.app.Application
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Эта строка должна быть ТОЛЬКО здесь, внутри фигурных скобок метода onCreate
        FirebaseApp.initializeApp(this)
    }
}
