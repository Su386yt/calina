package dev.su386.calina.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.su386.calina.app.App.activeIndex
import kotlinx.coroutines.launch

// TODO: Remove this when jetpack compose v1.9.0-alpha03 is released -- see https://youtrack.jetbrains.com/issue/CMP-8323/Material3-Modifier.badgeBounds-crashes-in-1.9.0-alpha01
private var firstCompose = true
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavRail(vararg iconsData: NavRailIconData) {
    val state = rememberWideNavigationRailState()
    val scope = rememberCoroutineScope()

    WideNavigationRail(
        state = state,
        header = {
            IconButton(
                modifier =
                    Modifier.padding(start = 24.dp).semantics {
                        // The button must announce the expanded or collapsed state of the rail
                        // for accessibility.
                        stateDescription =
                            if (state.currentValue == WideNavigationRailValue.Expanded) {
                                "Expanded"
                            } else {
                                "Collapsed"
                            }
                    },
                onClick = {
                    scope.launch {
                        if (state.targetValue == WideNavigationRailValue.Expanded)
                            state.collapse()
                        else state.expand()
                    }
                },
            ) {
                if (state.targetValue == WideNavigationRailValue.Expanded) {
                    Icon(Icons.AutoMirrored.Filled.MenuOpen, "Collapse rail", tint = colorScheme.onSurface)
                } else {
                    Icon(Icons.Filled.Menu, "Expand rail", tint = colorScheme.onSurface)
                }
            }
        },
    ) {
        if (firstCompose) {
            firstCompose = false
            return@WideNavigationRail
        }

        iconsData.forEachIndexed { index, iconData ->
            WideNavigationRailItem(
                railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                icon = {
                    val imageVector =
                        if (activeIndex == index) {
                            iconData.selectedIcon
                        } else {
                            iconData.icon
                        }
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        tint = colorScheme.onSurface
                    )

                },
                label = { Text(iconData.name) },
                selected = activeIndex == index,
                onClick = { activeIndex = index },
            )
        }
    }
}


data class NavRailIconData (
    val name: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val panel: @Composable () -> Unit,
    val onClick: () -> Unit = {}
)