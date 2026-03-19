package com.vlsch.fluiddarkmode

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlsch.fluiddarkmode.transition.FluidDarkMode
import com.vlsch.fluiddarkmode.transition.FluidDarkModeChangeRequest
import com.vlsch.fluiddarkmode.ui.theme.FluidDarkModeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FluidDarkModeDemoApp(activity = this)
        }
    }
}

@Composable
private fun FluidDarkModeDemoApp(activity: Activity) {
    var isDarkMode by rememberSaveable { mutableStateOf(false) }
    var isTransitionRunning by remember { mutableStateOf(false) }
    var toggleCenter by remember { mutableStateOf(Offset.Zero) }
    var changeRequest by remember { mutableStateOf<FluidDarkModeChangeRequest?>(null) }

    FluidDarkMode(
        activity = activity,
        isDarkMode = isDarkMode,
        fluidDarkModeChangeRequest = changeRequest,
        onTransitionRunningChanged = { isTransitionRunning = it },
    ) { displayedDarkMode ->
        FluidDarkModeTheme(darkTheme = displayedDarkMode) {
            DemoScreen(
                isDarkMode = displayedDarkMode,
                isTransitionRunning = isTransitionRunning,
                onToggle = {
                    changeRequest = FluidDarkModeChangeRequest(
                        id = SystemClock.elapsedRealtimeNanos(),
                        center = toggleCenter,
                    )
                    isDarkMode = !isDarkMode
                },
                onToggleCenterChanged = { toggleCenter = it },
            )
        }
    }
}

@Composable
private fun DemoScreen(
    isDarkMode: Boolean,
    isTransitionRunning: Boolean,
    onToggle: () -> Unit,
    onToggleCenterChanged: (Offset) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = Brush.linearGradient(
        colors = listOf(
            colors.background,
            colors.surface,
            colors.surfaceContainerLowest,
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
                .padding(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Header(
                isDarkMode = isDarkMode,
                isTransitionRunning = isTransitionRunning,
                onToggle = onToggle,
                onToggleCenterChanged = onToggleCenterChanged,
            )
            HeroCard()
            TagRow()
            MetricRow()
            StoryCard(
                title = "Motion Language",
                body = "The sample screen is intentionally small and independent from Serenity. The focus is the AGSL transition, not a copied product UI.",
            )
            StoryCard(
                title = "Integration",
                body = "Tap the toggle to capture the current theme, switch the theme under the hood, and reveal the new surface with the shader-driven snapshot overlay.",
            )
            StoryCard(
                title = "Platform Behavior",
                body = "Android 13 and above use the shader transition. Older devices fall back to an instant switch without crashing.",
            )
        }
    }
}

@Composable
private fun Header(
    isDarkMode: Boolean,
    isTransitionRunning: Boolean,
    onToggle: () -> Unit,
    onToggleCenterChanged: (Offset) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "FluidDarkMode",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open-source sample for a shader-driven dark mode reveal built with Jetpack Compose and AGSL.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        FilledTonalButton(
            onClick = onToggle,
            enabled = !isTransitionRunning,
            modifier = Modifier
                .size(60.dp)
                .semantics {
                    contentDescription = if (isDarkMode) {
                        "Switch to light mode"
                    } else {
                        "Switch to dark mode"
                    }
                }
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    onToggleCenterChanged(
                        Offset(
                            x = position.x + coordinates.size.width / 2f,
                            y = position.y + coordinates.size.height / 2f,
                        )
                    )
                },
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text(
                text = if (isDarkMode) "☀" else "☾",
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HeroCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 10.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Snapshot-hosted reveal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "The transition is packaged as a reusable composable. The demo simply provides theme state, a click origin, and visible content to reveal.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniBadge(label = "AGSL")
                MiniBadge(label = "PixelCopy")
                MiniBadge(label = "Compose")
            }
        }
    }
}

@Composable
private fun TagRow() {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MiniBadge(label = "RuntimeShader")
        MiniBadge(label = "Animated reveal")
        MiniBadge(label = "API 33+ shader")
        MiniBadge(label = "Instant fallback")
    }
}

@Composable
private fun MetricRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            value = "33+",
            label = "AGSL transition",
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            value = "29+",
            label = "Sample app min SDK",
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StoryCard(
    title: String,
    body: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MiniBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DemoPreview() {
    FluidDarkModeTheme(darkTheme = false) {
        DemoScreen(
            isDarkMode = false,
            isTransitionRunning = false,
            onToggle = {},
            onToggleCenterChanged = {},
        )
    }
}
