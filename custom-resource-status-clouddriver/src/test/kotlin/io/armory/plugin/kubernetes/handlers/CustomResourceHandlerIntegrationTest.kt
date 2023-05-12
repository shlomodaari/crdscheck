package io.armory.plugin.kubernetes.handlers

import com.netflix.spinnaker.clouddriver.api.test.clouddriverFixture
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectCatching
import strikt.assertions.isSuccess

class CustomResourceHandlerIntegrationTest : JUnit5Minutests {
    fun tests() = rootContext<CustomResourceHandlerTestFixture> {
        this.clouddriverFixture {
            CustomResourceHandlerTestFixture()
        }

        test("can fetch handler from application context") {
            expectCatching {
                handlers.first { it.javaClass == CustomResourceHandler::class.java }
            }.isSuccess()
        }
    }
}
