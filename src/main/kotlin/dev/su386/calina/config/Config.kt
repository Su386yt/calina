package dev.su386.calina.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.su386.calina.app.LocalHazeState
import dev.su386.calina.utils.AutoResizeText

open class Config(
    name: String,
    description: String
): ConfigOption(
    name,
    description,
) {

    final override val value: Config = this
    private val previousPathMap: MutableMap<String, Array<String>> = mutableMapOf()

    @Composable
    override fun getComposable() {
        getComposable(
            modifier = Modifier,
            sublevel = 0
        )
    }

    @OptIn(ExperimentalHazeMaterialsApi::class)
    @Composable
    fun getComposable(
        modifier: Modifier = Modifier,
        sublevel: Int = 0
    ) {
        val options = remember { this.map }
        val displayName = remember { this.name }
        val description = remember { this.name }

        MaterialTheme {
            when (sublevel) {
                0 -> {
                    LazyColumn(
                        modifier = modifier,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        item {
                            AutoResizeText(
                                text = name,
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth()
                                    .height(50.dp),
                                color = MaterialTheme.colorScheme.onBackground,
                                align = Alignment.CenterStart,
                            )
                        }
                        options.values
                            .sortedBy { it.name }
                            .forEach { option ->
                                item {
                                    if (option is Config) {
                                        option.getComposable(sublevel = sublevel + 1)
                                    }
                                    else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            option.getComposable()
                                        }
                                    }
                                }

                            }
                    }
                }
                1 -> {
                    Box(
                        modifier = modifier
                            .padding(vertical = 3.dp, horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .hazeSource(LocalHazeState.current, zIndex = 2f)
                            .hazeEffect(style = CupertinoMaterials.ultraThin(), state = LocalHazeState.current)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(vertical = 3.dp, horizontal = 16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            AutoResizeText(
                                text = displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                color = MaterialTheme.colorScheme.onBackground,
                                align = Alignment.CenterStart,
                            )

                            Column (
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                options.values
                                    .sortedBy { it.name }
                                    .forEach { option ->
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 5.dp)
                                                .fillMaxWidth()
                                        ) {
                                            if (option is Config) {
                                                option.getComposable(sublevel = sublevel + 1)
                                            }
                                            option.getComposable()
                                        }
                                    }
                            }
                        }
                    }

                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        AutoResizeText(
                            text = displayName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                        )

                        AutoResizeText(
                            text = description,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                        )
                    }
                }
            }

        }
    }

    override fun loadFromJson(jsonNode: JsonElement) {
        val obj = if (jsonNode.isJsonObject) {
            jsonNode as JsonObject
        } else {
            throw IllegalStateException("Json Node is not a config. $jsonNode")
        }

        for (key in obj.keySet()) {
            if (obj.has(key)) {
                map[key]?.loadFromJson(obj[key]) ?: continue
            }
        }

        for (en in previousPathMap.entries) {
            for (previousPath in en.value) {
                val trimmedPath = previousPath.trim('/')
                if ("/" in trimmedPath){
                    val split = trimmedPath.split("/")
                    var newObject = obj
                    for (i in 0..<(split.size - 1)) {
                        if (newObject.has(split[i]) && newObject[split[i]] is JsonObject) {
                            newObject = newObject[split[i]] as JsonObject
                        }
                    }

                    this[en.key.trim('/')].loadFromJson(newObject[split.last()] ?: continue)
                } else {
                    if (obj.has(previousPath)) {
                        this[en.key.trim('/')].loadFromJson(obj[previousPath])
                    }
                }
            }
        }
    }

    override fun saveToJson(): JsonElement {
        val obj = JsonObject()

        for (en in map.entries) {
            obj.add(en.key, en.value.saveToJson())
        }

        return obj
    }

    private val map: MutableMap<String, ConfigOption> = mutableMapOf()

    /**
     * Registers config option [option] at [path]
     * Handles backwards compatibility
     *
     * @param path - Current path the option should be registered to
     * @param option - Option to be registered
     * @param previousPaths - Previous paths to be checked when loading config
     * @throws IllegalArgumentException if path is duplicate
     */
    fun register(path: String, option: ConfigOption, vararg previousPaths: String) {
        if(previousPaths.isNotEmpty()) {
            previousPathMap[path] = previousPaths as Array<String>
        }

        val trimmedPath = path.trim('/')
        if ("/" in trimmedPath) {
            var subconfig = map[trimmedPath.substringBefore("/")]
            if (subconfig == null) {
                subconfig = Config("", "")
                map[trimmedPath.substringBefore("/")] = subconfig
            }

            if (subconfig !is Config){
                throw IllegalArgumentException("Invalid path $path. Item already located at ${path.substringBefore("/")}")
            }
            subconfig.register(trimmedPath.substringAfter("/"), option)
        } else {
            map[trimmedPath] = option
        }
    }

    /**
     * Registers config option [option] at path [path]
     *
     * @param path - Path of the config option the register
     * @param option - Option to register at specified path
     * @see dev.su386.calina.config.Config.register
     */
    operator fun set(path: String, option: ConfigOption) {
        register(path, option)
    }

    /**
     * Accesses config option at specified path [path]
     *
     * @param path - Path to access config option
     * @return Config option that has been registered at specified path
     * @throws IllegalArgumentException if path is invalid
     */
    operator fun get(path: String): ConfigOption {
        val trimmedPath = path.trim('/')
        if ("/" in trimmedPath) {
            val subconfig = map[trimmedPath.substringBefore("/")]
            if (subconfig !is Config) {
                throw IllegalArgumentException("Invalid path $trimmedPath. No subconfig found at ${trimmedPath.substringBefore("/")}")
            }
            return subconfig[trimmedPath.substringAfter("/")]
        } else {
            return map[trimmedPath] ?: throw IllegalArgumentException("Invalid path $trimmedPath. No option found at $trimmedPath.")
        }
    }

    /**
     * Gets option at [path] with specified type [T]
     *
     * @param path - Path to access config option
     * @param T - type to cast
     * @return Config option that has been registered at specified path
     * @throws IllegalArgumentException if path is invalid
     * @see dev.su386.calina.config.Config.get
     */
    fun <T> get(path: String): T {
        return get(path).value()
    }
}