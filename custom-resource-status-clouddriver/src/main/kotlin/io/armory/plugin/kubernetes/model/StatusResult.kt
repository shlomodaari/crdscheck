package io.armory.plugin.kubernetes.model

data class StatusResult(
    val statusMatches: Boolean = false,
    val waitForStable: Boolean = false,
    val message: String? = ""
)
