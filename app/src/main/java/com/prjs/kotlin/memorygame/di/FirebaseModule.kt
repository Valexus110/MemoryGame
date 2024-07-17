package com.prjs.kotlin.memorygame.di

import com.prjs.kotlin.memorygame.data.FirebaseRepository
import com.prjs.kotlin.memorygame.data.FirebaseRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
abstract class FirebaseModule {
    @Binds
    abstract fun provideRepository(repository: FirebaseRepositoryImpl): FirebaseRepository
}