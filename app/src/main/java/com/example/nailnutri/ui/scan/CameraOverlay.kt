package com.example.nailnutri.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nailnutri.theme.NutriGreen

@Composable
fun CameraOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val cardWidth = width * 0.5f
        val cardHeight = cardWidth * 1.5f
        val xOffset = (width - cardWidth) / 2
        val yOffset = (height - cardHeight) / 2

        val outerPath = Path().apply {
            addRect(
                androidx.compose.ui.geometry.Rect(0f, 0f, width, height)
            )
        }

        val innerPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = xOffset,
                    top = yOffset,
                    right = xOffset + cardWidth,
                    bottom = yOffset + cardHeight,
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            )
        }

        val resultPath = Path.combine(
            androidx.compose.ui.graphics.PathOperation.Difference,
            outerPath,
            innerPath
        )

        drawPath(
            path = resultPath,
            color = Color.Black.copy(alpha = 0.5f)
        )

        drawRoundRect(
            color = NutriGreen,
            topLeft = androidx.compose.ui.geometry.Offset(xOffset, yOffset),
            size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )

        // 1. 손가락 실루엣 (점선)
        val fingerPath = Path().apply {
            moveTo(xOffset + cardWidth * 0.2f, yOffset + cardHeight)
            quadraticTo(
                xOffset + cardWidth * 0.2f, yOffset + cardHeight * 0.3f,
                xOffset + cardWidth * 0.5f, yOffset + cardHeight * 0.25f
            )
            quadraticTo(
                xOffset + cardWidth * 0.8f, yOffset + cardHeight * 0.3f,
                xOffset + cardWidth * 0.8f, yOffset + cardHeight * 0.9f
            )
            lineTo(xOffset + cardWidth * 0.8f, yOffset + cardHeight)
        }
        drawPath(
            path = fingerPath,
            color = Color.White.copy(alpha = 0.6f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )

        // 2. 손톱 가이드라인 (녹색 실선)
        val nailPath = Path().apply {
            val nailLeft = xOffset + cardWidth * 0.35f
            val nailRight = xOffset + cardWidth * 0.65f
            val nailTop = yOffset + cardHeight * 0.33f
            val nailBottom = yOffset + cardHeight * 0.55f
            
            moveTo(nailLeft, nailBottom)
            quadraticTo(
                nailLeft - 5f, nailTop + 15f,
                xOffset + cardWidth * 0.5f, nailTop
            )
            quadraticTo(
                nailRight + 5f, nailTop + 15f,
                nailRight, nailBottom
            )
            quadraticTo(
                xOffset + cardWidth * 0.5f, nailBottom - 10f,
                nailLeft, nailBottom
            )
        }
        drawPath(
            path = nailPath,
            color = NutriGreen,
            style = Stroke(width = 2.5.dp.toPx())
        )

        // 3. 방향 화살표 (↑)
        val arrowPath = Path().apply {
            val arrowX = xOffset + cardWidth * 0.5f
            val arrowY = yOffset + cardHeight * 0.72f
            moveTo(arrowX, arrowY)
            lineTo(arrowX, arrowY - 40f)
            moveTo(arrowX - 15f, arrowY - 25f)
            lineTo(arrowX, arrowY - 40f)
            lineTo(arrowX + 15f, arrowY - 25f)
        }
        drawPath(
            path = arrowPath,
            color = NutriGreen,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 260.dp)
        ) {
            Text(
                text = "손가락 끝을 녹색 프레임에 맞춰 정렬하세요.",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
