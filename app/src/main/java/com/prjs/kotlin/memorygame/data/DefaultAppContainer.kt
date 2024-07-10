package com.prjs.kotlin.memorygame.data

class DefaultAppContainer: AppContainer {
    override val firebaseRepository: FirebaseRepository by lazy {
        FirebaseRepositoryImpl()
    }
}