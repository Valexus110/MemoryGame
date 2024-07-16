package com.prjs.kotlin.memorygame.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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

    fun saveDataToFirebase(customGameName: String, title: String, message: String): Flow<String> {
        return repository.saveDataToFirebase(customGameName, title, message)
    }

    fun handleImageUploading(
        gameName: String,
        filePath: String,
        imageBytes: ByteArray
    ): Flow<Pair<String, Boolean>> {
        return repository.handleImageUploading(gameName, filePath, imageBytes)
    }

    fun handleAllImagesUploaded(
        gameName: String,
        imageUrls: MutableList<String>
    ): Flow<Boolean> {
        return repository.handleAllImagesUploaded(gameName, imageUrls)
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