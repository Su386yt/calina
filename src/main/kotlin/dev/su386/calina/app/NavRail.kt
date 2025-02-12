package dev.su386.calina.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.su386.calina.Calina
import dev.su386.calina.CalinaTheme
import dev.su386.calina.utils.AutoResizeText


@Composable
fun NavRail(activeIndex: MutableState<Int>, vararg iconsData: NavRailIconData) {
    Column (
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (iconData in iconsData) {
            NavRailIcon(
                name = iconData.name,
                icon = iconData.icon,
                selected = iconData.active,
                backgroundColor = MaterialTheme.colors.secondary,
                activeColor = MaterialTheme.colors.secondary,
                textColor = MaterialTheme.colors.onSecondary,
                onClick = iconData.onClick
            )

        }

    }
}

@Composable
fun NavRailIcon (
    name: String,
    icon: ImageVector,
    selected: Boolean = false,
    backgroundColor: Color,
    activeColor: Color,
    textColor: Color,
    onClick: () -> Unit = {}
) {
    CalinaTheme {
        Box(
            Modifier
                .aspectRatio(1f)
                .fillMaxSize()
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(10)
                )
                .clickable(
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center // Centers the Column inside the Box
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    tint = textColor
                )

                AutoResizeText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.25f), // Adjust height for better separation
                    color = textColor,
                    text = name
                )
            }
        }
    }

}


data class NavRailIconData (
    val name: String,
    val icon: ImageVector,
    val active: Boolean,
    val onClick: () -> Unit = {}
)