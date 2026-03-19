# FluidDarkMode

`FluidDarkMode` is a small Jetpack Compose sample app that demonstrates a fluid shader-driven dark mode switch.

![FluidDarkMode demo](assets/dark_mode_demo.gif)

## What it includes

- `FluidDarkMode` host composable for snapshot capture and reveal orchestration
- `ShaderDarkModeTransitionEffect` AGSL wrapper for the animated overlay
- a simple sample screen that triggers the reveal from a top-right toggle
- fallback behavior for Android 10-12L: instant theme switch with no shader crash; the same visual idea can be supported on older Android versions if the effect is rewritten with OpenGL and rendered through a `SurfaceView`

## Platform support

- Min SDK: 29
- AGSL transition: Android 13+ (API 33+); for lower Android versions, the effect would need an OpenGL-based implementation, typically backed by a `SurfaceView`
- API 29-32: no shader effect, theme changes instantly

## Run the sample

```bash
./gradlew :app:installDebug
```

## Usage

```kotlin
var isDarkMode by rememberSaveable { mutableStateOf(false) }
var request by remember { mutableStateOf<FluidDarkModeChangeRequest?>(null) }
var toggleCenter by remember { mutableStateOf(Offset.Zero) }

FluidDarkMode(
    activity = activity,
    isDarkMode = isDarkMode,
    fluidDarkModeChangeRequest = request,
) { displayedDarkMode ->
    FluidDarkModeTheme(darkTheme = displayedDarkMode) {
        DemoContent(
            onToggle = {
                request = FluidDarkModeChangeRequest(
                    id = SystemClock.elapsedRealtimeNanos(),
                    center = toggleCenter,
                )
                isDarkMode = !isDarkMode
            }
        )
    }
}
```

## Notes
- The procedural `noise` work can also be moved to the CPU and passed into the shader as precomputed input data if you need to improve optimization
