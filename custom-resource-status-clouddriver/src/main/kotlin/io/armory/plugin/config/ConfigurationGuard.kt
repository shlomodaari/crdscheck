package io.armory.plugin.config

import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Component

@Component
class ConfigurationGuard(val configurationProperties: CustomResourceConfigurationProperties) : InitializingBean {

    override fun afterPropertiesSet() {
        configurationProperties.kind?.forEach {
            if (it.value.stable?.failIfNoMatch == true
                && it.value.stable?.markAsUnavailableUntilStable == true
            ) {
                throw IllegalArgumentException("stable.failIfNoMatch and stable.markAsUnavailableUntilStable properties cannot be both set to true.")
            }
        }

        if (configurationProperties.status?.stable?.failIfNoMatch == true
            && configurationProperties.status?.stable?.markAsUnavailableUntilStable == true
        ) {
            throw IllegalArgumentException("stable.failIfNoMatch and stable.markAsUnavailableUntilStable properties cannot be both set to true.")
        }
    }
}