package com.prjs.kotlin.memorygame.data

import com.prjs.kotlin.memorygame.models.UserImageList
import com.prjs.kotlin.memorygame.utils.FlowStatus
import kotlinx.coroutines.flow.Flow

interface FirebaseRepository {
    fun downloadGame(customGameName: String): Flow<Pair<FlowStatus, UserImageList?>>
    fun saveDataToFirebase(customGameName: String): Flow<FlowStatus>
    fun handleImageUploading(
        gameName: String,
        filePath: String,
        imageBytes: ByteArray
    ): Flow<Pair<String, FlowStatus>>

    fun handleAllImagesUploaded(
        gameName: String,
        imageUrls: MutableList<String>
    ): Flow<FlowStatus>
}