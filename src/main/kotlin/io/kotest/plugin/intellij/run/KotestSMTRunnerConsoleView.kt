package io.kotest.plugin.intellij.run

import com.intellij.build.BuildViewSettingsProvider
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleViewContentType

class KotestSMTRunnerConsoleView(
   consoleProperties: KotestConsoleProperties,
   splitterPropertyName: String
) : SMTRunnerConsoleView(consoleProperties, splitterPropertyName), BuildViewSettingsProvider {

   private var lastMessageWasEmptyLine = false

   override fun isExecutionViewHidden() = true

   override fun print(s: String, contentType: ConsoleViewContentType) {
      if (detectUnwantedEmptyLine(s)) return;
      super.print(s, contentType);
   }

   // IJ test runner events protocol produces many unwanted empty strings
   // this is a workaround to avoid the trash in the console
   private fun detectUnwantedEmptyLine(s: String): Boolean {
      if (com.intellij.execution.Platform.current().lineSeparator == s) {
         if (lastMessageWasEmptyLine) return true
         lastMessageWasEmptyLine = true
      } else {
         lastMessageWasEmptyLine = false
      }
      return false
   }
}
