package com.prjs.kotlin.memorygame

import android.app.Application
import com.prjs.kotlin.memorygame.di.FirebaseModule
import com.prjs.kotlin.memorygame.ui.CreateActivity
import com.prjs.kotlin.memorygame.ui.MainActivity
import dagger.Component

@Component(modules = [FirebaseModule::class])
interface AppComponent {
    fun inject(activity: MainActivity)
    fun inject(activity: CreateActivity)
}

class MemoryGameApplication:Application() {
    val appComponent: AppComponent = DaggerAppComponent.create()
}