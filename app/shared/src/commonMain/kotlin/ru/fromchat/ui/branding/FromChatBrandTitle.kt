package ru.fromchat.ui.branding

import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import ru.fromchat.Res
import ru.fromchat.montserrat_cyrillic
import ru.fromchat.montserrat_cyrillic_ext
import ru.fromchat.montserrat_latin
import ru.fromchat.montserrat_latin_ext
import ru.fromchat.montserrat_vietnamese
import kotlin.math.abs

/**
 * Left → right gradient (like the web header reference), not stretched to parent width:
 * [Modifier.wrapContentWidth] + brush [end] = measured text width from [onTextLayout].
 *
 * Palette aligned with [Web/frontend/src/pages/chat/css/left-panel.module.scss] `.productName`.
 */
private val fromChatTitleGradientStops = arrayOf(
    // 0f to Color(0xFF6366F1),
    // 0.18f to Color(0xFF3B82F6),
    // 0.38f to Color(0xFF9333EA),
    // 0.55f to Color(0xFFA855F7),
    // 0.72f to Color(0xFFD946EF),
    // 0.88f to Color(0xFFEC4899),
    // 1f to Color(0xFFC084FC),
    0f to Color(0xFF6366F1),
    0.2f to Color(0xFF3B82F6),
    0.4f to Color(0xFF9333EA),
    0.6f to Color(0xFFA855F7),
    0.8f to Color(0xFFD946EF),
    1f to Color(0xFFEC4899),
)

@Composable
private fun montserratBoldBrandFamily(): FontFamily = FontFamily(
    // Latin first: Cyrillic-only subsets have no ASCII glyphs, so "FromChat" never used Montserrat.
    Font(Res.font.montserrat_latin, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.montserrat_latin_ext, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.montserrat_cyrillic, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.montserrat_cyrillic_ext, FontWeight.Bold, FontStyle.Normal),
    Font(Res.font.montserrat_vietnamese, FontWeight.Bold, FontStyle.Normal),
)

@Composable
fun FromChatBrandTitle(
    text: String = "FromChat",
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val fontFamily = montserratBoldBrandFamily()
    var textDrawWidthPx by remember(text) { mutableFloatStateOf(0f) }
    val gradientBrush = remember(text, textDrawWidthPx) {
        val w = if (textDrawWidthPx > 1f) textDrawWidthPx else with(density) { 180.dp.toPx() }
        Brush.linearGradient(
            colorStops = fromChatTitleGradientStops,
            start = Offset.Zero,
            end = Offset(w, 0f),
        )
    }
    val shadowBlur = with(density) { 20.dp.toPx() }
    Text(
        text = text,
        modifier = modifier.wrapContentWidth(align = Alignment.Start),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        onTextLayout = { layout ->
            val w = layout.size.width.toFloat()
            if (abs(w - textDrawWidthPx) > 0.5f) {
                textDrawWidthPx = w
            }
        },
        style = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 29.sp,
            lineHeight = 34.sp,
            brush = gradientBrush,
            shadow = Shadow(
                color = Color(0x809333EA),
                offset = Offset.Zero,
                blurRadius = shadowBlur,
            ),
        ),
    )
}
