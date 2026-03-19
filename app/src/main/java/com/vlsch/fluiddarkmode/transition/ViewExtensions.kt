package com.vlsch.fluiddarkmode.transition

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun View.captureToBitmapPixelCopy(activity: Activity): Bitmap? =
    suspendCancellableCoroutine { cont ->
        if (width <= 0 || height <= 0) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val output = createBitmap(width, height)
        val location = IntArray(2)
        getLocationInWindow(location)
        val sourceRect = Rect(
            location[0],
            location[1],
            location[0] + width,
            location[1] + height,
        )

        PixelCopy.request(
            activity.window,
            sourceRect,
            output,
            { result ->
                cont.resume(if (result == PixelCopy.SUCCESS) output else null)
            },
            Handler(Looper.getMainLooper()),
        )
    }

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
