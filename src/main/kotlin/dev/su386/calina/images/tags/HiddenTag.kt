package dev.su386.calina.images.tags

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DisabledVisible
import java.util.*

private val uuid = UUID.fromString("f1a6baf8-48ed-4345-9f07-00c0ad432325")

object HiddenTag: SystemTag(
    name = "Hidden",
    uuid = uuid,
    icon = Icons.Outlined.DisabledVisible,
    tagPriority = UByte.MAX_VALUE,
)