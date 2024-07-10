package com.prjs.kotlin.memorygame.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.prjs.kotlin.memorygame.models.UserImageList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirebaseRepositoryImpl: FirebaseRepository {
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

    override fun saveDataToFirebase() {
        TODO("Not yet implemented")
    }

    override fun handleImageUploading() {
        TODO("Not yet implemented")
    }

    override fun handleAllImagesUploaded() {
        TODO("Not yet implemented")
    }

}