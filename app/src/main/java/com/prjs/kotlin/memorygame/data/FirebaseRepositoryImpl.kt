package com.prjs.kotlin.memorygame.data

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.prjs.kotlin.memorygame.models.UserImageList
import com.prjs.kotlin.memorygame.utils.FlowStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class FirebaseRepositoryImpl @Inject constructor() : FirebaseRepository {
    private val storage = Firebase.storage
    private val db = Firebase.firestore
    override fun downloadGame(customGameName: String) = callbackFlow {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            val userImageList = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null) {
                trySend(FlowStatus.Error to userImageList)
                return@addOnSuccessListener
            }
            trySend(FlowStatus.Success to userImageList)
            return@addOnSuccessListener
        }.addOnFailureListener { exception ->
            Log.e(
                TAG,
                "Exception when retrieving game",
                Exception(exception)
            )
            trySend(FlowStatus.Error to null)
        }
        awaitClose { channel.close() }
    }

    override fun saveDataToFirebase(customGameName: String, title: String, message: String) =
        callbackFlow {
            db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
                if (document != null && document.data != null) {
                    trySend(FlowStatus.Success)
                } else {
                    storage.reference.child("images/$customGameName").delete()
                    trySend(FlowStatus.HandleImages)
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Encounter error while saving game", exception)
                trySend(FlowStatus.Error)
            }
            awaitClose { channel.close() }
        }

    override fun handleImageUploading(gameName: String, filePath: String, imageBytes: ByteArray) =
        callbackFlow {
            val photoReference = storage.reference.child(filePath)
            var didEncounterError = false
            photoReference.putBytes(imageBytes)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    photoReference.downloadUrl
                }
                .addOnCompleteListener { downloadUrlTask ->         //}
                    if (!downloadUrlTask.isSuccessful) {
                        trySend(downloadUrlTask.toString() to FlowStatus.Error)
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if (didEncounterError) {
                        storage.reference.child("images/$gameName").delete()
                        trySend(downloadUrlTask.toString() to FlowStatus.Error)
                        return@addOnCompleteListener
                    }
                    trySend(downloadUrlTask.result.toString() to FlowStatus.Success)
                    return@addOnCompleteListener
                }.addOnFailureListener { e ->
                    trySend(e.toString() to FlowStatus.Error)
                }
            awaitClose { channel.close() }
        }


    override fun handleAllImagesUploaded(
        gameName: String,
        imageUrls: MutableList<String>
    ) = callbackFlow {
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                if (!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    trySend(FlowStatus.Error)
                    return@addOnCompleteListener
                }
                trySend(FlowStatus.Success)
            }.addOnFailureListener {
                trySend(FlowStatus.Error)
                return@addOnFailureListener
            }
        awaitClose { channel.close() }
    }

    companion object {
        private const val TAG = "FirebaseRepositoryImpl"
    }
}