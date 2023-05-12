package io.armory.plugin.config

import com.netflix.spinnaker.clouddriver.kubernetes.op.handler.KubernetesHandler
import com.netflix.spinnaker.kork.plugins.api.spring.ExposeToApp
import io.armory.plugin.kubernetes.description.CustomResourcePropertyRegistry
import io.armory.plugin.kubernetes.handlers.CustomResourceHandler
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@EnableConfigurationProperties(CustomResourceConfigurationProperties::class)
class CustomResourceStatusPluginConfiguration {

    @Bean
    @Primary
    @ExposeToApp
    fun kubernetesUnregisteredCustomResourceHandler(customResourceConfigurationProperties: CustomResourceConfigurationProperties): CustomResourceHandler {
        return CustomResourceHandler(customResourceConfigurationProperties)
    }

    @Bean
    @Primary
    @ExposeToApp
    fun globalResourcePropertyRegistry(
        handlers: MutableList<KubernetesHandler>,
        customResourceHandler: CustomResourceHandler
    ): CustomResourcePropertyRegistry {
        return CustomResourcePropertyRegistry(handlers, customResourceHandler)
    }

}
