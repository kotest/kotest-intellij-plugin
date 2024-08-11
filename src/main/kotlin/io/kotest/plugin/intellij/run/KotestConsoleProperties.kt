package io.kotest.plugin.intellij.run

import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.testframework.JavaAwareTestConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.pom.Navigatable
import io.kotest.plugin.intellij.KotestTestLocator
import javax.swing.tree.TreeSelectionModel

class KotestConsoleProperties(conf: ExternalSystemRunConfiguration, executor: Executor) :
   SMTRunnerConsoleProperties(conf, "kotest", executor) {
   override fun getTestLocator(): SMTestLocator = KotestTestLocator()
   override fun isIdBasedTestTree(): Boolean = true
   override fun isEditable(): Boolean = true
   override fun isPrintTestingStartedTime(): Boolean = true
   override fun getSelectionMode(): Int = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
   override fun getErrorNavigatable(location: Location<*>, stacktrace: String): Navigatable? {
      return JavaAwareTestConsoleProperties.getStackTraceErrorNavigatable(location, stacktrace)
   }

}
