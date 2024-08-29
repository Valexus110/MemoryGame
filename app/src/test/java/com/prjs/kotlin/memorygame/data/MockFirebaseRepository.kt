package com.prjs.kotlin.memorygame.data

import com.prjs.kotlin.memorygame.models.UserImageList
import com.prjs.kotlin.memorygame.utils.FlowStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MockFirebaseRepository : FirebaseRepository {

    override fun downloadGame(customGameName: String): Flow<Pair<FlowStatus, UserImageList?>> =
        callbackFlow {
            if (customGameName != "game") {
                trySend(FlowStatus.Error to null)
            } else {
                //When images ARE found
                val imagesList = listOf("image_1", "image_2")
                trySend(FlowStatus.Success to UserImageList(images = imagesList))
                //When images ARE NOT found
//                val imagesList: Nothing? = null
//                trySend(FlowStatus.Error to UserImageList(images = imagesList))

            }
            awaitClose { channel.close() }
        }

    override fun saveDataToFirebase(
        customGameName: String
    ): Flow<FlowStatus> = callbackFlow {
        if (customGameName != "game") {
            trySend(FlowStatus.Error)
        } else {
            trySend(FlowStatus.Success)
        }
        awaitClose { channel.close() }
    }

    override fun handleImageUploading(
        gameName: String,
        filePath: String,
        imageBytes: ByteArray
    ): Flow<Pair<String, FlowStatus>> = callbackFlow {
            if(gameName != "game" || filePath != "path") {
                trySend("Exception" to FlowStatus.Error)
            } else {
                //When there's no error
                trySend("Result" to FlowStatus.Success)
                //When there's an error
                trySend("Error" to FlowStatus.Error)
            }
        awaitClose { channel.close() }
    }

    override fun handleAllImagesUploaded(
        gameName: String,
        imageUrls: MutableList<String>
    ): Flow<FlowStatus> = callbackFlow {
        if (gameName != "game") {
            trySend(FlowStatus.Error)
        } else {
            trySend(FlowStatus.Success)
        }
        awaitClose { channel.close() }
    }
}