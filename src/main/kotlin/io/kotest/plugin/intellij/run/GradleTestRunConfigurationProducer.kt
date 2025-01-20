package io.kotest.plugin.intellij.run

import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import io.kotest.plugin.intellij.Constants
import io.kotest.plugin.intellij.gradle.GradleUtils
import io.kotest.plugin.intellij.psi.enclosingKtClass
import io.kotest.plugin.intellij.styles.SpecStyle
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.plugins.gradle.execution.GradleRunConfigurationProducer
import org.jetbrains.plugins.gradle.execution.build.CachedModuleDataFinder
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

/**
 * Runs a Kotest individual test using the `kotest` gradle task.
 *
 * This uses a [GradleRunConfiguration] which is an intellij provided ExternalSystemRunConfiguration
 * that runs gradle tasks.
 */
class GradleTestRunConfigurationProducer : GradleRunConfigurationProducer() {

   /**
    * When two configurations are created from the same context by two different producers, checks if the
    * configuration created by this producer should be discarded in favor of the other one.
    */
   override fun isPreferredConfiguration(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
      // we need this gradle configuration to take precedence over the previous kotest run that ran a java process directly,
      // but we only want to do this if the user has enabled the kotest gradle plugin
      // we don't have access to the module at this point, but we can assume that the presence of this configuration
      // means the [isConfigurationFromContext] set it up and did the detection for us.
      return true
   }

   /**
    * Returns true if this configuration should replace the other configuration.
    * // todo determine what the logic should be here, sometimes we create a new configuration, sometimes we don't,
    */
   override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
      return false
   }

   /**
    *
    * This function is called to customize the blank [configuration] if it is applicable to the [context].
    * If the given configuration is not something this producer is interested in, then it should return false.
    *
    * Receives a blank configuration of the specified type and a [context] containing information about
    * a source code location (accessible by calling getLocation() or getPsiLocation()). The implementation
    * needs to check whether the location is applicable to the configuration type
    * (e.g., if it is in a file of the supported language). If it is, put the correct context-specific
    * settings into the run configuration and return true.
    *
    * Return false otherwise.
    */
   override fun setupConfigurationFromContext(
      configuration: GradleRunConfiguration,
      context: ConfigurationContext,
      sourceElement: Ref<PsiElement>
   ): Boolean {

      // we must have kotest as a task configured in gradle for this run producer to be applicable
      if (!GradleUtils.hasKotestTask(context.module)) return false

      val project = context.project ?: return false
      val module = context.module ?: return false

      println("setupConfigurationFromContext $configuration $context $sourceElement")

      // we must have the element we clicked on as we are running from the gutter
      val element = sourceElement.get()
      if (element == null) return false

      // if a gradle task was initiated outside a kotest test then we are not interested
      val test = SpecStyle.findTest(element)
      if (test == null) return false

      // we must be in a class, as we need the fully qualified name of the spec
      val spec: KtClass = element.enclosingKtClass() ?: return false

      val externalProjectPath = GradleUtils.resolveProjectPath(module) ?: return false
      val location = context.location ?: return false

      val gradleModuleData = CachedModuleDataFinder.getGradleModuleData(module) ?: return false
      val path = gradleModuleData.getTaskPath(Constants.GRADLE_TASK_NAME)

      configuration.name = GradleTestRunNameBuilder.builder().withSpec(spec).withTest(test).build()
      configuration.isDebugServerProcess = false

      val runManager = RunManager.getInstance(project)
      runManager.setUniqueNameIfNeeded(configuration)

      // note: configuration.settings.externalSystemId is set for us
      configuration.settings.externalProjectPath = externalProjectPath
      configuration.settings.scriptParameters = ""
      configuration.settings.taskNames = KotestTaskPathBuilder.builder(gradleModuleData).withSpec(spec).build()

      JavaRunConfigurationExtensionManager.instance.extendCreatedConfiguration(configuration, location)
      return true
   }

   /**
    * Checks if a configuration was created from the specified context.
    * This method allows reusing an existing run configuration, which applies to the current context,
    * instead of creating a new one and possibly ignoring the user's customizations in the existing one
    */
   override fun isConfigurationFromContext(
      configuration: GradleRunConfiguration,
      context: ConfigurationContext
   ): Boolean {

      // we must have kotest as a task configured in gradle for this run producer to be applicable
      if (!GradleUtils.hasKotestTask(context.module)) return false

      // if kotest is not the task this configuration is running, then this isn't a configuration we can re-use
      // eg, we might be passed another gradle run configuration that was running build or clean etc
      if (configuration.settings.taskNames.firstOrNull() != Constants.GRADLE_TASK_NAME) return false

      println("isConfigurationFromContext $configuration $context")

      val element = context.psiLocation
      if (element != null) {
         val test = SpecStyle.findTest(element)
         if (test != null) {
            val spec = element.enclosingKtClass()
            return false

            // todo we need to compare the test path with the test path from the context
            // for now we'll just make a new one each time as we figure it out

//            return configuration.getTestPath() == test.testPath()
//               && configuration.getPackageName().isNullOrBlank()
//               && configuration.getSpecName() == spec?.fqName?.asString()
         }
      }
      return false
   }
}

