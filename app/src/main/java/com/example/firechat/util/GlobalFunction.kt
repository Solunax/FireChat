package com.example.firechat.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import java.lang.Exception

fun showText(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun handleError(
    context: Context,
    exception: Exception?,
    errorTag: String,
    errorName: String,
    errorMessage: String
) {
    if (exception != null) {
        Log.e(errorTag, "$errorName: ${exception.message}")
    } else {
        Log.e(errorTag, errorName)
    }

    showText(context, errorMessage)
}