package dev.su386.calina.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.su386.calina.CalinaTheme
import dev.su386.calina.utils.AutoResizeText


@Composable
fun NavRail(modifier: Modifier = Modifier, vararg iconsData: NavRailIconData) {
    Column (
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colors.surface),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in iconsData.indices) {
            NavRailIcon(
                modifier = Modifier
                    .padding(5.dp),
                name = iconsData[i].name,
                icon = iconsData[i].icon,
                iconIndex = i,
                backgroundColor = MaterialTheme.colors.surface,
                activeColor = MaterialTheme.colors.secondary,
                textColor = MaterialTheme.colors.onBackground,
                onClick = iconsData[i].onClick
            )

        }

    }
}

@Composable
fun NavRailIcon (
    modifier: Modifier = Modifier,
    name: String,
    icon: ImageVector,
    iconIndex: Int,
    backgroundColor: Color,
    activeColor: Color,
    textColor: Color,
    onClick: () -> Unit = {}
) {
    CalinaTheme {
        Box(
            modifier
                .aspectRatio(1f)
                .fillMaxSize(.9f)
                .background(
                    color = if (iconIndex == App.activeIndex) {
                        activeColor
                    } else {
                        backgroundColor
                    },
                    shape = RoundedCornerShape(25)
                )
                .clickable(
                    onClick = {
                        App.activeIndex = iconIndex
                        onClick.invoke()
                    },
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
                        .fillMaxHeight(0.25f),
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
    val panel: @Composable () -> Unit,
    val onClick: () -> Unit = {}
)