package io.kotest.plugin.intellij.console

import com.intellij.build.BuildViewSettingsProvider
import com.intellij.execution.Platform
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project

/**
 * An [KotestSMTRunnerConsole] is a customized [SMTRunnerConsoleView] for ServiceMessage (ie TeamCity format)
 * based test runners. We are extending this to support hiding the gradle build stages view pane.
 */
@Suppress("UnstableApiUsage")
class KotestSMTRunnerConsole(
   consoleProperties: KotestSMTRunnerConsoleProperties,
   splitterPropertyName: String,
   internal val publisher: SMTRunnerEventsListener,
   private val project: Project,
) : SMTRunnerConsoleView(consoleProperties, splitterPropertyName), BuildViewSettingsProvider {

   private var lastMessageWasEmptyLine = false

   internal val onOutputHandler: KotestConsoleViewOnOutputHandler = KotestConsoleViewOnOutputHandler()

   // each new proxy must be attached to its parent, so we keep a map of test ids to proxies
   // we keep this map here because the console is the object that is passed around by intellij to our callbacks
   private val proxies = mutableMapOf<String, SMTestProxy>()

   /**
    * ANDROID STUDIO WORKAROUND - see [AndroidStudioGradleTestTreeFix] for details
    * Hide execution view in Android Studio to prevent duplicate pane
    * Remove this when Android Studio ijLog parser works as expected
    */
   override fun isExecutionViewHidden(): Boolean {
      val appName = ApplicationInfo.getInstance().fullApplicationName
      return appName.contains("Android Studio", ignoreCase = true)
   }

   override fun print(s: String, contentType: ConsoleViewContentType) {
      if (detectUnwantedEmptyLine(s)) return
      super.print(s, contentType)
   }

   internal fun getTestProxy(testId: String): SMTestProxy {
      return proxies[testId] ?: error("Proxy $testId not found")
   }

   /**
    * Used by [AndroidStudioGradleTestTreeFix] - returns null instead of throwing
    */
   internal fun getTestProxyOrNull(testId: String): SMTestProxy? {
      return proxies[testId]
   }

   internal fun addTestProxy(testId: String, proxy: SMTestProxy) {
      proxies[testId] = proxy
   }

   override fun dispose() {
      proxies.clear()
      super.dispose()
   }

   // IJ test runner events protocol produces many unwanted empty strings
   // this is a workaround to avoid this in the console
   private fun detectUnwantedEmptyLine(s: String): Boolean {
      if (Platform.current().lineSeparator == s) {
         if (lastMessageWasEmptyLine) return true
         lastMessageWasEmptyLine = true
      } else {
         lastMessageWasEmptyLine = false
      }
      return false
   }

   fun notifyError(subtitle: String, message: String) {
      notify(subtitle, message, NotificationType.ERROR)
      NotificationGroupManager.getInstance().getNotificationGroup("kotest.notification.group")
   }

   fun notifyWarn(subtitle: String, message: String) {
      notify(subtitle, message, NotificationType.WARNING)
      NotificationGroupManager.getInstance().getNotificationGroup("kotest.notification.group")
   }

   fun notify(subtitle: String, message: String, type: NotificationType) {
      NotificationGroupManager.getInstance().getNotificationGroup("kotest.notification.group")
         .createNotification(message, type)
         .setTitle("Kotest")
         .setSubtitle(subtitle)
         .notify(project)
   }

}

