package io.armory.plugin.config

import io.armory.plugin.kubernetes.model.KubernetesCondition
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding


@ConstructorBinding
@ConfigurationProperties("spinnaker.extensibility.plugins.armory.k8scustomresourcestatus.config")
class CustomResourceConfigurationProperties(
    val kind: Map<String, StatusConfig>?,
    val status: StatusConfig?
) {
    data class StatusConfig(
        val stable: Stable?,
        val failed: Failed?,
        val paused: Paused?,
        val unavailable: Unavailable?
    ) {
        data class Stable(
            val conditions: List<KubernetesCondition>?,
            val fields: List<Map<String, String>?>?,
            val failIfNoMatch: Boolean = false,
            val markAsUnavailableUntilStable: Boolean = false
        )

        data class Failed(
            val conditions: List<KubernetesCondition>?,
            val fields: List<Map<String, String>?>?
        )

        data class Paused(
            val conditions: List<KubernetesCondition>?,
            val fields: List<Map<String, String>?>?
        )

        data class Unavailable(
            val conditions: List<KubernetesCondition>?,
            val fields: List<Map<String, String>?>?
        )
    }

    fun findStableStatusConfig(kindName: String): StatusConfig.Stable? {
        if (kind?.containsKey(kindName) == true) {
            return kind[kindName]?.stable
        }

        return status?.stable
    }

    fun findFailedStatusConfig(kindName: String): StatusConfig.Failed? {
        if (kind?.containsKey(kindName) == true) {
            return kind[kindName]?.failed
        }

        return status?.failed
    }

    fun findPausedStatusConfig(kindName: String): StatusConfig.Paused? {
        if (kind?.containsKey(kindName) == true) {
            return kind[kindName]?.paused
        }

        return status?.paused
    }

    fun findUnavailableStatusConfig(kindName: String): StatusConfig.Unavailable? {
        if (kind?.containsKey(kindName) == true) {
            return kind[kindName]?.unavailable
        }

        return status?.unavailable
    }
}
