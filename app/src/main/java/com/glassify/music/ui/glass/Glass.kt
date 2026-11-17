package com.glassify.music.ui.glass

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Everything that controls how a glass surface looks. Built from the user's settings. */
@Immutable
data class GlassStyle(
    val blurDp: Float,
    val bendDp: Float,
    val edgeDp: Float,
    val tintAlpha: Float,
    val shine: Float,
    val saturation: Float,
) {
    companion object {
        val Default = GlassStyle(
            blurDp = 10f,
            bendDp = 25f,
            edgeDp = 20f,
            tintAlpha = 0.3f,
            shine = 0.7f,
            saturation = 1.5f,
        )
    }
}

private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t

fun glassStyleOf(blur: Float, refraction: Float, opacity: Float, shine: Float) = GlassStyle(
    blurDp = mix(1f, 30f, blur),
    bendDp = mix(0f, 40f, refraction),
    edgeDp = mix(8f, 28f, refraction),
    tintAlpha = mix(0.04f, 0.92f, opacity),
    shine = shine,
    saturation = 1f + 0.8f * (1f - opacity),
)

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

private fun buildEffect(
    style: GlassStyle,
    w: Int,
    h: Int,
    radiusPx: Float,
    density: Float,
): androidx.compose.ui.graphics.RenderEffect {
    val blurPx = style.blurDp * density
    val matrix = ColorMatrix().apply { setSaturation(style.saturation) }
    val saturation = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))
    var effect: RenderEffect =
        if (blurPx >= 0.5f) {
            RenderEffect.createBlurEffect(blurPx, blurPx, saturation, Shader.TileMode.CLAMP)
        } else {
            saturation
        }

    val bendPx = style.bendDp * density
    if (bendPx >= 0.5f) {
        val shader = RuntimeShader(LENS_SHADER)
        shader.setFloatUniform("size", w.toFloat(), h.toFloat())
        shader.setFloatUniform("radius", radiusPx)
        shader.setFloatUniform("bend", bendPx)
        shader.setFloatUniform("edge", max(style.edgeDp * density, 1f))
        shader.setFloatUniform("chroma", bendPx * 0.08f)
        effect = RenderEffect.createChainEffect(
            RenderEffect.createRuntimeShaderEffect(shader, "content"),
            effect,
        )
    }
    return effect.asComposeRenderEffect()
}

/** Turns this element into a live liquid-glass surface that looks through [backdrop]. */
@Composable
fun Modifier.glass(
    backdrop: GlassBackdrop,
    cornerRadius: Dp,
    style: GlassStyle,
    tint: Color,
): Modifier {
    val density = LocalDensity.current
    val effectLayer = rememberGraphicsLayer()
    var me by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pxSize by remember { mutableStateOf(IntSize.Zero) }
    val radiusPx = with(density) { cornerRadius.toPx() }

    val effect = remember(style, pxSize, radiusPx, density.density) {
        if (pxSize.width <= 0 || pxSize.height <= 0) {
            null
        } else {
            buildEffect(
                style,
                pxSize.width,
                pxSize.height,
                min(radiusPx, min(pxSize.width, pxSize.height) / 2f),
                density.density,
            )
        }
    }

    return this
        .onGloballyPositioned { me = it }
        .onSizeChanged { pxSize = it }
        .drawWithContent {
            val source = backdrop.coordinates
            val self = me
            if (effect != null && source != null && source.isAttached && self != null && self.isAttached) {
                val origin = source.localPositionOf(self, Offset.Zero)
                val r = min(radiusPx, min(size.width, size.height) / 2f)

                effectLayer.clip = true
                effectLayer.renderEffect = effect
                effectLayer.record(this, layoutDirection, pxSize) {
                    translate(-origin.x, -origin.y) {
                        drawLayer(backdrop.layer)
                    }
                }

                val shape = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r)))
                }
                clipPath(shape) {
                    drawLayer(effectLayer)
                    // colour tint
                    drawRect(tint.copy(alpha = style.tintAlpha))
                    // soft light falling from the top, like a glass sheen
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.16f * style.shine),
                                Color.Transparent,
                            ),
                            endY = size.height * 0.6f,
                        ),
                    )
                }
                // bright glass rim
                drawPath(
                    shape,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f * style.shine),
                            Color.White.copy(alpha = 0.06f * style.shine),
                            Color.White.copy(alpha = 0.45f * style.shine),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    style = Stroke(width = 1.2f * density.density),
                )
            }
            drawContent()
        }
}
