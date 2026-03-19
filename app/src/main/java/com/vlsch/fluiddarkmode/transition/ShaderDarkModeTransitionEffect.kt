package com.vlsch.fluiddarkmode.transition

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.max

private const val DARK_MODE_TRANSITION_SHADER = """
uniform shader inputImage;
uniform float2 center;
uniform float radius;
uniform float maxRadius;
uniform float feather;
uniform float2 resolution;
uniform float time;
uniform float isDarkening;

float hash(float2 uv) {
    return fract(sin(dot(uv, vec2(127.5, 315.10))) * 45148.43);
}

float noise(float2 uv) {
    float2 cellId       = floor(uv);
    float2 cellPosition = fract(uv);

    float bottomLeft  = hash(cellId + float2(0.0, 0.0));
    float bottomRight = hash(cellId + float2(1.0, 0.0));
    float topLeft     = hash(cellId + float2(0.0, 1.0));
    float topRight    = hash(cellId + float2(1.0, 1.0));

    float2 u = cellPosition * cellPosition * (3.0 - 2.0 * cellPosition);

    float bottom = mix(bottomLeft, bottomRight, u.x);
    float top    = mix(topLeft, topRight, u.x);

    return mix(bottom, top, u.y);
}

float fbmNormalized(float2 uv) {
    float total = 0.0;
    float amplitude = 0.5;
    float weight = 0.0;

    total += noise(uv) * amplitude;
    weight += amplitude;
    amplitude *= 0.5;

    total += noise(uv * 2.0 + float2(17.0, 9.0)) * amplitude;
    weight += amplitude;
    amplitude *= 0.5;

    total += noise(uv * 4.0 + float2(31.0, 15.0)) * amplitude;
    weight += amplitude;
    amplitude *= 0.5;

    total += noise(uv * 8.0 + float2(47.0, 23.0)) * amplitude;
    weight += amplitude;

    return total / max(weight, 0.0001);
}

float sdfCircle(float2 uv, float radius) {
    return length(uv) - radius; //  < 0 inside, = 0 edge, > 0 outside
}

half4 main(float2 fragCoord) {
    half2 uv = fragCoord;

    float warpMargin = 160.0;
    float normalizedRadius = (radius - 0.0) / (max(maxRadius, 0.0001) - 0.0);
    float u = clamp(normalizedRadius, 0.0, 1.0);
    float remapped = u * warpMargin;

    float warpMarginGate = smoothstep(0.85, 1.0, normalizedRadius);
    float effectiveWarpMargin = warpMargin * warpMarginGate;
    float2 circleCenter = uv - center;
    float circle = sdfCircle(circleCenter, radius + effectiveWarpMargin);

    float lenP = max(length(circleCenter), 0.0001);
    float2 dir = circleCenter / lenP;
    float angle = atan(circleCenter.y, circleCenter.x);
    float noiseValue = fbmNormalized(dir * 20.0 + float2(time * 0.15, 0.0));
    float noiseValue2 = fbmNormalized(circleCenter * 0.03 + float2(time * 0.35, -time * 0.2));
    float warp = ((noiseValue - 0.5) * 0.65 + (noiseValue2 - 0.5) * 0.35) * 2.0 * remapped;

    circle += warp;

    float baseWidth = 120.0;
    float wave = 1.0 - smoothstep(0.0, baseWidth, abs(circle));
    float waveFront = wave;
    float strength = 40.0;
    float2 displacementOffset = dir * strength * waveFront;

    float safeFeather = max(feather, 0.001);
    float keepAlpha = smoothstep(-safeFeather, safeFeather, circle);

    float2 sampleUV = fragCoord + displacementOffset;
    sampleUV = clamp(sampleUV, float2(0.5), resolution - float2(0.5));
    float chromaticBase = 2.0;
    float chromaticGate = smoothstep(0.05, 0.2, normalizedRadius);
    float2 caOffset = dir * (chromaticBase * chromaticGate) * waveFront;
    half4 textureR = inputImage.eval(clamp(sampleUV + caOffset, float2(0.5), resolution - float2(0.5)));
    half4 textureG = inputImage.eval(sampleUV);
    half4 textureB = inputImage.eval(clamp(sampleUV - caOffset, float2(0.5), resolution - float2(0.5)));
    half4 texture = half4(textureR.r, textureG.g, textureB.b, textureG.a);

    float glowBrightness = 0.65;
    float glowGate = smoothstep(0.05, 0.2, normalizedRadius);
    half glow = half(waveFront * glowBrightness * glowGate);
    half3 brightened = min(texture.rgb + half3(glow), half3(1.0));
    half3 darkened = max(texture.rgb - half3(glow), half3(0.0));
    texture.rgb = mix(brightened, darkened, half(isDarkening));
    half4 result = texture * half(keepAlpha);
    return result;
}
"""

@Composable
fun ShaderDarkModeTransitionEffect(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    enabled: Boolean,
    centerPx: Offset,
    radiusPx: Float,
    maxRadiusPx: Float,
    resolutionPx: Size,
    featherPx: Float = 1.5f,
    content: @Composable () -> Unit,
) {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Box(modifier = modifier) { content() }
        return
    }

    ShaderDarkModeTransitionEffectApi33(
        modifier = modifier,
        isDarkMode = isDarkMode,
        centerPx = centerPx,
        radiusPx = radiusPx,
        maxRadiusPx = maxRadiusPx,
        resolutionPx = resolutionPx,
        featherPx = featherPx,
        content = content,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderDarkModeTransitionEffectApi33(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    centerPx: Offset,
    radiusPx: Float,
    maxRadiusPx: Float,
    resolutionPx: Size,
    featherPx: Float,
    content: @Composable () -> Unit,
) {
    val shader = remember { RuntimeShader(DARK_MODE_TRANSITION_SHADER) }
    val safeWidth = max(resolutionPx.width, 1f)
    val safeHeight = max(resolutionPx.height, 1f)

    val time by produceState(0f) {
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            value = (now - start) / 1_000_000_000f
        }
    }

    shader.setFloatUniform("center", centerPx.x, centerPx.y)
    shader.setFloatUniform("radius", radiusPx)
    shader.setFloatUniform("feather", max(featherPx, 0.001f))
    shader.setFloatUniform("resolution", safeWidth, safeHeight)
    shader.setFloatUniform("maxRadius", maxRadiusPx)
    shader.setFloatUniform("time", time)
    shader.setFloatUniform("isDarkening", if (isDarkMode) 1f else 0f)

    val renderEffect = RenderEffect
        .createRuntimeShaderEffect(shader, "inputImage")
        .asComposeRenderEffect()

    Box(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            this.renderEffect = renderEffect
        }
    ) {
        content()
    }
}
