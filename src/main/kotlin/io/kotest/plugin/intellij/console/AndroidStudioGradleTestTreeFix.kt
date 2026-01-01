/**
 * ===================================================================================
 * ANDROID STUDIO WORKAROUND - REMOVE WHEN FIXED
 * ===================================================================================
 *
 * This file contains a workaround for a bug in Android Studio's Gradle test output
 * parser. Android Studio incorrectly renders nested test trees (like Kotest's
 * BehaviorSpec) because it ignores the `parentId` attribute in `<ijLog>` events.
 *
 * IntelliJ IDEA handles this correctly, so this workaround only applies to Android Studio.
 *
 * Bug: When running tests via Gradle in Android Studio, sibling test containers
 * (e.g., multiple `when` blocks in BehaviorSpec) appear incorrectly nested under
 * each other instead of being siblings.
 *
 * HOW TO REMOVE THIS WORKAROUND:
 * 1. Delete this file (AndroidStudioGradleTestTreeFix.kt)
 * 2. In KotestExecutionConsoleManager.kt:
 *    - Remove the import for AndroidStudioGradleTestTreeFix
 *    - In isApplicableFor(), remove the AndroidStudioGradleTestTreeFix.shouldHandleTestTask() call
 *    - In KotestConsoleViewOnOutputHandler.onOutput(), remove the AndroidStudioGradleTestTreeFix.handleOutput() call
 * 3. In KotestSMTRunnerConsole.kt:
 *    - Remove isExecutionViewHidden() override if it returns true
 *
 * ===================================================================================
 */
package io.kotest.plugin.intellij.console

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.ApplicationInfo
import java.util.Base64

/**
 * Workaround for Android Studio's incorrect handling of nested test tree structures.
 * See file header for details.
 */
object AndroidStudioGradleTestTreeFix {

   /**
    * Detects if we're running in Android Studio.
    */
   private fun isAndroidStudio(): Boolean {
      val appName = ApplicationInfo.getInstance().fullApplicationName
      val result = appName.contains("Android Studio", ignoreCase = true)
      return result
   }

   /**
    * Returns true if we should take over handling of standard Gradle test tasks.
    * Only returns true in Android Studio for test-related tasks.
    */
   fun shouldHandleTestTask(tasksToExecute: List<String>): Boolean {
      if (!isAndroidStudio()) {
         return false
      }

      val result = tasksToExecute.any { taskName ->
         taskName.contains("test", ignoreCase = true) || taskName.contains("Test")
      }
      return result
   }

   private var ijLogBuffer = StringBuilder()

   /**
    * Handles Gradle's <ijLog> test event format.
    * Returns true if the output was handled, false if it should be processed normally.
    *
    * NOTE: We must handle ALL event types (beforeSuite, afterSuite, beforeTest, afterTest, onOutput)
    * because by returning true we prevent Android Studio's default handler from seeing the events.
    * If we only handled onOutput, the test tree would be empty.
    */
   fun handleOutput(console: KotestSMTRunnerConsole, text: String): Boolean {
      if (!isAndroidStudio()) {
         return false
      }

      val trimmed = text.trim()

      // Only handle ijLog XML format
      if (!trimmed.contains("<ijLog>") && ijLogBuffer.isEmpty()) {
         return false
      }

      // Buffer ijLog events (they can span multiple lines)
      if (trimmed.contains("<ijLog>")) {
         ijLogBuffer.clear()
         ijLogBuffer.append(text)
      } else if (ijLogBuffer.isNotEmpty()) {
         ijLogBuffer.append(text)
      }

      // Process complete ijLog event
      val buffered = ijLogBuffer.toString()
      if (buffered.contains("</ijLog>")) {
         try {
            parseIjLogEvent(console, buffered)
         } catch (_: Exception) {
            // Ignore parsing errors
         }
         ijLogBuffer.clear()
      }

      return true
   }

   @Suppress("UnstableApiUsage", "OverrideOnly")
   private fun parseIjLogEvent(console: KotestSMTRunnerConsole, text: String) {
      // Extract XML from ijLog wrapper
      val start = text.indexOf("<event")
      val end = text.lastIndexOf("</event>")
      if (start == -1 || end == -1) return

      val xmlContent = text.substring(start, end + "</event>".length)
         .replace("<ijLogEol/>", "")
         .trim()

      val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
      val document = factory.newDocumentBuilder().parse(xmlContent.byteInputStream())
      val eventElement = document.documentElement
      if (eventElement.tagName != "event") return

      val eventType = eventElement.getAttribute("type")
      val testElement = eventElement.getElementsByTagName("test").item(0) as? org.w3c.dom.Element ?: return
      val testId = testElement.getAttribute("id")
      val parentId = testElement.getAttribute("parentId").takeIf { it.isNotBlank() }
      val descriptorElement = testElement.getElementsByTagName("descriptor").item(0) as? org.w3c.dom.Element
      val displayName = descriptorElement?.getAttribute("displayName")
         ?: descriptorElement?.getAttribute("name")
         ?: testId

      // Skip Gradle internal nodes
      if (displayName.startsWith("Gradle Test") || testId.startsWith(":")) return

      val root = console.resultsViewer.testsRootNode

      when (eventType) {
         "onOutput" -> {
            // Extract and print stdout/stderr content
            val outputEvent = testElement.getElementsByTagName("event").item(0) as? org.w3c.dom.Element
            if (outputEvent != null) {
               val base64Content = outputEvent.textContent?.trim()
               if (!base64Content.isNullOrBlank()) {
                  try {
                     val decoded = Base64.getDecoder().decode(base64Content)
                     val text = String(decoded, Charsets.UTF_8)
                     console.print(text, com.intellij.execution.ui.ConsoleViewContentType.NORMAL_OUTPUT)
                  } catch (_: Exception) {
                     // Ignore decode errors
                  }
               }
            }
         }
         "beforeSuite" -> {
            val parent = resolveParent(console, parentId, root)
            val proxy = TestProxyBuilder.builder(displayName, true, parent).build()
            console.addTestProxy(testId, proxy)
            proxy.setSuiteStarted()
            console.resultsViewer.onSuiteStarted(proxy)
            console.publisher.onSuiteStarted(proxy)
         }
         "afterSuite" -> {
            val proxy = console.getTestProxyOrNull(testId) ?: return
            proxy.setFinished()
            console.resultsViewer.onSuiteFinished(proxy)
            console.publisher.onSuiteFinished(proxy)
         }
         "beforeTest" -> {
            val parent = resolveParent(console, parentId, root)
            val proxy = TestProxyBuilder.builder(displayName, false, parent).build()
            console.addTestProxy(testId, proxy)
            proxy.setStarted()
            console.resultsViewer.onTestStarted(proxy)
            console.publisher.onTestStarted(proxy)
         }
         "afterTest" -> {
            val proxy = console.getTestProxyOrNull(testId) ?: return
            val resultElement = eventElement.getElementsByTagName("result").item(0) as? org.w3c.dom.Element
            when (resultElement?.getAttribute("resultType")) {
               "FAILURE" -> {
                  proxy.setTestFailed("Test failed", null, false)
                  console.resultsViewer.onTestFailed(proxy)
                  console.publisher.onTestFailed(proxy)
               }
               "SKIPPED" -> {
                  proxy.setTestIgnored("Test skipped", null)
                  console.resultsViewer.onTestIgnored(proxy)
                  console.publisher.onTestIgnored(proxy)
               }
               else -> {
                  proxy.setFinished()
                  console.resultsViewer.onTestFinished(proxy)
                  console.publisher.onTestFinished(proxy)
               }
            }
         }
      }
   }

   private fun resolveParent(
      console: KotestSMTRunnerConsole,
      parentId: String?,
      root: SMTestProxy.SMRootTestProxy
   ): SMTestProxy {
      if (parentId.isNullOrBlank() || parentId.startsWith(":")) return root
      // IDs like "1.1" for "Gradle Test Executor 1" are internal
      if (parentId.matches(Regex("\\d+\\.\\d+")) && parentId.split(".")[1] == "1") return root
      return console.getTestProxyOrNull(parentId) ?: root
   }
}

