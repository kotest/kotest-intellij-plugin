package io.kotest.plugin.intellij.run

import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import io.kotest.plugin.intellij.Constants
import io.kotest.plugin.intellij.Test
import io.kotest.plugin.intellij.psi.enclosingKtClass
import io.kotest.plugin.intellij.styles.SpecStyle
import org.jetbrains.plugins.gradle.execution.GradleRunnerUtil
import org.jetbrains.plugins.gradle.execution.build.CachedModuleDataFinder
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.project.GradleTasksIndices
import org.jetbrains.plugins.gradle.util.GradleModuleData

/**
 * Runs a Kotest individual test using the Kotest custom gradle task.
 */
class GradleTestRunConfigurationProducer : RunConfigurationProducer<GradleRunConfiguration>(true) {

   override fun getConfigurationFactory(): ConfigurationFactory {
      return GradleExternalTaskConfigurationType.getInstance().factory
   }

   /**
    * When two configurations are created from the same context by two different producers, checks if the configuration created by
    * this producer should be discarded in favor of the other one.
    *
    * We always return true because no one else should be creating Kotest configurations.
    */
   override fun isPreferredConfiguration(self: ConfigurationFromContext?, other: ConfigurationFromContext?): Boolean {
      return true
   }

   override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
      return false
   }

   /**
    * This function is called to check if the given context is applicable to this run producer as
    * well as configure the configuration based on the template.
    *
    * In other words, it's used to determine if this run producer should act on the run context that
    * was initialized by the user. In our case, we want to act when the user clicks run on a test case.
    */
   override fun setupConfigurationFromContext(
      configuration: GradleRunConfiguration,
      context: ConfigurationContext,
      sourceElement: Ref<PsiElement>
   ): Boolean {

      if (!hasKotestTask(context.module)) return false

      val element = sourceElement.get()
      if (element != null) {
         val test = findTest(element)
         if (test != null) {

            val spec = element.enclosingKtClass() ?: return false
            val fqn = spec.fqName ?: return false

            val project = context.project ?: return false
            val module = context.module ?: return false

            val externalProjectPath = resolveProjectPath(module) ?: return false
            val location = context.location ?: return false

            val gradleModuleData = CachedModuleDataFinder.getGradleModuleData(module) ?: return false
            val path = gradleModuleData.getTaskPath(Constants().gradleTaskName)

            configuration.name = generateName(spec, test)
            configuration.setDebugServerProcess(false)

            val runManager = RunManager.getInstance(project)
            runManager.setUniqueNameIfNeeded(configuration)

            configuration.settings.externalProjectPath = externalProjectPath
            configuration.settings.taskNames =
               listOf(
                  path,
                  "--tests '${fqn.asString()}'"
               )
            configuration.settings.scriptParameters = ""

            JavaRunConfigurationExtensionManager.instance.extendCreatedConfiguration(configuration, location)
            return true
         }
      }
      return false
   }

   private fun hasKotestTask(module: Module): Boolean {
      val externalProjectPath = resolveProjectPath(module) ?: return false
      return GradleTasksIndices.getInstance(module.project)
         .findTasks(externalProjectPath)
         .any { it.name.endsWith(Constants().gradleTaskName) }
   }

   /**
    * This function is called to check if the given configuration should be re-used.
    * This stops a new context being created every time the user runs the same test.
    */
   override fun isConfigurationFromContext(
      configuration: GradleRunConfiguration,
      context: ConfigurationContext
   ): Boolean {

      if (!hasKotestTask(context.module)) return false

      val element = context.psiLocation
      if (element != null) {
         val test = findTest(element)
         if (test != null) {
            val spec = element.enclosingKtClass()
            return false

//            return configuration.getTestPath() == test.testPath()
//               && configuration.getPackageName().isNullOrBlank()
//               && configuration.getSpecName() == spec?.fqName?.asString()
         }
      }
      return false
   }

   private fun findTest(element: PsiElement): Test? {
      return SpecStyle.styles.asSequence()
         .filter { it.isContainedInSpec(element) }
         .mapNotNull { it.findAssociatedTest(element) }
         .firstOrNull()
   }

   private fun resolveProjectPath(module: Module): String? {
      val gradleModuleData: GradleModuleData = CachedModuleDataFinder.getGradleModuleData(module) ?: return null
      val isGradleProjectDirUsedToRunTasks = gradleModuleData.directoryToRunTask == gradleModuleData.gradleProjectDir
      if (!isGradleProjectDirUsedToRunTasks) {
         return gradleModuleData.directoryToRunTask
      }
      return GradleRunnerUtil.resolveProjectPath(module)
   }
}
