package com.prjs.kotlin.memorygame.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.prjs.kotlin.memorygame.ui.CreateActivity.Companion.TAG
import java.io.ByteArrayOutputStream

 fun getImageByteArray(photoUri: Uri, contentResolver: ContentResolver): ByteArray {
    val originalBitmap = if (Build.VERSION.SDK_INT >= 28) {
        val source = ImageDecoder.createSource(contentResolver, photoUri)
        ImageDecoder.decodeBitmap(source)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
    }
    Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
    val scaledBitmap = BitmapScale.scaleToFitHeight(originalBitmap, 250)
    Log.i(TAG, "Scaled width ${scaledBitmap.width} and scaled height ${scaledBitmap.height}")
    val byteOutputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
    return byteOutputStream.toByteArray()
}

object BitmapScale {
    /* fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
    } */

    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
        val factor = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(), height, true)
    }
}

