package com.prjs.kotlin.memorygame.ui

import androidx.lifecycle.ViewModel
import com.prjs.kotlin.memorygame.data.FirebaseRepository
import com.prjs.kotlin.memorygame.models.UserImageList
import com.prjs.kotlin.memorygame.utils.FlowStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val repository: FirebaseRepository
) : ViewModel() {
    fun downloadGame(customName: String): Flow<Pair<FlowStatus, UserImageList?>> {
        return repository.downloadGame(customName)
    }

    fun saveDataToFirebase(customGameName: String, title: String, message: String): Flow<FlowStatus> {
        return repository.saveDataToFirebase(customGameName, title, message)
    }

    fun handleImageUploading(
        gameName: String,
        filePath: String,
        imageBytes: ByteArray
    ): Flow<Pair<String, FlowStatus>> {
        return repository.handleImageUploading(gameName, filePath, imageBytes)
    }

    fun handleAllImagesUploaded(
        gameName: String,
        imageUrls: MutableList<String>
    ): Flow<FlowStatus> {
        return repository.handleAllImagesUploaded(gameName, imageUrls)
    }
}