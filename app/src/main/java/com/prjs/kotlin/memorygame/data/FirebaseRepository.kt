package com.prjs.kotlin.memorygame.data

import com.prjs.kotlin.memorygame.models.UserImageList
import kotlinx.coroutines.flow.Flow

interface FirebaseRepository {
    fun downloadGame(customGameName: String): Flow<Pair<String, UserImageList?>>
    fun saveDataToFirebase(customGameName: String, title: String, message: String): Flow<String>
    fun handleImageUploading(
        gameName: String,
        filePath: String,
        imageBytes: ByteArray
    ): Flow<Pair<String, Boolean>>

    fun handleAllImagesUploaded(
        gameName: String,
        imageUrls: MutableList<String>
    ): Flow<Boolean>
}