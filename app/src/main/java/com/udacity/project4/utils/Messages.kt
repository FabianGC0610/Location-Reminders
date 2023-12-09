package com.udacity.project4.utils

import android.content.Context
import android.widget.Toast

fun showToast(message: String, context: Context) {
    Toast.makeText(
        context,
        message,
        Toast.LENGTH_SHORT,
    ).show()
}
