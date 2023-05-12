package io.armory.plugin

import com.netflix.spinnaker.kork.plugins.api.spring.SpringLoaderPlugin
import org.pf4j.PluginWrapper
import org.slf4j.LoggerFactory

class KubernetesCustomResourceStatusPlugin(wrapper: PluginWrapper) : SpringLoaderPlugin(wrapper) {

    private val logger = LoggerFactory.getLogger(KubernetesCustomResourceStatusPlugin::class.java)

    override fun getPackagesToScan(): List<String> {
        return listOf(
            "io.armory.plugin"
        )
    }

    override fun start() {
        logger.info("KubernetesCustomResourceStatusPlugin.start()")
    }

    override fun stop() {
        logger.info("KubernetesCustomResourceStatusPlugin.stop()")
    }
}
