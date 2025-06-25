package dev.su386.calina.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import dev.su386.calina.app.App.activeIndex
import dev.su386.calina.app.App.navigationStack
import dev.su386.calina.app.App.panels
import dev.su386.calina.app.App.searchBarContent
import dev.su386.calina.utils.AutoResizeText
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun App() {
    val focusManager = LocalFocusManager.current
    val stack = remember { navigationStack }
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(1f)
                .background(colorScheme.background)
                .focusable(true)
                .onClick {
                    focusManager.clearFocus()
                },
        ) {
            NavRail(
                iconsData = panels.toTypedArray(),
            )

            // Display
            Column(
                horizontalAlignment = Alignment.Start,
            ) {
                Box(modifier = Modifier
                    .padding(5.dp)
                    .fillMaxHeight()
                    .fillMaxWidth()
                ) {
                    NavigationWindow(modifier = Modifier.fillMaxSize().focusable())
                }
            }
        }

        for (popup in stack) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                popup()
            }
        }
    }

}

@Composable
fun NavigationWindow(modifier: Modifier = Modifier) {
    Column(modifier = modifier.focusable(true)) {
        Header(
            heading = panels[activeIndex].name,
            modifier = Modifier.fillMaxWidth()
                .fillMaxHeight(.075f)
                .heightIn(max = 50.dp)
                .padding(4.dp)
        )

        val panels = remember { panels }
        panels[activeIndex].panel()
    }
}

@Composable
fun Header(heading: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AutoResizeText(
            text = heading,
            modifier = Modifier
                .fillMaxHeight()
                .weight(.1f),
            color = colorScheme.onBackground,
        )
        Box(
            Modifier.weight(.2f)
        )
        CalinaSearchBar(
            textFieldState = searchBarContent,
            onSearch = {

            },
            searchResults = listOf(),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 10.dp)
        )
        Box(
            Modifier.weight(.2f)
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(.1f)
                .clip(RoundedCornerShape(25))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun CalinaSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: List<String>,
    modifier: Modifier = Modifier
) {
    // Controls expansion state of the search bar
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val inputField =
        @Composable {
            SearchBarDefaults.InputField(
                modifier = Modifier,
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                placeholder = { Text("Search...") },
                leadingIcon = {
                    if (searchBarState.currentValue == SearchBarValue.Expanded) {
                        IconButton(
                            onClick = { scope.launch { searchBarState.animateToCollapsed() } }
                        ) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                trailingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
            )
        }
    Box(
        modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        TopSearchBar(
            scrollBehavior = scrollBehavior,
            state = searchBarState,
            inputField = inputField,
        )
        ExpandedDockedSearchBar(state = searchBarState, inputField = inputField) {
//            SearchResults(
//                onResultClick = { result ->
//                    textFieldState.setTextAndPlaceCursorAtEnd(result)
//                    scope.launch { searchBarState.animateToCollapsed() }
//                }
//            )
        }
    }
}
object App {
    val panels = listOf(
        NavRailIconData(
            name = "Gallery",
            icon = Icons.Outlined.Photo,
            selectedIcon = Icons.Filled.Photo,
            panel = { GalleryPanel() },
            onClick = { println("Home button clicked") }
        ),
        NavRailIconData(
            name = "Settings",
            icon = Icons.Outlined.Settings,
            selectedIcon = Icons.Filled.Settings,
            panel = { ConfigPanel() },
            onClick = { println("Settings button clicked") }
        ),
    )
    var activeIndex by mutableStateOf(0)
    @OptIn(ExperimentalMaterial3Api::class)
    var searchBarContent by mutableStateOf(TextFieldState(""))
    val navigationStack = mutableStateListOf<@Composable () -> Unit>()

    fun closePopup() {
        if (navigationStack.isNotEmpty()) {
            navigationStack.removeLast()
        } else {
            activeIndex = 0
        }
    }

    fun closeAllPopups() {
        navigationStack.clear()
        activeIndex = 0
    }

    fun openPopup(popupComposable: @Composable () -> Unit) {
        navigationStack.add(popupComposable)
    }
}