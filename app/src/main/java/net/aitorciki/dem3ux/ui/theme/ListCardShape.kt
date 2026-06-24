package net.aitorciki.dem3ux.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val ListItemOuterCorner = 16.dp
internal val ListItemInnerCorner = 4.dp
internal val ListItemGap = 4.dp
internal val DropdownMenuCorner = 16.dp
internal const val LIST_ITEM_SHAPE_ANIMATION_MILLIS = 250
internal const val LIST_ITEM_COLOR_ANIMATION_MILLIS = 250

@Composable
internal fun animatedListCardShape(
    index: Int,
    count: Int,
    selected: Boolean = false,
): Shape {
    val topCorner = if (selected || count <= 1 || index == 0) ListItemOuterCorner else ListItemInnerCorner
    val bottomCorner = if (selected || count <= 1 || index == count - 1) ListItemOuterCorner else ListItemInnerCorner
    val shapeAnimationSpec =
        tween<Dp>(
            durationMillis = LIST_ITEM_SHAPE_ANIMATION_MILLIS,
            easing = FastOutSlowInEasing,
        )
    val topStart by animateDpAsState(
        targetValue = topCorner,
        animationSpec = shapeAnimationSpec,
        label = "listCardTopStart",
    )
    val topEnd by animateDpAsState(
        targetValue = topCorner,
        animationSpec = shapeAnimationSpec,
        label = "listCardTopEnd",
    )
    val bottomEnd by animateDpAsState(
        targetValue = bottomCorner,
        animationSpec = shapeAnimationSpec,
        label = "listCardBottomEnd",
    )
    val bottomStart by animateDpAsState(
        targetValue = bottomCorner,
        animationSpec = shapeAnimationSpec,
        label = "listCardBottomStart",
    )

    return RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart,
    )
}
