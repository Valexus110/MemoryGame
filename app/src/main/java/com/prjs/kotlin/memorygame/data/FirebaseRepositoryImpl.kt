package com.prjs.kotlin.memorygame.data

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.prjs.kotlin.memorygame.models.UserImageList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class FirebaseRepositoryImpl : FirebaseRepository {
    private val storage = Firebase.storage
    private val db = Firebase.firestore
    override fun downloadGame(customGameName: String) = callbackFlow {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            val userImageList = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null) {
                trySend("error" to userImageList)
                return@addOnSuccessListener
            }
            trySend("success" to userImageList)
            return@addOnSuccessListener
        }.addOnFailureListener { exception ->
            trySend(exception.toString() to null)
        }
        awaitClose { channel.close() }
    }

    override fun saveDataToFirebase(customGameName: String, title: String, message: String) =
        callbackFlow {
            db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
                if (document != null && document.data != null) {
                    trySend("success")
                } else {
                    storage.reference.child("images/$customGameName").delete()
                    trySend("handle images")
                }
            }.addOnFailureListener { exception ->
                trySend(exception.toString())
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
                        trySend(downloadUrlTask.toString() to false)
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if (didEncounterError) {
                        storage.reference.child("images/$gameName").delete()
                        trySend(downloadUrlTask.toString() to false)
                        return@addOnCompleteListener
                    }
                    trySend(downloadUrlTask.result.toString() to true)
                    return@addOnCompleteListener
                }.addOnFailureListener { e ->
                    trySend(e.toString() to false)
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
                    trySend(false)
                    return@addOnCompleteListener
                }
                trySend(true)
            }.addOnFailureListener {
                trySend(false)
                return@addOnFailureListener
            }
        awaitClose { channel.close() }
    }

    companion object {
        private const val TAG = "FirebaseRepositoryImpl"
    }
}