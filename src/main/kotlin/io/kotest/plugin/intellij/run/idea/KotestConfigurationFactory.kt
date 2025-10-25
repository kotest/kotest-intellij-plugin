package io.kotest.plugin.intellij.run.idea

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import io.kotest.plugin.intellij.Constants
import io.kotest.plugin.intellij.run.idea.KotestRunConfiguration

@Deprecated("Starting with Kotest 6 the preferred method is to run via gradle")
class KotestConfigurationFactory(configurationType: ConfigurationType) : ConfigurationFactory(configurationType) {

   override fun createTemplateConfiguration(project: Project): RunConfiguration {
      return KotestRunConfiguration(Constants.FRAMEWORK_NAME, this, project)
   }

   override fun getId(): String = Constants.FRAMEWORK_NAME
}
