package com.example.firechat.model.repository

import android.net.Uri
import com.example.firechat.model.data.CurrentUserData
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class ProfileRepository {
    private val firebaseStorage = FirebaseStorage.getInstance()

    private fun getProfileReference(): StorageReference {
        return firebaseStorage.getReference("${CurrentUserData.uid}/profile.jpg")
    }

    suspend fun uploadProfileImage(uri: Uri): Boolean {
        return try {
            getProfileReference().putFile(uri).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getProfileImage(): Uri? {
        return try {
            getProfileReference().downloadUrl.await()
        } catch (e: Exception) {
            null
        }
    }
}