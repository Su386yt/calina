package dev.su386.calina.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.su386.calina.app.App.activeIndex

// TODO: Remove this when jetpack compose v1.9.0-alpha03 is released -- see https://youtrack.jetbrains.com/issue/CMP-8323/Material3-Modifier.badgeBounds-crashes-in-1.9.0-alpha01
private var firstCompose = true
@Composable
fun NavRail(modifier: Modifier = Modifier, vararg iconsData: NavRailIconData) {
    NavigationRail (
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .fillMaxWidth()
            .fillMaxHeight()
            .background(colorScheme.surface),
    ) {
        if (firstCompose) {
            firstCompose = false
            return@NavigationRail
        }
        for (i in iconsData.indices) {
            NavigationRailItem(
                icon = {
                    Icon(
                        iconsData[i].icon,
                        contentDescription = iconsData[i].name,
                    )
                },
                label = { Text(iconsData[i].name) },
                selected = i == activeIndex,
                onClick = { activeIndex = i; iconsData[i].onClick() },
            )
        }
    }
}


data class NavRailIconData (
    val name: String,
    val icon: ImageVector,
    val panel: @Composable () -> Unit,
    val onClick: () -> Unit = {}
)