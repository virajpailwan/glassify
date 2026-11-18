package com.glassify.music.ui.glass

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Everything that controls how a glass surface looks. Built from the user's settings. */
@Immutable
data class GlassStyle(
    val blurDp: Float,
    val bendDp: Float,
    val edgeDp: Float,
    val tintColor: Color,
    val tintAlpha: Float,
    val shine: Float,
    val saturation: Float,
    /** 0.25 = glass is computed at quarter size (fast), 1.0 = full size (slow, sharpest). */
    val resolution: Float,
) {
    companion object {
        val Default = GlassStyle(
            blurDp = 4f,
            bendDp = 26f,
            edgeDp = 22f,
            tintColor = Color.White,
            tintAlpha = 0.14f,
            shine = 0.7f,
            saturation = 1.7f,
            resolution = 0.25f,
        )
    }
}

private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private val DarkSurface = Color(0xFF14141C)

/**
 * Turns the user's 0..1 settings into a [GlassStyle]. The numbers follow Convx's liquid glass:
 * very light blur, strong edge bending, boosted colour and a light white tint. Moving "opacity"
 * up fades that light glass into a dark solid panel.
 */
fun glassStyleOf(blur: Float, refraction: Float, opacity: Float, shine: Float, quality: Int) =
    GlassStyle(
        blurDp = mix(0.5f, 14f, blur),
        bendDp = mix(0f, 48f, refraction),
        edgeDp = mix(8f, 32f, refraction),
        tintColor = lerp(Color.White, DarkSurface, ((opacity - 0.3f) / 0.5f).coerceIn(0f, 1f)),
        tintAlpha = mix(0.06f, 0.92f, opacity.coerceIn(0f, 1f).pow(1.8f)),
        shine = shine,
        saturation = 1f + 1.1f * (1f - opacity.coerceIn(0f, 1f)),
        resolution = when (quality) {
            0 -> 0.25f
            1 -> 0.5f
            else -> 1f
        },
    )

/** Turned off while another screen completely covers this one, to save battery and frames. */
val LocalGlassActive = compositionLocalOf { true }

/**
 * The thing glass looks at. Wrap the content that should be visible *through* glass with
 * [glassSource]; then any [glass] element placed on top (as a sibling, not a child) shows a
 * live blurred / refracted copy of it.
 */
@Stable
class GlassBackdrop(val layer: GraphicsLayer) {
    var coordinates: LayoutCoordinates? = null
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier =
    this
        .onGloballyPositioned { backdrop.coordinates = it }
        .drawWithContent {
            backdrop.layer.record(
                this,
                layoutDirection,
                IntSize(size.width.roundToInt(), size.height.roundToInt()),
            ) {
                this@drawWithContent.drawContent()
            }
            drawLayer(backdrop.layer)
        }

/**
 * AGSL shader (Android 13+): bends the picture near the glass edge like a thick lens and
 * splits colours slightly (chromatic aberration).
 */
private const val LENS_SHADER = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float bend;
uniform float edge;
uniform float chroma;

half4 main(float2 p) {
    float2 c = size * 0.5;
    float2 q = abs(p - c) - (c - float2(radius));
    float2 s = sign(p - c);
    float sd = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
    float2 g;
    if (q.x > 0.0 && q.y > 0.0) {
        g = s * normalize(q);
    } else if (q.x > q.y) {
        g = float2(s.x, 0.0);
    } else {
        g = float2(0.0, s.y);
    }
    float depth = clamp(-sd / edge, 0.0, 1.0);
    float t = 1.0 - depth;
    t = t * t;
    float2 base = p - g * (bend * t);
    half4 c0 = content.eval(base);
    half4 c1 = content.eval(base + g * (chroma * t));
    half4 c2 = content.eval(base - g * (chroma * t));
    return half4(c1.r, c0.g, c2.b, c0.a);
}
"""

/** [scale] shrinks every size so the effect runs on fewer pixels. */
private fun buildEffect(
    style: GlassStyle,
    w: Int,
    h: Int,
    radiusPx: Float,
    density: Float,
    scale: Float,
): androidx.compose.ui.graphics.RenderEffect {
    val blurPx = style.blurDp * density * scale
    val matrix = ColorMatrix().apply { setSaturation(style.saturation) }
    val saturation = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))
    var effect: RenderEffect =
        if (blurPx >= 0.5f) {
            RenderEffect.createBlurEffect(blurPx, blurPx, saturation, Shader.TileMode.CLAMP)
        } else {
            saturation
        }

    val bendPx = style.bendDp * density * scale
    if (bendPx >= 0.5f) {
        val shader = RuntimeShader(LENS_SHADER)
        shader.setFloatUniform("size", w.toFloat(), h.toFloat())
        shader.setFloatUniform("radius", radiusPx * scale)
        shader.setFloatUniform("bend", bendPx)
        shader.setFloatUniform("edge", max(style.edgeDp * density * scale, 1f))
        shader.setFloatUniform("chroma", bendPx * 0.08f)
        effect = RenderEffect.createChainEffect(
            RenderEffect.createRuntimeShaderEffect(shader, "content"),
            effect,
        )
    }
    return effect.asComposeRenderEffect()
}

private val FallbackFill = Color(0xCC15151D)

/** Turns this element into a live liquid-glass surface that looks through [backdrop]. */
@Composable
fun Modifier.glass(
    backdrop: GlassBackdrop,
    cornerRadius: Dp,
    style: GlassStyle,
): Modifier {
    val density = LocalDensity.current
    val active = LocalGlassActive.current
    val effectLayer = rememberGraphicsLayer()
    var me by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pxSize by remember { mutableStateOf(IntSize.Zero) }
    val radiusPx = with(density) { cornerRadius.toPx() }
    val res = style.resolution.coerceIn(0.1f, 1f)

    val smallW = max(1, (pxSize.width * res).roundToInt())
    val smallH = max(1, (pxSize.height * res).roundToInt())

    val effect = remember(style, pxSize, radiusPx, density.density) {
        if (pxSize.width <= 0 || pxSize.height <= 0) {
            null
        } else {
            buildEffect(
                style,
                smallW,
                smallH,
                min(radiusPx, min(pxSize.width, pxSize.height) / 2f),
                density.density,
                res,
            )
        }
    }

    return this
        .onGloballyPositioned { me = it }
        .onSizeChanged { pxSize = it }
        .drawWithContent {
            val source = backdrop.coordinates
            val self = me
            val r = min(radiusPx, min(size.width, size.height) / 2f)
            val shape = Path().apply {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
            }

            if (active && effect != null && source != null && source.isAttached &&
                self != null && self.isAttached && size.width > 0f && size.height > 0f
            ) {
                val origin = source.localPositionOf(self, Offset.Zero)
                val sx = smallW / size.width
                val sy = smallH / size.height

                effectLayer.clip = true
                effectLayer.renderEffect = effect
                // Copy of what is behind this element, shrunk to a small size.
                effectLayer.record(this, layoutDirection, IntSize(smallW, smallH)) {
                    scale(sx, sy, pivot = Offset.Zero) {
                        translate(-origin.x, -origin.y) {
                            drawLayer(backdrop.layer)
                        }
                    }
                }

                clipPath(shape) {
                    // Blurred + bent copy, scaled back up to full size.
                    scale(1f / sx, 1f / sy, pivot = Offset.Zero) {
                        drawLayer(effectLayer)
                    }
                    // light tint
                    drawRect(style.tintColor.copy(alpha = style.tintAlpha))
                    // soft light falling from the top, like a glass sheen
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.18f * style.shine),
                                Color.Transparent,
                            ),
                            endY = size.height * 0.55f,
                        ),
                    )
                }
                // thin dark outline so clear glass still reads against bright backgrounds
                drawPath(shape, Color.Black.copy(alpha = 0.32f), style = Stroke(width = 2.4f * density.density))
                // bright glass rim, brighter at the top-left and bottom-right like real glass
                drawPath(
                    shape,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f * style.shine),
                            Color.White.copy(alpha = 0.05f * style.shine),
                            Color.White.copy(alpha = 0.5f * style.shine),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    style = Stroke(width = 1.2f * density.density),
                )
            } else {
                // Cheap stand-in while glass is not ready or is switched off.
                clipPath(shape) { drawRect(FallbackFill) }
                drawPath(shape, Color.White.copy(alpha = 0.18f), style = Stroke(width = 1f * density.density))
            }
            drawContent()
        }
}
