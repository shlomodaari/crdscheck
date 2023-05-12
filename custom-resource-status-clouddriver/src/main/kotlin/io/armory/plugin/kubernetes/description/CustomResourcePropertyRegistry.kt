package io.armory.plugin.kubernetes.description

import com.netflix.spinnaker.clouddriver.kubernetes.description.GlobalResourcePropertyRegistry
import com.netflix.spinnaker.clouddriver.kubernetes.description.KubernetesResourceProperties
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesKind
import com.netflix.spinnaker.clouddriver.kubernetes.op.handler.KubernetesHandler
import io.armory.plugin.kubernetes.handlers.CustomResourceHandler

class CustomResourcePropertyRegistry(
    handlers: MutableList<KubernetesHandler>,
    defaultHandler: CustomResourceHandler
) : GlobalResourcePropertyRegistry(handlers, defaultHandler) {

    private lateinit var globalProperties: Map<KubernetesKind, KubernetesResourceProperties>
    private lateinit var defaultProperties: KubernetesResourceProperties

    init {
        this.globalProperties = handlers.associate { it.kind() to KubernetesResourceProperties(it, it.versioned()) }
        this.defaultProperties = KubernetesResourceProperties(defaultHandler, defaultHandler.versioned())
    }

    override fun get(kind: KubernetesKind): KubernetesResourceProperties {
        return globalProperties[kind] ?: defaultProperties
    }
}
