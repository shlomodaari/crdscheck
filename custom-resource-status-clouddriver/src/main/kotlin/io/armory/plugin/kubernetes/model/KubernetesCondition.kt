package io.armory.plugin.kubernetes.model

/**
 * @see https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-conditions
 */
data class KubernetesCondition(
    val message: String? = null,
    val reason: String? = null,
    val status: String? = null,
    val type: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KubernetesCondition) return false

        if (message != other.message) return false
        if (reason != other.reason) return false
        if (status != other.status) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + reason.hashCode() + status.hashCode() + type.hashCode()
        return result
    }
}
