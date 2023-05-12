package io.armory.plugin.kubernetes.handlers

import com.netflix.spinnaker.clouddriver.Main
import com.netflix.spinnaker.clouddriver.api.test.ClouddriverFixture
import com.netflix.spinnaker.clouddriver.kubernetes.op.handler.KubernetesHandler
import com.netflix.spinnaker.kork.plugins.internal.PluginJar
import io.armory.plugin.KubernetesCustomResourceStatusPlugin
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.io.File

@SpringBootTest(classes = [Main::class])
@TestPropertySource(properties = ["spring.config.location=classpath:clouddriver-test-app.yml"])
class CustomResourceHandlerTestFixture : ClouddriverFixture() {

    @Autowired
    lateinit var handlers: List<KubernetesHandler>

    init {
        val pluginId = "Armory.K8sCustomResourceStatus"
        val plugins = File("build/plugins").also {
            it.delete()
            it.mkdir()
        }

        PluginJar.Builder(plugins.toPath().resolve("$pluginId.jar"), pluginId)
            .pluginClass(KubernetesCustomResourceStatusPlugin::class.java.name)
            .pluginVersion("1.0.0")
            .manifestAttribute("Plugin-Requires", "clouddriver>=0.0.0")
            .build()
    }
}
