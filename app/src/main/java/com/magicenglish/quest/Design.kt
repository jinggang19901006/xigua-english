package com.magicenglish.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

object AppColors {
    val Navy = Color(0xFF0C1A30)
    val Header = Color(0xFF173B5A)
    val Panel = Color(0xFF1C2A40)
    val PanelSoft = Color(0xFF25344B)
    val Gold = Color(0xFFFFC83D)
    val Green = Color(0xFF58CC02)
    val GreenDark = Color(0xFF327501)
    val Sky = Color(0xFF1CB0F6)
    val Purple = Color(0xFFA855F7)
    val Coral = Color(0xFFEE5253)
    val Muted = Color(0xFF94A3B8)
}

@Composable
fun TabletCanvas(content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(AppColors.Navy),
        contentAlignment = Alignment.Center
    ) {
        val scale = min(maxWidth.value / 1280f, maxHeight.value / 800f)
        Box(
            modifier = Modifier
                .size(width = (1280f * scale).dp, height = (800f * scale).dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(1280.dp, 800.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                content = content
            )
        }
    }
}

@Composable
fun GameButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AppColors.Green,
    shadow: Color = AppColors.GreenDark,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shadow)
        )
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF475569),
                disabledContentColor = Color(0xFF94A3B8)
            )
        ) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val compact = maxWidth < 140.dp
                val fontSize = when {
                    maxWidth < 90.dp -> 12.sp
                    compact && text.length >= 8 -> 12.sp
                    compact && text.length >= 6 -> 13.sp
                    text.length >= 12 -> 13.sp
                    text.length >= 9 -> 14.sp
                    else -> 16.sp
                }
                Text(
                    text = text,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = if (maxHeight >= 52.dp) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun Modifier.gamePanel(
    background: Color = AppColors.Panel,
    border: Color = Color(0xFF334155),
    radius: Int = 20
): Modifier = clip(RoundedCornerShape(radius.dp))
    .background(background)
    .border(1.dp, border, RoundedCornerShape(radius.dp))

@Composable
fun Pill(text: String, active: Boolean, onClick: (() -> Unit)? = null) {
    val modifier = Modifier
        .clip(RoundedCornerShape(50))
        .background(if (active) AppColors.Gold else Color(0xFF3A4A61))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .size(width = 102.dp, height = 32.dp)
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text,
            color = if (active) AppColors.Navy else Color(0xFFCBD5E1),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}
