package com.glassify.music.ui.neu

import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

object NeuColors {
    val Bg = Color(0xFF202127)
    val Surface = Color(0xFF252630)
    val Raised = Color(0xFF292A35)
    val Inset = Color(0xFF1A1B21)
    val Violet = Color(0xFFA78BFA)
    val Blue = Color(0xFF7C8CFF)
    val TextMain = Color(0xFFF5F5F7)
    val TextDim = Color(0xFF92939F)
    val ShadowDark = Color(0xFF14151A)
    val ShadowLight = Color(0xFF30313E)
}

/** 0 = violet accent, 1 = blue accent. Changing it recolours the whole app. */
var AccentIndex by mutableIntStateOf(0)

val AccentColor: Color
    get() = if (AccentIndex == 0) NeuColors.Violet else NeuColors.Blue

val Accent2Color: Color
    get() = if (AccentIndex == 0) NeuColors.Blue else NeuColors.Violet

/** Soft raised surface: light shadow top-left, dark shadow bottom-right. Draws outside its bounds. */
fun Modifier.neuRaised(
    corner: Dp = 20.dp,
    depth: Dp = 6.dp,
    circle: Boolean = false,
    fill: Color = NeuColors.Raised,
): Modifier = this.drawWithCache {
    val w = size.width
    val h = size.height
    val r = if (circle) size.minDimension / 2f else corner.toPx()
    val blur = depth.toPx() * 1.7f
    val off = depth.toPx()
    val paint = AndroidPaint().apply {
        isAntiAlias = true
        color = fill.toArgb()
        style = AndroidPaint.Style.FILL
    }
    val light = NeuColors.ShadowLight.toArgb()
    val dark = NeuColors.ShadowDark.toArgb()
    val sheen = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent, Color.Black.copy(alpha = 0.14f)),
        start = Offset.Zero,
        end = Offset(w, h),
    )
    onDrawBehind {
        drawIntoCanvas { canvas ->
            paint.setShadowLayer(blur, -off, -off, light)
            canvas.nativeCanvas.drawRoundRect(0f, 0f, w, h, r, r, paint)
            paint.setShadowLayer(blur, off, off, dark)
            canvas.nativeCanvas.drawRoundRect(0f, 0f, w, h, r, r, paint)
        }
        drawRoundRect(brush = sheen, cornerRadius = CornerRadius(r, r))
    }
}

/** Recessed well: dark inside with a shaded top-left edge and a faint lit bottom-right edge. */
fun Modifier.neuInset(
    corner: Dp = 16.dp,
    circle: Boolean = false,
    fill: Color = NeuColors.Inset,
): Modifier = this.drawBehind {
    val r = if (circle) size.minDimension / 2f else corner.toPx()
    drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
    val sw = 3.dp.toPx()
    val ir = max(0f, r - sw / 2f)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.White.copy(alpha = 0.07f)),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        topLeft = Offset(sw / 2f, sw / 2f),
        size = Size(size.width - sw, size.height - sw),
        cornerRadius = CornerRadius(ir, ir),
        style = Stroke(width = sw),
    )
}

@Composable
fun Modifier.plainClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    corner: Dp = 24.dp,
    depth: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.neuRaised(corner, depth), content = content)
}

@Composable
fun NeuButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 22.dp,
    accent: Boolean = false,
    active: Boolean = false,
    description: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, label = "press")
    val tint = when {
        accent -> Color.White
        active -> AccentColor
        else -> NeuColors.TextMain
    }
    val depth = (size.value / 11f).coerceIn(3f, 8f).dp
    val look = when {
        pressed && accent -> Modifier.neuInset(circle = true, fill = lerp(AccentColor, Color.Black, 0.35f))
        pressed -> Modifier.neuInset(circle = true)
        accent -> Modifier.neuRaised(circle = true, depth = depth, fill = AccentColor)
        else -> Modifier.neuRaised(circle = true, depth = depth)
    }
    Box(
        modifier
            .padding(8.dp)
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(look)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun NeuChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val base = modifier.padding(6.dp)
    Box(
        (if (selected) base.neuInset(18.dp) else base.neuRaised(18.dp, 4.dp))
            .clip(RoundedCornerShape(18.dp))
            .plainClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) AccentColor else NeuColors.TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun NeuToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val pos by animateDpAsState(if (checked) 22.dp else 0.dp, label = "toggle")
    Box(
        Modifier
            .padding(4.dp)
            .width(54.dp)
            .height(32.dp)
            .neuInset(16.dp)
            .plainClickable { onChange(!checked) }
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset(x = pos, y = 0.dp)
                .size(24.dp)
                .neuRaised(12.dp, 2.dp, fill = if (checked) AccentColor else NeuColors.Raised),
        )
    }
}

@Composable
fun NeuSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    val change by rememberUpdatedState(onChange)
    val finished by rememberUpdatedState(onFinished)
    val v = value.coerceIn(0f, 1f)
    val thumb = 26.dp
    val thumbPx = with(LocalDensity.current) { thumb.toPx() }
    Box(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { o ->
                    change((o.x / widthPx).coerceIn(0f, 1f))
                    finished()
                })
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { finished() },
                    onDragCancel = { finished() },
                    onHorizontalDrag = { c, _ ->
                        c.consume()
                        change((c.position.x / widthPx).coerceIn(0f, 1f))
                    },
                )
            },
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(16.dp)
                .neuInset(8.dp),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 4.dp)
                .fillMaxWidth(v)
                .height(8.dp)
                .background(
                    Brush.horizontalGradient(listOf(AccentColor, Accent2Color)),
                    RoundedCornerShape(50),
                ),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(((widthPx - thumbPx) * v).roundToInt(), 0) }
                .size(thumb)
                .neuRaised(corner = 13.dp, depth = 3.dp, circle = true),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(8.dp).background(AccentColor, CircleShape))
        }
    }
}

@Composable
fun NeuVSlider(value: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    var heightPx by remember { mutableFloatStateOf(1f) }
    val change by rememberUpdatedState(onChange)
    val v = value.coerceIn(0f, 1f)
    val thumb = 28.dp
    val thumbPx = with(LocalDensity.current) { thumb.toPx() }
    Box(
        modifier
            .width(44.dp)
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { o -> change(1f - (o.y / heightPx).coerceIn(0f, 1f)) })
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { c, _ ->
                        c.consume()
                        change(1f - (c.position.y / heightPx).coerceIn(0f, 1f))
                    },
                )
            },
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .width(14.dp)
                .fillMaxHeight()
                .neuInset(7.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .width(6.dp)
                .fillMaxHeight(v)
                .background(
                    Brush.verticalGradient(listOf(Accent2Color, AccentColor)),
                    RoundedCornerShape(50),
                ),
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, ((1f - v) * (heightPx - thumbPx)).roundToInt()) }
                .size(thumb)
                .neuRaised(corner = 14.dp, depth = 3.dp, circle = true),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(8.dp).background(AccentColor, CircleShape))
        }
    }
}

/** Animated bars next to the player. They move while music plays (decorative, not tied to the audio). */
@Composable
fun Visualizer(playing: Boolean, modifier: Modifier = Modifier, bars: Int = 28) {
    if (playing) {
        val transition = rememberInfiniteTransition(label = "viz")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "phase",
        )
        BarsCanvas(modifier, bars, phase)
    } else {
        BarsCanvas(modifier, bars, -1f)
    }
}

@Composable
private fun BarsCanvas(modifier: Modifier, bars: Int, phase: Float) {
    val a = AccentColor
    val b = Accent2Color
    Canvas(modifier) {
        val gap = 4.dp.toPx()
        val bw = (size.width - gap * (bars - 1)) / bars
        val minH = 4.dp.toPx()
        for (i in 0 until bars) {
            val h = if (phase < 0f) {
                minH
            } else {
                val v = sin(2.0 * PI * (1 + i % 3) * phase + i * 0.9).toFloat() * 0.5f + 0.5f
                minH + (size.height - minH) * (0.15f + 0.85f * v)
            }
            drawRoundRect(
                color = lerp(a, b, i / (bars - 1f)),
                topLeft = Offset(i * (bw + gap), size.height - h),
                size = Size(bw, h),
                cornerRadius = CornerRadius(bw / 2f, bw / 2f),
            )
        }
    }
}
