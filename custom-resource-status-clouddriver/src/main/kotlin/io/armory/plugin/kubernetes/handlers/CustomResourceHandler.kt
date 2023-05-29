package io.armory.plugin.kubernetes.handlers

import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesManifest
import com.netflix.spinnaker.clouddriver.kubernetes.model.Manifest
import com.netflix.spinnaker.clouddriver.kubernetes.op.handler.KubernetesUnregisteredCustomResourceHandler
import io.armory.plugin.config.CustomResourceConfigurationProperties
import io.armory.plugin.kubernetes.model.KubernetesCondition
import io.armory.plugin.kubernetes.model.StatusResult
import io.armory.plugin.kubernetes.model.UnstableReason
import org.apache.commons.beanutils.PropertyUtils
import org.slf4j.LoggerFactory

class CustomResourceHandler(private val properties: CustomResourceConfigurationProperties) :
    KubernetesUnregisteredCustomResourceHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun status(manifest: KubernetesManifest): Manifest.Status {
        val result = Manifest.Status.defaultStatus()

        val statusConditions = getStatusConditions(manifest)
        checkStable(manifest, statusConditions)?.also {
            if (it.waitForStable) {
                // did not find the stable status, but we should wait for the status to appear
                result.unstable(null)
            } else if (!it.statusMatches) {
                // fail because user wants the deployment to fail if stable status is not found
                result.failed(it.message)
            } else if (!it.message.isNullOrEmpty()) {
                // deployment stable and there is a message/reason to set
                result.stable(it.message)
            }
        }
        checkPaused(manifest, statusConditions)?.also {
            if (it.statusMatches) result.paused(it.message)
        }
        checkUnavailable(manifest, statusConditions)?.also {
            if (it.statusMatches) result.unstable(it.message).unavailable(it.message)
        }
        checkFailure(manifest, statusConditions)?.also {
            if (it.statusMatches) result.failed(it.message)
        }
        checkReplicaCounts(manifest)?.also {
            result.unstable(it.message)
        }

        return result
    }

    /**
     * Get a property from Any object.
     * Returns null if no property is found
     *
     * @param any the Kubernetes Manifest
     * @param field the field from the Manifest to get
     * @return Any object
     */
    private fun getProperty(any: Any?, field: String): Any? {
        val property = try {
            PropertyUtils.getProperty(any, field)
        } catch (e: Exception) {
            log.debug("Could not find field: {} in manifest", field)
            return null
        }

        return property
    }

    /**
     * Extract the Manifests' Status in Conditions format.
     * Can return null if the manifest does not have conditions in the status field.
     *
     * @param manifest the Kubernetes Manifest
     * @return A set of [KubernetesCondition]
     */
    private fun getStatusConditions(manifest: KubernetesManifest): Set<KubernetesCondition>? {
        val statusConditionsList =
            getProperty(manifest, "status.conditions") as? List<*>?
                ?: return null

        val statusConditions = mutableSetOf<KubernetesCondition>()

        statusConditionsList.forEach {
            val message = getProperty(it, "message")
            val reason = getProperty(it, "reason")
            val status = getProperty(it, "status")
            val type = getProperty(it, "type")

            statusConditions.add(
                KubernetesCondition(
                    message = message as? String,
                    reason = reason as? String,
                    status = status as? String,
                    type = type as? String
                )
            )
        }

        return statusConditions
    }

    private fun getStatusMessage(kubernetesCondition: KubernetesCondition?): String? {
        if (!kubernetesCondition?.message.isNullOrEmpty()) {
            return kubernetesCondition?.message
        } else if (!kubernetesCondition?.reason.isNullOrEmpty()) {
            return kubernetesCondition?.reason
        }

        return null
    }

    private fun shouldCheckConditions(
        conditions: List<KubernetesCondition>?,
        statusConditions: Set<KubernetesCondition>?
    ): Boolean {
        return !conditions.isNullOrEmpty() && !statusConditions.isNullOrEmpty()
    }

    private fun shouldCheckFields(fields: List<Map<String, String>?>?): Boolean {
        return !fields.isNullOrEmpty()
    }

    private fun statusFieldsMatch(manifest: KubernetesManifest, fieldProperties: List<Map<String, String>?>?): Boolean {
        fieldProperties!!.forEach { propertiesMap ->
            val manifestStatusMap = mutableMapOf<String, String?>()

            // Fill in a Map from the Manifest's values and compare
            propertiesMap?.forEach {
                val manifestField = getProperty(manifest, it.key) as? String
                manifestStatusMap[it.key] = manifestField
            }

            // Properties map matches Manifest
            if (manifestStatusMap == propertiesMap) {
                return true
            }
        }

        return false
    }

    /**
     * Checks in the Manifest if the Custom Resource deployment status is stable
     * Checks for conditions or other fields declared in the properties
     *
     * @param manifest the Kubernetes Manifest
     * @param statusConditions the conditions from the manifest
     * @return a [String] with the reason or message to be used to report as [Manifest.Status.stable]
     */
    private fun checkStable(manifest: KubernetesManifest, statusConditions: Set<KubernetesCondition>?): StatusResult? {
        val stableDeploymentProperties = properties.findStableStatusConfig(manifest.kindName) ?: return null

        val conditionProperties = stableDeploymentProperties.conditions
        val fieldProperties = stableDeploymentProperties.fields

        // Conditions are set and they exist in the manifest
        if (shouldCheckConditions(conditionProperties, statusConditions)) {
            log.debug("Checking Custom Resource conditions for Stable status")
            // Compare K8s Manifest
            val intersect = conditionProperties!!.intersect(statusConditions!!).firstOrNull()

            // if the manifest's condition does not equal the expected stable values it should mark as failed
            // this can mean the manifest didn't have the expected condition
            if (intersect == null && stableDeploymentProperties.failIfNoMatch) {
                log.debug("Did not find Stable Status Condition, marking as Failed.")
                return StatusResult(statusMatches = false)
            } else if (intersect == null && stableDeploymentProperties.markAsUnavailableUntilStable) {
                log.debug("Did not find Stable Status, waiting for Stable deployment.")
                return StatusResult(waitForStable = true)
            }

            val message = getStatusMessage(intersect)

            return StatusResult(statusMatches = true, message = message)
        }

        if (shouldCheckFields(fieldProperties)) {
            log.debug("Checking for Stable Status fields")

            val statusFieldsMatch = statusFieldsMatch(manifest, fieldProperties)

            if (!statusFieldsMatch && stableDeploymentProperties.failIfNoMatch) {
                log.debug("Did not find Stable Status, marking as Failed")
                return StatusResult(statusMatches = false)
            } else if (!statusFieldsMatch && stableDeploymentProperties.markAsUnavailableUntilStable) {
                log.debug("Did not find Stable Status, waiting for Stable deployment.")
                return StatusResult(waitForStable = true)
            }
        }

        return null
    }

    /**
     * Checks in the Manifest if the Custom Resource deployment status is paused
     * Checks for conditions or other fields declared in the properties
     *
     * @param manifest the Kubernetes Manifest
     * @param statusConditions the conditions from the manifest
     * @return a [String] with the reason or message to be used to report as [Manifest.Status.paused]
     */
    private fun checkPaused(manifest: KubernetesManifest, statusConditions: Set<KubernetesCondition>?): StatusResult? {
        val pausedDeploymentProperties = properties.findPausedStatusConfig(manifest.kindName) ?: return null

        val conditionProperties = pausedDeploymentProperties.conditions
        val fieldProperties = pausedDeploymentProperties.fields


        if (shouldCheckConditions(conditionProperties, statusConditions)) {
            log.debug("Checking Custom Resource conditions for Paused status")
            // Compare K8s Manifest
            val intersect = conditionProperties!!.intersect(statusConditions!!).firstOrNull()
                ?: return null

            val message = getStatusMessage(intersect)

            return StatusResult(statusMatches = true, message = message)
        }

        if (shouldCheckFields(fieldProperties)) {
            log.debug("Checking for Paused Status fields")
            if (statusFieldsMatch(manifest, fieldProperties)) {
                return StatusResult(statusMatches = true)
            }
        }

        return null
    }

    /**
     * Checks in the Manifest if the Custom Resource deployment status is unavailable
     * Checks for conditions or other fields declared in the properties
     *
     * @param manifest the Kubernetes Manifest
     * @param statusConditions the conditions from the manifest
     * @return a [String] with the reason or message to be used to report as [Manifest.Status.unavailable]
     */
    private fun checkUnavailable(
        manifest: KubernetesManifest,
        statusConditions: Set<KubernetesCondition>?
    ): StatusResult? {
        val unavailableDeploymentProperties =
            properties.findUnavailableStatusConfig(manifest.kindName) ?: return null

        val conditionProperties = unavailableDeploymentProperties.conditions
        val fieldProperties = unavailableDeploymentProperties.fields


        if (shouldCheckConditions(conditionProperties, statusConditions)) {
            log.debug("Checking Custom Resource conditions for Unavailable status")
            // Compare K8s Manifest
            val intersect = conditionProperties!!.intersect(statusConditions!!).firstOrNull()
                ?: return null

            val message = getStatusMessage(intersect)

            return StatusResult(statusMatches = true, message = message)
        }

        if (shouldCheckFields(fieldProperties)) {
            log.debug("Checking for Unavailable Status fields")
            if (statusFieldsMatch(manifest, fieldProperties)) {
                return StatusResult(statusMatches = true, message = "Deployment unavailable")
            }
        }

        return null
    }

    /**
     * Checks in the Manifest if the Custom Resource deployment has failed
     * Checks for conditions or other fields declared in the properties
     *
     * @param manifest the Kubernetes Manifest
     * @param statusConditions the conditions from the manifest
     * @return a [String] with the reason or message to be used to report as [Manifest.Status.failed]
     */
    private fun checkFailure(manifest: KubernetesManifest, statusConditions: Set<KubernetesCondition>?): StatusResult? {
        val failedDeploymentProperties = properties.findFailedStatusConfig(manifest.kindName) ?: return null

        val conditionProperties = failedDeploymentProperties.conditions
        val fieldProperties = failedDeploymentProperties.fields


        if (shouldCheckConditions(conditionProperties, statusConditions)) {
            log.debug("Checking Custom Resource conditions for Failed status")
            // Compare K8s Manifest
            val intersect = conditionProperties!!.intersect(statusConditions!!).firstOrNull()
                ?: return null

            val message = getStatusMessage(intersect)

            return StatusResult(statusMatches = true, message = message)
        }

        if (shouldCheckFields(fieldProperties)) {
            log.debug("Checking for Failed Status fields")
            if (statusFieldsMatch(manifest, fieldProperties)) {
                return StatusResult(statusMatches = true, message = "Deployment failed")
            }
        }

        return null
    }

    /**
     * Checks that:
     * All the replicas associated with the Deployment have been updated to the latest version you've specified,
     * meaning any updates you've requested have been completed.
     * All the replicas associated with the Deployment are available.
     * No old replicas for the Deployment are running.
     *
     * @param manifest the Kubernetes Manifest
     * @return an [UnstableReason] if the deployment is unstable. Used in [Manifest.Status.unstable]
     */
    private fun checkReplicaCounts(manifest: KubernetesManifest): UnstableReason? {
        val configProperties = CustomResourceConfigurationProperties()
        if(configProperties.kind != null){
            if(configurationProperties.kind?.containsKey(kindName) == true){
                val desiredReplicas = defaultToZero(getProperty(manifest, "spec.replicas"))
                val updatedReplicas = defaultToZero(getProperty(manifest, "status.updatedReplicas"))
                if (updatedReplicas < desiredReplicas) {
                    return UnstableReason.UPDATED_REPLICAS
                }

                val statusReplicas = defaultToZero(getProperty(manifest, "status.replicas"))
                if (statusReplicas > updatedReplicas) {
                    return UnstableReason.OLD_REPLICAS
                }

                val availableReplicas = defaultToZero(getProperty(manifest, "status.availableReplicas"))
                if (availableReplicas < desiredReplicas) {
                    return UnstableReason.AVAILABLE_REPLICAS
                }

                val readyReplicas = defaultToZero(getProperty(manifest, "status.readyReplicas"))
                if (readyReplicas < desiredReplicas) {
                    return UnstableReason.READY_REPLICAS
                }
                return null
            }
        }else{
            val desiredReplicas = defaultToZero(getProperty(manifest, "spec.replicas"))
            val updatedReplicas = defaultToZero(getProperty(manifest, "status.updatedReplicas"))
            if (updatedReplicas < desiredReplicas) {
                return UnstableReason.UPDATED_REPLICAS
            }

            val statusReplicas = defaultToZero(getProperty(manifest, "status.replicas"))
            if (statusReplicas > updatedReplicas) {
                return UnstableReason.OLD_REPLICAS
            }

            val availableReplicas = defaultToZero(getProperty(manifest, "status.availableReplicas"))
            if (availableReplicas < desiredReplicas) {
                return UnstableReason.AVAILABLE_REPLICAS
            }

            val readyReplicas = defaultToZero(getProperty(manifest, "status.readyReplicas"))
            if (readyReplicas < desiredReplicas) {
                return UnstableReason.READY_REPLICAS
            }
            return null
        }

        return null
    }

    private fun defaultToZero(input: Any?): Int {
        return when (input) {
            is Number -> input.toInt()
            else -> 0
        }
    }
}
