package io.kotest.plugin.intellij.run

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.execution.ExternalSystemExecutionConsoleManager
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import io.kotest.plugin.intellij.Constants
import io.kotest.plugin.intellij.teamcity.TeamCityMessageParser
import io.kotest.plugin.intellij.teamcity.TeamCityMessageType
import org.jetbrains.plugins.gradle.util.GradleConstants

class KotestTestsExecutionConsoleManager :
   ExternalSystemExecutionConsoleManager<KotestSMTRunnerConsoleView, ProcessHandler> {

   private var handler: TestEventHandler? = null

   override fun getExternalSystemId(): ProjectSystemId {
      return GradleConstants.SYSTEM_ID
   }

   /**
    * Provides actions to restart execution task process witch handled by given console.
    * Params:
    * consoleView – is console into which restart actions will be placed.
    */
   override fun getRestartActions(consoleView: KotestSMTRunnerConsoleView): Array<AnAction> {
      return emptyArray()
   }

   override fun attachExecutionConsole(
      project: Project,
      task: ExternalSystemTask,
      env: ExecutionEnvironment?,
      processHandler: ProcessHandler?
   ): KotestSMTRunnerConsoleView? {

      if (env == null) return null
      val settings = env.runnerAndConfigurationSettings ?: return null

      val conf = settings.configuration as ExternalSystemRunConfiguration
      val consoleProperties = KotestConsoleProperties(conf, env.executor)
      val testFrameworkName = Constants().FrameworkName
      val splitterPropertyName = SMTestRunnerConnectionUtil.getSplitterPropertyName(testFrameworkName)
      val consoleView = KotestSMTRunnerConsoleView(consoleProperties, splitterPropertyName)

      // sets up the process listener on the console view, using the proeprties that were passed to the console
      SMTestRunnerConnectionUtil.initConsoleView(consoleView, testFrameworkName)
      val testsRootNode: SMTestProxy.SMRootTestProxy = consoleView.resultsViewer.testsRootNode

      testsRootNode.executionId = env.executionId
      testsRootNode.setSuiteStarted()

      val publisher = project.messageBus.syncPublisher(SMTRunnerEventsListener.TEST_STATUS)
      handler = TestEventHandler(publisher)
      publisher.onTestingStarted(testsRootNode)

      processHandler?.addProcessListener(object : ProcessAdapter() {
         override fun processTerminated(event: ProcessEvent) {
            if (testsRootNode.isInProgress) {
               ApplicationManager.getApplication().invokeLater {
                  if (event.exitCode == 1) {
                     testsRootNode.setTestFailed("", null, false)
                  } else {
                     testsRootNode.setFinished()
                  }
                  consoleView.resultsViewer.onBeforeTestingFinished(testsRootNode)
                  consoleView.resultsViewer.onTestingFinished(testsRootNode)
               }
            }
         }
      })

      return consoleView
   }

   override fun isApplicableFor(task: ExternalSystemTask): Boolean {
      if (task is ExternalSystemExecuteTaskTask) {
         if (task.externalSystemId.id == GradleConstants.SYSTEM_ID.id) {
            return task.tasksToExecute.any { it.endsWith(Constants().gradleTaskName) }
         }
      }
      return false
   }

   override fun onOutput(
      executionConsole: KotestSMTRunnerConsoleView,
      processHandler: ProcessHandler,
      text: String,
      processOutputType: Key<*>
   ) {
      val message = TeamCityMessageParser().parse(text)
      if (message == null)
         executionConsole.print(text, ConsoleViewContentType.NORMAL_OUTPUT)
      else {
         when (message.type) {
            TeamCityMessageType.TestSuiteStarted ->
               handler?.handleTestSuiteStarted(message, executionConsole)
            TeamCityMessageType.TestSuiteFinished ->
               handler?.handleTestSuiteFinished(message, executionConsole)
            TeamCityMessageType.TestStarted ->
               handler?.handleTestStarted(message, executionConsole)
            TeamCityMessageType.TestFinished ->
               handler?.handleTestFinished(message, executionConsole)
         }
      }
   }
}

