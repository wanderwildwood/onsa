package com.wanderwildwood.onsa.ui.plot

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import kotlin.math.max
import kotlin.math.min

private data class PointsCache(
    private var coordinates: LineCoordinates,
    private val maxPointSizeScreen: Float,
    private var transformation: Transformation
) {
    //val path = Path()
    private var numCoordinates = 0
    private var coordinatesScreen = mutableListOf<Offset>()
    private val pointSizeBoxScreen = Rect(Offset.Zero, Size(maxPointSizeScreen, maxPointSizeScreen))

    init {
        _update(coordinates, transformation, init = true)
    }

    fun update(coordinates: LineCoordinates, transformation: Transformation): List<Offset> {
        return _update(coordinates, transformation, init = false)
    }

    private fun _update(coordinates: LineCoordinates, transformation: Transformation, init: Boolean)
            :List<Offset>{
        if (coordinates == this.coordinates && transformation == this.transformation && !init)
            return coordinatesScreen.subList(0, numCoordinates)
        this.coordinates = coordinates
        this.transformation = transformation

        val pointSizeBoxRaw = transformation.toRaw(pointSizeBoxScreen)
        val pointHeightRaw = pointSizeBoxRaw.height
        val pointWidthRaw = pointSizeBoxRaw.width

        //path.rewind()
        val rawMinX = min(transformation.viewPortRaw.right, transformation.viewPortRaw.left) - pointWidthRaw
        val rawMaxX = max(transformation.viewPortRaw.right, transformation.viewPortRaw.left) + pointWidthRaw
        val rawMinY = min(transformation.viewPortRaw.top, transformation.viewPortRaw.bottom) - pointHeightRaw
        val rawMaxY = max(transformation.viewPortRaw.top, transformation.viewPortRaw.bottom) + pointHeightRaw
        numCoordinates = coordinates.coordinates.count {
            it.x in rawMinX..rawMaxX  && it.y in rawMinY .. rawMaxY
        }
        if (numCoordinates > coordinatesScreen.size) {
            var count = 0
            coordinatesScreen = MutableList(numCoordinates) {
                var c = coordinates.coordinates[count]
                while (!(c.x in rawMinX .. rawMaxX && c.y in rawMinY .. rawMaxY)) {
                    count += 1
                    c = coordinates.coordinates[count]
                }
                count += 1
                transformation.toScreen(c)
            }
        } else {
            var count = 0
            coordinates.coordinates.forEach {
                if (it.x in rawMinX..rawMaxX  && it.y in rawMinY .. rawMaxY) {
                    coordinatesScreen[count] = transformation.toScreen(it)
                    count += 1
                }
            }
        }
        return coordinatesScreen.subList(0, numCoordinates)
    }
}

@Composable
fun Points(
    data: LineCoordinates,
    shape: DrawScope.() -> Unit,
    maxPointSizeScreen: Dp,
    transformation: () -> Transformation
) {
    Spacer(modifier = Modifier
        .fillMaxSize()
        .drawWithCache {
            val cachedData = PointsCache(data, maxPointSizeScreen.toPx(), transformation())
            onDrawBehind {
                val points = cachedData.update(data, transformation())
                points.forEach { p ->
                    translate(p.x, p.y) {
                        this.shape()
                    }
                }
            }
        }
    )
}

@Composable
private fun rememberTransformation(
    screenWidth: Dp, screenHeight: Dp,
    viewPortRaw: Rect
): Transformation {
    val widthPx = with(LocalDensity.current) { screenWidth.roundToPx() }
    val heightPx = with(LocalDensity.current) { screenHeight.roundToPx() }

    val transformation = remember(widthPx, heightPx, viewPortRaw) {
        Transformation(IntRect(0, 0, widthPx, heightPx), viewPortRaw)
    }
    return transformation
}

@Preview(widthDp = 200, heightDp = 200, showBackground = true)
@Composable
private fun PointsPreview() {
    TunerTheme {
        BoxWithConstraints {
            val x = remember { floatArrayOf(0f, 1f, 2f, 3f, 4f, 5.03f) }
            val y = remember { floatArrayOf(3f, 1f, 2f, -2f, 0f, -3.01f) }
            val coords = remember { LineCoordinates.create(
                size = x.size, x = { x[it] }, y = { y[it] }
            )
//                LineCoordinates(
//                    size = 5, x = { x[it] }, y = { y[it] }
//                )
            }
            val transformation = rememberTransformation(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                viewPortRaw = Rect(-1f, 5f, 5f, -3f)
            )

            Points(
                coords,
                 PointShape.circle(6.dp, MaterialTheme.colorScheme.error),
                6.dp,
                { transformation }
            )
        }
    }
}
