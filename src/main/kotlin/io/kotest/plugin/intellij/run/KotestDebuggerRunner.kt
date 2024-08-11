package io.kotest.plugin.intellij.run

import com.intellij.execution.JavaTestFrameworkDebuggerRunner
import com.intellij.execution.configurations.RunProfile
import io.kotest.plugin.intellij.Constants

@Deprecated("Migrating to Kotest external system runner")
class KotestDebuggerRunner : JavaTestFrameworkDebuggerRunner() {

   override fun validForProfile(profile: RunProfile): Boolean {
      return profile is KotestRunConfiguration
   }

   override fun getThreadName(): String = Constants().FrameworkName
   override fun getRunnerId(): String = "KotestDebug"
}
