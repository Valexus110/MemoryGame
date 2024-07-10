package com.prjs.kotlin.memorygame

import android.app.Application
import com.prjs.kotlin.memorygame.data.AppContainer
import com.prjs.kotlin.memorygame.data.DefaultAppContainer

class MemoryGameApplication:Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}