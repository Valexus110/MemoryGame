package com.prjs.kotlin.memorygame.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.prjs.kotlin.memorygame.MemoryGameApplication
import com.prjs.kotlin.memorygame.data.FirebaseRepository
import com.prjs.kotlin.memorygame.models.UserImageList
import kotlinx.coroutines.flow.Flow

class MainViewModel(
    private val repository: FirebaseRepository
) : ViewModel() {
    fun downloadGame(customName: String): Flow<Pair<String, UserImageList?>> {
        return repository.downloadGame(customName)
    }

    fun saveDataToFirebase(customName: String) {

    }

    fun handleImageUploading() {

    }

    fun handleAllImagesUploaded() {

    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MemoryGameApplication)
                val firebaseRepository = application.container.firebaseRepository
                MainViewModel(repository = firebaseRepository)
            }
        }
    }
}