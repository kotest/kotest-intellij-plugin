package io.kotest.plugin.intellij.console

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testDiscovery.JvmToggleAutoTestAction
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.testframework.sm.runner.ui.TestTreeRenderer
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.execution.ExternalSystemExecutionConsoleManager
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.kotest.plugin.intellij.Constants
import io.kotest.plugin.intellij.gradle.GradleUtils
import jetbrains.buildServer.messages.serviceMessages.ServiceMessagesParser
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * An implementation of [ExternalSystemExecutionConsoleManager] that provides a custom [SMTRunnerConsoleView]
 * for displaying Kotest test results when executing tests via the Kotest Gradle plugin tasks.
 */
class KotestExecutionConsoleManager : ExternalSystemExecutionConsoleManager<SMTRunnerConsoleView, ProcessHandler> {

   override fun getExternalSystemId(): ProjectSystemId {
      return GradleConstants.SYSTEM_ID
   }

   /**
    * Provides actions to restart execution task process handled by given console.
    * @param consoleView – is console into which restart actions will be placed.
    */
   override fun getRestartActions(consoleView: SMTRunnerConsoleView): Array<AnAction> {
//      val rerunFailedTestsAction = GradleRerunFailedTestsAction(consoleView)
//      rerunFailedTestsAction.setModelProvider { consoleView.resultsViewer }
      return arrayOf(JvmToggleAutoTestAction())
   }

   @Suppress("UnstableApiUsage", "OverrideOnly")
   override fun attachExecutionConsole(
      project: Project,
      task: ExternalSystemTask,
      env: ExecutionEnvironment?,
      processHandler: ProcessHandler?
   ): SMTRunnerConsoleView? {

      if (env == null) return null
      if (processHandler == null) return null
      val settings = env.runnerAndConfigurationSettings ?: return null

      val consoleProperties = KotestSMTRunnerConsoleProperties(project, settings.configuration, env.executor)

      val splitterPropertyName = SMTestRunnerConnectionUtil.getSplitterPropertyName(Constants.FRAMEWORK_NAME)
      val publisher = project.messageBus.syncPublisher(SMTRunnerEventsListener.TEST_STATUS)
      val console = KotestSMTRunnerConsole(consoleProperties, splitterPropertyName, publisher, project)

      // sets up the process listener on the console view, using the properties that were passed to the console
      SMTestRunnerConnectionUtil.initConsoleView(console, Constants.FRAMEWORK_NAME)

      val testsRootNode = console.resultsViewer.testsRootNode
      testsRootNode.executionId = env.executionId
      testsRootNode.setSuiteStarted()
      console.publisher.onTestingStarted(testsRootNode)

      val testTreeView = console.resultsViewer.treeView
      if (testTreeView != null) {
         val originalRenderer = testTreeView.getCellRenderer() as? TestTreeRenderer
         originalRenderer?.setAdditionalRootFormatter { testProxy: SMTestProxy.SMRootTestProxy, renderer: TestTreeRenderer ->
            if (!testProxy.isInProgress && testProxy.isEmptySuite) {
               renderer.clear()
               renderer.append("No tests were found!")
            }
         }
      }

      // this process handler will set the root proxy node to failed if the process exits with non-zero exit code
      processHandler.addProcessListener(object : ProcessListener {
         override fun processTerminated(event: ProcessEvent) {
            if (testsRootNode.isInProgress) {
               ApplicationManager.getApplication().invokeLater {
                  if (event.exitCode != 0) {
                     testsRootNode.setTestFailed("", null, false)
                  } else {
                     testsRootNode.setFinished()
                  }
                  console.resultsViewer.onBeforeTestingFinished(testsRootNode)
                  console.resultsViewer.onTestingFinished(testsRootNode)
               }
            }
         }
      })

//      if (task instanceof ExternalSystemExecuteTaskTask) {
//            console . addMessageFilter (ReRunTaskFilter((ExternalSystemExecuteTaskTask) task, env));
//      }

      return console
   }

   /**
    * Returns true if this implementation of [ExternalSystemExecutionConsoleManager] should be used to
    * handle the output of the given [task]. We determine true if we are running tests and have the
    * kotest gradle plugin applied to the project
    *
    * This method is invoked for all extensions for each task that is executed by an external system.
    * It is up to this extension to determine if it is applicable for the given task.
    */
   override fun isApplicableFor(task: ExternalSystemTask): Boolean {
      if (task is ExternalSystemExecuteTaskTask) {
         if (task.externalSystemId.id == GradleConstants.SYSTEM_ID.id) {
            if (GradleUtils.hasKotestTask(task.tasksToExecute)) {
               return true
            }

            /**
             * ANDROID STUDIO WORKAROUND - see [AndroidStudioGradleTestTreeFix] for details
             * Remove this block when Android Studio fixes their ijLog parser
             */
            if (AndroidStudioGradleTestTreeFix.shouldHandleTestTask(task.tasksToExecute)) {
               return true
            }
         }
      }
      return false
   }

   override fun onOutput(
      executionConsole: SMTRunnerConsoleView,
      processHandler: ProcessHandler,
      text: String,
      processOutputType: Key<*>, // is stdout or stderr
   ) {
      when (executionConsole) {
         is KotestSMTRunnerConsole -> {
            executionConsole.onOutputHandler.onOutput(executionConsole, text, processOutputType)
         }
      }
   }
}

class KotestConsoleViewOnOutputHandler {

   private var buffer = StringBuilder()

   fun onOutput(console: KotestSMTRunnerConsole, text: String, processOutputType: Key<*>) {
      // ANDROID STUDIO WORKAROUND - see AndroidStudioGradleTestTreeFix.kt for details
      // Remove this block when Android Studio fixes their ijLog parser
      // handleOutput returns true if it handled an ijLog event (don't print raw XML)
      if (AndroidStudioGradleTestTreeFix.handleOutput(console, text)) {
         return
      }

      val startsWith = text.trim().startsWith("##teamcity[")
      val endsWith = text.trim().endsWith("]")
      if (startsWith && endsWith) {
         ServiceMessagesParser().parse(text, KotestServiceMessageCallback(console))
      } else if (startsWith) {
         buffer.clear()
         buffer.append(text)
      } else if (endsWith) {
         buffer.append(text)
         ServiceMessagesParser().parse(buffer.toString(), KotestServiceMessageCallback(console))
         buffer.clear()
      } else if (buffer.isNotEmpty()) {
         buffer.append(text)
      }
   }
}
