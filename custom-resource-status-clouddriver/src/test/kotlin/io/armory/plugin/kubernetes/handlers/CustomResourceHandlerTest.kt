package io.armory.plugin.kubernetes.handlers

import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesApiVersion
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesKind
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesManifest
import com.netflix.spinnaker.clouddriver.kubernetes.model.Manifest
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.armory.plugin.config.CustomResourceConfigurationProperties
import io.armory.plugin.kubernetes.model.KubernetesCondition
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue


class CustomResourceHandlerTest : JUnit5Minutests {

    fun tests() = rootContext<Fixture> {
        fixture {
            Fixture()
        }

        test("No properties set. Should report as stable (default behavior)") {
            val manifestName = "my-cr"
            val manifest = KubernetesManifest().apply {
                put("metadata", mutableMapOf("annotations" to null))
                apiVersion = KubernetesApiVersion.fromString("spinnaker.armory.io/v1alpha2")
                kind = KubernetesKind.fromString("SpinnakerService")
                name = manifestName
            }

            expectThat(nullPropertiesSubject.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isTrue()
            }
        }

        test("Check if stable with condition") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Resource is stable",
                                "reason" to "Reconciling",
                                "status" to "True",
                                "type" to "Ready"
                            )
                        )
                    )
                )
            }

            expectThat(stableSubjectCondition.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isTrue()
                get { stable.message }.isEqualTo("Resource is stable")
                get { failed.isState }.isFalse()
                get { paused.isState }.isFalse()
            }
        }

        test("Check if stable with condition by kind") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("CronTab")
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Resource is stable",
                                "reason" to "Reconciling",
                                "status" to "True",
                                "type" to "Ready"
                            )
                        )
                    )
                )
            }

            expectThat(stableSubjectConditionByKind.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isTrue()
                get { stable.message }.isEqualTo("Resource is stable")
                get { failed.isState }.isFalse()
                get { paused.isState }.isFalse()
            }
        }

        test("Check if stable with custom field") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put("status", mapOf("available" to "True", "ready" to "True"))
            }

            expectThat(stableSubjectField.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isTrue()
                get { stable.message }.isEqualTo(null)
                get { failed.isState }.isFalse()
                get { paused.isState }.isFalse()
            }
        }

        test("Check if stable with custom field by kind") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("CronTab")
                put("status", mapOf("available" to "True", "ready" to "True"))
            }

            expectThat(stableSubjectFieldByKind.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isTrue()
                get { stable.message }.isEqualTo(null)
                get { failed.isState }.isFalse()
                get { paused.isState }.isFalse()
            }
        }

        test("Check if stable when failIfNotStable=true && field is not found") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put("status", mapOf("conditions" to listOf(mapOf("status" to "Ready"))))
            }

            expectThat(stableSubjectFieldFailWhenNotFound.status(manifest)).isA<Manifest.Status>().and {
                get { failed.isState }.isTrue()
                get { failed.message }.isEqualTo(null)
            }
        }

        test("Check if failed with condition") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment failed",
                                "reason" to "ProgressDeadlineExceeded",
                                "status" to "False",
                                "type" to "Ready"
                            )
                        )
                    )
                )
            }

            expectThat(failedSubjectCondition.status(manifest)).isA<Manifest.Status>().and {
                get { failed.isState }.isTrue()
                get { failed.message }.isEqualTo("Deployment failed")
            }
        }

        test("Check if failed with condition by kind") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("CronTab")
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment failed",
                                "reason" to "ProgressDeadlineExceeded",
                                "status" to "False",
                                "type" to "Ready"
                            )
                        )
                    )
                )
            }

            expectThat(failedSubjectConditionByKind.status(manifest)).isA<Manifest.Status>().and {
                get { failed.isState }.isTrue()
                get { failed.message }.isEqualTo("Deployment failed")
            }
        }

        test("Check if failed with condition by kind. Kind does not match so it does not fail") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("Gateway")
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment failed",
                                "reason" to "ProgressDeadlineExceeded",
                                "status" to "False",
                                "type" to "Ready"
                            )
                        )
                    )
                )
            }

            expectThat(failedSubjectConditionByKind.status(manifest)).isA<Manifest.Status>().and {
                get { failed.isState }.isFalse()
                get { failed.message }.isEqualTo(null)
            }
        }

        test("Check if failed with custom field") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put("status", mapOf("available" to "False", "message" to "Deployment failure"))
            }

            expectThat(failedSubjectField.status(manifest)).isA<Manifest.Status>().and {
                get { failed.isState }.isTrue()
                get { failed.message }.isEqualTo("Deployment failed")
            }
        }

        test("Check if paused with condition") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment paused",
                                "reason" to "deploymentpaused",
                                "status" to "False",
                                "type" to "Progressing"
                            )
                        )
                    )
                )
            }


            expectThat(pausedSubjectCondition.status(manifest)).isA<Manifest.Status>().and {
                get { paused.isState }.isTrue()
                get { paused.message }.isEqualTo("Deployment paused")
            }
        }

        test("Check if paused with condition by kind") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("CronTab")
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment paused",
                                "reason" to "deploymentpaused",
                                "status" to "False",
                                "type" to "Progressing"
                            )
                        )
                    )
                )
            }


            expectThat(pausedSubjectConditionByKind.status(manifest)).isA<Manifest.Status>().and {
                get { paused.isState }.isTrue()
                get { paused.message }.isEqualTo("Deployment paused")
            }
        }

        test("Check if paused with custom field") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put("status", mapOf("paused" to "True"))
            }

            expectThat(pausedSubjectField.status(manifest)).isA<Manifest.Status>().and {
                get { paused.isState }.isTrue()
                get { paused.message }.isEqualTo(null)
            }
        }

        test("Check if unavailable with condition") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment unavailable",
                                "reason" to "deploymentunavailable",
                                "status" to "False",
                                "type" to "Ready"
                            )
                        )
                    )
                )
            }

            expectThat(unavailableSubjectCondition.status(manifest)).isA<Manifest.Status>().and {
                get { available.isState }.isFalse()
                get { available.message }.isEqualTo("Deployment unavailable")
            }
        }

        test("Check if unavailable with custom field") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put("status", mapOf("available" to "False"))
            }

            expectThat(unavailableSubjectField.status(manifest)).isA<Manifest.Status>().and {
                get { available.isState }.isFalse()
                get { available.message }.isEqualTo("Deployment unavailable")
            }
        }

        test("Check if unavailable with custom field by kind") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("CronTab")
                put("status", mapOf("available" to "False"))
            }

            expectThat(unavailableSubjectFieldByKind.status(manifest)).isA<Manifest.Status>().and {
                get { available.isState }.isFalse()
                get { available.message }.isEqualTo("Deployment unavailable")
            }
        }

        test("Check if unavailable with custom field with replicas in spec") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put("status", mapOf("available" to "False"))
                put("spec.replicas", "5")
            }

            expectThat(unavailableSubjectField.status(manifest)).isA<Manifest.Status>().and {
                get { available.isState }.isFalse()
                get { available.message }.isEqualTo("Deployment unavailable")
            }
        }

        test("No replicas in status") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put("spec", mapOf("replicas" to 0))
                put("status", mapOf("conditions" to emptyList<Any>()))
            }

            expectThat(nullPropertiesSubject.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isFalse()
                get { stable.message }.isEqualTo("Waiting for all replicas to be updated")
                get { available.isState }.isTrue()
                get { paused.isState }.isFalse()
                get { failed.isState }.isFalse()
            }
        }

        test("No Replicas when none desired") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put(
                    "spec",
                    mapOf(
                        "replicas" to 0,
                        "container" to listOf(
                            mapOf(
                                "image" to "nginx:1.7.9",
                                "imagePullPolicy" to "IfNotPresent",
                                "name" to "nginx"
                            )
                        )
                    )
                )
                put("status", mapOf("conditions" to emptyList<Any>()))
            }

            expectThat(nullPropertiesSubject.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isTrue()
                get { available.isState }.isTrue()
                get { paused.isState }.isFalse()
                get { failed.isState }.isFalse()
            }
        }

        test("Condition reports unavailable") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put(
                    "spec",
                    mapOf(
                        "replicas" to 100,
                        "container" to listOf(
                            mapOf(
                                "image" to "nginx:1.7.9",
                                "imagePullPolicy" to "IfNotPresent",
                                "name" to "nginx"
                            )
                        )
                    )
                )
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment unavailable",
                                "reason" to "deploymentunavailable",
                                "status" to "False",
                                "type" to "Ready"
                            )
                        ),
                        "availableReplicas" to 20,
                        "readyReplicas" to 20,
                        "replicas" to 100,
                        "unavailableReplicas" to 80,
                        "updatedReplicas" to 100
                    )
                )
            }

            expectThat(unavailableSubjectCondition.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isFalse()
                get { stable.message }.isEqualTo("Waiting for all replicas to be available")
                get { available.isState }.isFalse()
                get { paused.isState }.isFalse()
                get { failed.isState }.isFalse()
            }
        }

        test("Awaiting ready") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("SpinnakerService")
                put(
                    "spec",
                    mapOf(
                        "replicas" to 100,
                        "container" to listOf(
                            mapOf(
                                "image" to "nginx:1.7.9",
                                "imagePullPolicy" to "IfNotPresent",
                                "name" to "nginx"
                            )
                        )
                    )
                )
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment has minimum availability.",
                                "reason" to "MinimumReplicasAvailable",
                                "status" to "True",
                                "type" to "Progressing"
                            )
                        ),
                        "availableReplicas" to 100,
                        "readyReplicas" to 62,
                        "replicas" to 100,
                        "unavailableReplicas" to 100,
                        "updatedReplicas" to 100
                    )
                )
            }

            expectThat(unavailableSubjectCondition.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isFalse()
                get { stable.message }.isEqualTo("Waiting for all replicas to be ready")
                get { available.isState }.isTrue()
                get { paused.isState }.isFalse()
                get { failed.isState }.isFalse()
            }
        }

        test("Awaiting termination") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("CronTab")
                put(
                    "spec",
                    mapOf(
                        "replicas" to 100,
                        "container" to listOf(
                            mapOf(
                                "image" to "nginx:1.7.9",
                                "imagePullPolicy" to "IfNotPresent",
                                "name" to "nginx"
                            )
                        )
                    )
                )
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment unavailable",
                                "reason" to "deploymentunavailable",
                                "status" to "False",
                                "type" to "Ready"
                            )
                        ),
                        "availableReplicas" to 75,
                        "readyReplicas" to 75,
                        "replicas" to 114,
                        "unavailableReplicas" to 39,
                        "updatedReplicas" to 100
                    )
                )
            }

            expectThat(unavailableSubjectCondition.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isFalse()
                get { stable.message }.isEqualTo("Waiting for old replicas to finish termination")
                get { available.isState }.isFalse()
                get { paused.isState }.isFalse()
                get { failed.isState }.isFalse()
            }
        }
        test("Replica Count") {
            val manifest = KubernetesManifest().apply {
                apiVersion = KubernetesApiVersion.fromString("spinnaker.io/v1alpha1")
                put("metadata", mutableMapOf("annotations" to null))
                kind = KubernetesKind.fromString("CronTab")
                put(
                    "spec",
                    mapOf(
                        "replicas" to 100,
                        "container" to listOf(
                            mapOf(
                                "image" to "nginx:1.7.9",
                                "imagePullPolicy" to "IfNotPresent",
                                "name" to "nginx"
                            )
                        )
                    )
                )
                put(
                    "status",
                    mapOf(
                        "conditions" to listOf(
                            mapOf(
                                "message" to "Deployment unavailable",
                                "reason" to "deploymentunavailable",
                                "status" to "False",
                                "type" to "Ready"
                            )
                        ),
                        "availableReplicas" to 100,
                        "readyReplicas" to 62,
                        "replicas" to 100,
                        "unavailableReplicas" to 100,
                        "updatedReplicas" to 100
                    )
                )
            }

            expectThat(unavailableSubjectFieldByKind.status(manifest)).isA<Manifest.Status>().and {
                get { stable.isState }.isFalse()
                get { stable.message }.isEqualTo("Waiting for all replicas to be ready")
                get { available.isState }.isTrue()
                get { paused.isState }.isFalse()
                get { failed.isState }.isFalse()
            }
        }

    }

    private class Fixture {
        // Setup multiple types of properties

        // STABLE
        val stableCondition = CustomResourceConfigurationProperties.StatusConfig.Stable(
            conditions = listOf(
                KubernetesCondition(
                    message = "Resource is stable",
                    reason = "Reconciling",
                    status = "True",
                    type = "Ready"
                )
            ),
            fields = null
        )
        val stablePropertiesCondition =
            CustomResourceConfigurationProperties(
                null,
                status = CustomResourceConfigurationProperties.StatusConfig(stableCondition, null, null, null)
            )
        val stableSubjectCondition = CustomResourceHandler(stablePropertiesCondition)

        // Stable by kind
        val stablePropertiesConditionByKind =
            CustomResourceConfigurationProperties(
                kind = mapOf(
                    "CronTab" to CustomResourceConfigurationProperties.StatusConfig(
                        stableCondition,
                        null,
                        null,
                        null
                    )
                ),
                status = null
            )
        val stableSubjectConditionByKind = CustomResourceHandler(stablePropertiesConditionByKind)

        val stableField = CustomResourceConfigurationProperties.StatusConfig.Stable(
            conditions = null,
            fields = listOf(mapOf("status.available" to "True", "status.ready" to "True"))
        )
        val stablePropertiesField =
            CustomResourceConfigurationProperties(
                null,
                status = CustomResourceConfigurationProperties.StatusConfig(stableField, null, null, null)
            )
        val stableSubjectField = CustomResourceHandler(stablePropertiesField)

        val stablePropertiesFieldByKind =
            CustomResourceConfigurationProperties(
                kind = mapOf(
                    "CronTab" to CustomResourceConfigurationProperties.StatusConfig(
                        stableField,
                        null,
                        null,
                        null
                    )
                ),
                status = null
            )
        val stableSubjectFieldByKind = CustomResourceHandler(stablePropertiesFieldByKind)


        val stableFieldFailWhenNotFound = CustomResourceConfigurationProperties.StatusConfig.Stable(
            conditions = null,
            fields = listOf(mapOf("status.available" to "True")),
            failIfNoMatch = true
        )
        val stablePropertiesFieldFailWhenNotFound =
            CustomResourceConfigurationProperties(
                null, CustomResourceConfigurationProperties.StatusConfig(
                    stableFieldFailWhenNotFound,
                    null,
                    null,
                    null
                )
            )
        val stableSubjectFieldFailWhenNotFound = CustomResourceHandler(stablePropertiesFieldFailWhenNotFound)

        // FAILED
        val failedCondition = CustomResourceConfigurationProperties.StatusConfig.Failed(
            conditions = listOf(
                KubernetesCondition(
                    message = "Deployment failed",
                    reason = "ProgressDeadlineExceeded",
                    status = "False",
                    type = "Ready"
                )
            ),
            fields = null
        )
        val failedPropertiesCondition =
            CustomResourceConfigurationProperties(
                null,
                status = CustomResourceConfigurationProperties.StatusConfig(null, failedCondition, null, null)
            )
        val failedSubjectCondition = CustomResourceHandler(failedPropertiesCondition)

        val failedPropertiesConditionByKind =
            CustomResourceConfigurationProperties(
                kind = mapOf(
                    "CronTab" to CustomResourceConfigurationProperties.StatusConfig(
                        null,
                        failedCondition,
                        null,
                        null
                    )
                ),
                status = null
            )
        val failedSubjectConditionByKind = CustomResourceHandler(failedPropertiesConditionByKind)

        val failedField = CustomResourceConfigurationProperties.StatusConfig.Failed(
            conditions = null,
            fields = listOf(mapOf("status.available" to "False", "status.message" to "Deployment failure"))
        )
        val failedPropertiesField = CustomResourceConfigurationProperties(
            null,
            status = CustomResourceConfigurationProperties.StatusConfig(null, failedField, null, null)
        )
        val failedSubjectField = CustomResourceHandler(failedPropertiesField)

        // PAUSED
        val pausedCondition = CustomResourceConfigurationProperties.StatusConfig.Paused(
            conditions = listOf(
                KubernetesCondition(
                    message = "Deployment paused",
                    reason = "deploymentpaused",
                    status = "False",
                    type = "Progressing"
                )
            ),
            fields = null
        )
        val pausedPropertiesCondition =
            CustomResourceConfigurationProperties(
                null,
                status = CustomResourceConfigurationProperties.StatusConfig(null, null, pausedCondition, null)
            )
        val pausedSubjectCondition = CustomResourceHandler(pausedPropertiesCondition)

        val pausedPropertiesConditionByKind =
            CustomResourceConfigurationProperties(
                kind = mapOf(
                    "CronTab" to CustomResourceConfigurationProperties.StatusConfig(
                        null,
                        null,
                        pausedCondition,
                        null
                    )
                ),
                status = null
            )
        val pausedSubjectConditionByKind = CustomResourceHandler(pausedPropertiesConditionByKind)

        val pausedField = CustomResourceConfigurationProperties.StatusConfig.Paused(
            conditions = null,
            fields = listOf(mapOf("status.paused" to "True"))
        )
        val pausedPropertiesField = CustomResourceConfigurationProperties(
            null,
            status = CustomResourceConfigurationProperties.StatusConfig(null, null, pausedField, null)
        )
        val pausedSubjectField = CustomResourceHandler(pausedPropertiesField)

        // UNAVAILABLE
        val unavailableCondition = CustomResourceConfigurationProperties.StatusConfig.Unavailable(
            conditions = listOf(
                KubernetesCondition(
                    message = "Deployment unavailable",
                    reason = "deploymentunavailable",
                    status = "False",
                    type = "Ready"
                )
            ),
            fields = null
        )
        val unavailablePropertiesCondition =
            CustomResourceConfigurationProperties(
                null,
                status = CustomResourceConfigurationProperties.StatusConfig(
                    null,
                    null,
                    null,
                    unavailableCondition
                )
            )
        val unavailableSubjectCondition = CustomResourceHandler(unavailablePropertiesCondition)

        val unavailableField = CustomResourceConfigurationProperties.StatusConfig.Unavailable(
            conditions = null,
            fields = listOf(mapOf("status.available" to "False"))
        )
        val unavailablePropertiesField =
            CustomResourceConfigurationProperties(
                null,
                status = CustomResourceConfigurationProperties.StatusConfig(
                    null,
                    null,
                    null,
                    unavailableField
                )
            )
        val unavailableSubjectField = CustomResourceHandler(unavailablePropertiesField)

        val unavailablePropertiesFieldByKind =
            CustomResourceConfigurationProperties(
                kind = mapOf(
                    "CronTab" to CustomResourceConfigurationProperties.StatusConfig(
                        null,
                        null,
                        null,
                        unavailableField
                    )
                ), status = null
            )
        val unavailableSubjectFieldByKind = CustomResourceHandler(unavailablePropertiesFieldByKind)

        // NULL / No properties set
        val nullProperties =
            CustomResourceConfigurationProperties(null, null)
        val nullPropertiesSubject = CustomResourceHandler(nullProperties)
    }

}
