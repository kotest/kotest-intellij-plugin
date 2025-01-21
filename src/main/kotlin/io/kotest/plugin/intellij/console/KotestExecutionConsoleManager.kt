package io.kotest.plugin.intellij.console

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy.SMRootTestProxy
import com.intellij.execution.testframework.sm.runner.events.TestDurationStrategy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.externalSystem.execution.ExternalSystemExecutionConsoleManager
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.kotest.plugin.intellij.Constants
import jetbrains.buildServer.messages.serviceMessages.ServiceMessagesParser
import org.jetbrains.plugins.gradle.util.GradleConstants

//object KotestConsoleFilterProvider: ConsoleFilterProvider {
//   override fun getDefaultFilters(p0: Project): Array<out Filter?> {
//      return arrayOf(object : Filter {
//         override fun applyFilter(p0: String, p1: Int): Filter.Result? {
//            println("applyFilter: $p0 $p1")
//            return Filter.Result
//         }
//      })
//   }
//}

class KotestExecutionConsoleManager : ExternalSystemExecutionConsoleManager<SMTRunnerConsoleView, ProcessHandler> {

   // needs to be defined here so we don't created a new one in the onObject method every time
   private var callback: KotestServiceMessageCallback? = null

   override fun getExternalSystemId(): ProjectSystemId {
      return GradleConstants.SYSTEM_ID
   }

   /**
    * Provides actions to restart execution task process handled by given console.
    * @param consoleView – is console into which restart actions will be placed.
    */
   override fun getRestartActions(consoleView: SMTRunnerConsoleView): Array<AnAction> {
      return emptyArray()
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

      val consoleProperties = KotestSMTRunnerConsoleProperties(settings.configuration, env.executor)

//      val splitterPropertyName = SMTestRunnerConnectionUtil.getSplitterPropertyName(Constants.FrameworkName)
//      val consoleView = KotestSMTRunnerConsoleView(consoleProperties, splitterPropertyName)

      val consoleView = SMTestRunnerConnectionUtil.createConsole(consoleProperties)

      // sets up the process listener on the console view, using the properties that were passed to the console
      SMTestRunnerConnectionUtil.initConsoleView(consoleView, Constants.FRAMEWORK_NAME)

      consoleView.resultsViewer.testsRootNode.executionId = env.executionId
      try {
         // don't know why this method is not public, and cannot figure out how to override it
         // see https://youtrack.jetbrains.com/issue/IJSDK-2340/set-duration-strategy-on-SMRootTestProxy
         val method = SMRootTestProxy::class.java.getDeclaredMethod("setDurationStrategy", TestDurationStrategy::class.java)
         method.isAccessible = true
         method.invoke(consoleView.resultsViewer.testsRootNode, TestDurationStrategy.MANUAL)
      } catch (e: Exception) {
         println(e)
         e.printStackTrace()
      }
      consoleView.resultsViewer.testsRootNode.setSuiteStarted()

      val publisher = project.messageBus.syncPublisher(SMTRunnerEventsListener.TEST_STATUS)
      consoleView.resultsViewer.onSuiteStarted(consoleView.resultsViewer.testsRootNode)
      publisher.onTestingStarted(consoleView.resultsViewer.testsRootNode)

      callback = KotestServiceMessageCallback(consoleView, publisher)

      processHandler.addProcessListener(object : ProcessAdapter() {
         override fun processTerminated(event: ProcessEvent) {
            if (event.exitCode == 1) {
               consoleView.resultsViewer.testsRootNode.setTestFailed("", null, true)
            } else {
               consoleView.resultsViewer.testsRootNode.setFinished()
            }
            consoleView.resultsViewer.onBeforeTestingFinished(consoleView.resultsViewer.testsRootNode)
            publisher.onBeforeTestingFinished(consoleView.resultsViewer.testsRootNode)

            consoleView.resultsViewer.onTestingFinished(consoleView.resultsViewer.testsRootNode)
            publisher.onTestingFinished(consoleView.resultsViewer.testsRootNode)
         }
      })

      return consoleView
   }

   /**
    * Returns true if this [KotestExecutionConsoleManager] should be used to handle the output
    * of the given [ExternalSystemTask]. We determine true if the task is a gradle task
    * that contains the kotest task name.
    *
    * This method is invoked for each task that is executed by the external system.
    * It is up to the extension to determine if it is applicable for the given task.
    */
   override fun isApplicableFor(task: ExternalSystemTask): Boolean {
      if (task is ExternalSystemExecuteTaskTask) {
         if (task.externalSystemId.id == GradleConstants.SYSTEM_ID.id) {
            // todo this should be updated to handle any command line as long as it contains kotest
            return task.tasksToExecute.any {
               it.endsWith(Constants.GRADLE_TASK_NAME)
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
      ServiceMessagesParser().parse(text, callback ?: error("callback must be set"))
//      if (message == null)
//         executionConsole.print(text, ConsoleViewContentType.NORMAL_OUTPUT)
   }
}
