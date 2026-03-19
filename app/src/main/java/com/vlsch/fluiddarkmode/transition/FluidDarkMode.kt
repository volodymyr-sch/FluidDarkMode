package com.vlsch.fluiddarkmode.transition

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.hypot

private const val TOGGLE_UNLOCK_THRESHOLD = 0.94f

@Composable
fun FluidDarkMode(
    activity: Activity,
    isDarkMode: Boolean,
    fluidDarkModeChangeRequest: FluidDarkModeChangeRequest? = null,
    onTransitionRunningChanged: (Boolean) -> Unit = {},
    content: @Composable (Boolean) -> Unit,
) {
    var displayedDarkMode by remember { mutableStateOf(isDarkMode) }
    var snapshot by remember { mutableStateOf<Bitmap?>(null) }
    var revealCenter by remember { mutableStateOf(Offset.Zero) }
    var isTransitionRunning by remember { mutableStateOf(false) }
    var isInputBlocked by remember { mutableStateOf(false) }
    val radius = remember { Animatable(0f) }
    val maxRadius = remember { mutableFloatStateOf(0f) }
    val transitionRunningCallback by rememberUpdatedState(onTransitionRunningChanged)
    val view = activity.window.decorView.findViewById<android.view.View>(android.R.id.content)

    LaunchedEffect(isTransitionRunning) {
        transitionRunningCallback(isTransitionRunning)
    }

    LaunchedEffect(isDarkMode, fluidDarkModeChangeRequest?.id) {
        val request = fluidDarkModeChangeRequest
        if (request == null) {
            displayedDarkMode = isDarkMode
            isInputBlocked = false
            isTransitionRunning = false
            return@LaunchedEffect
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            snapshot?.recycle()
            snapshot = null
            radius.snapTo(0f)
            displayedDarkMode = isDarkMode
            isInputBlocked = false
            isTransitionRunning = false
            return@LaunchedEffect
        }

        isTransitionRunning = true
        isInputBlocked = true

        try {
            radius.snapTo(0f)
            snapshot?.recycle()
            snapshot = null

            val capturedSnapshot = view.captureToBitmapPixelCopy(activity)
            if (capturedSnapshot == null) {
                displayedDarkMode = isDarkMode
                return@LaunchedEffect
            }

            val viewSize = Size(view.width.toFloat(), view.height.toFloat())
            if (viewSize.width <= 0f || viewSize.height <= 0f) {
                capturedSnapshot.recycle()
                displayedDarkMode = isDarkMode
                return@LaunchedEffect
            }

            revealCenter = sanitizeCenter(
                candidate = request.center,
                size = viewSize,
            )
            snapshot = capturedSnapshot

            withFrameNanos { }
            displayedDarkMode = isDarkMode
            withFrameNanos { }

            maxRadius.floatValue = getMaxRadius(
                center = revealCenter,
                size = viewSize,
            )

            isInputBlocked = false

            var didUnlockToggle = false
            coroutineScope {
                val animationJob = launch {
                    radius.animateTo(
                        targetValue = maxRadius.floatValue,
                        animationSpec = tween(
                            durationMillis = 3600,
                            easing = FastOutSlowInEasing,
                        )
                    )
                }

                launch {
                    while (animationJob.isActive) {
                        withFrameNanos { }

                        val progress = if (maxRadius.floatValue <= 0f) {
                            0f
                        } else {
                            radius.value / maxRadius.floatValue
                        }

                        if (!didUnlockToggle && progress >= TOGGLE_UNLOCK_THRESHOLD) {
                            isTransitionRunning = false
                            didUnlockToggle = true
                            return@launch
                        }
                    }
                }

                animationJob.join()
            }

            if (!didUnlockToggle) {
                isTransitionRunning = false
            }
        } finally {
            snapshot?.recycle()
            snapshot = null
            isInputBlocked = false
            isTransitionRunning = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content(displayedDarkMode)

        snapshot?.let { bitmap ->
            ShaderDarkModeTransitionEffect(
                modifier = Modifier.fillMaxSize(),
                isDarkMode = isDarkMode,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                centerPx = revealCenter,
                radiusPx = radius.value,
                maxRadiusPx = maxRadius.floatValue,
                resolutionPx = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (isInputBlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
            )
        }
    }
}

private fun sanitizeCenter(candidate: Offset, size: Size): Offset {
    val fallback = Offset(size.width / 2f, size.height / 2f)
    val rawCenter = if (
        candidate == Offset.Zero ||
        candidate.x.isFinite().not() ||
        candidate.y.isFinite().not()
    ) {
        fallback
    } else {
        candidate
    }

    return Offset(
        x = rawCenter.x.coerceIn(0f, size.width),
        y = rawCenter.y.coerceIn(0f, size.height),
    )
}

private fun getMaxRadius(center: Offset, size: Size): Float {
    val d1 = hypot(center.x - 0f, center.y - 0f)
    val d2 = hypot(size.width - center.x, center.y - 0f)
    val d3 = hypot(center.x - 0f, size.height - center.y)
    val d4 = hypot(size.width - center.x, size.height - center.y)
    return maxOf(d1, d2, d3, d4)
}
