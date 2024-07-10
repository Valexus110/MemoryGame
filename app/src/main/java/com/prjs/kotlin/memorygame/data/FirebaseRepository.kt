package com.prjs.kotlin.memorygame.data

import com.prjs.kotlin.memorygame.models.UserImageList
import kotlinx.coroutines.flow.Flow

interface FirebaseRepository {
    fun downloadGame(customGameName: String): Flow<Pair<String, UserImageList?>>
    fun saveDataToFirebase()
    fun handleImageUploading()
    fun handleAllImagesUploaded()
}