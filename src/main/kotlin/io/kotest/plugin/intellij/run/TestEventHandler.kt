package io.kotest.plugin.intellij.run

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import io.kotest.plugin.intellij.KotestTestLocator
import io.kotest.plugin.intellij.teamcity.TeamCityMessage

class TestEventHandler(private val publisher: SMTRunnerEventsListener) {

   private val proxies = mutableMapOf<String, SMTestProxy>()

   fun handleTestSuiteStarted(message: TeamCityMessage, console: KotestSMTRunnerConsoleView) {
      val id = message["id"] ?: return
      // parentId is null for top level tests in a spec
      val parentId = message["parent_id"]
      val name = message["name"] ?: return
      val location = message["locationHint"]
      println(message)

      val root = console.resultsViewer.testsRootNode

      val proxy = SMTestProxy(name, true, location)
      proxy.setLocator(KotestTestLocator())

      val parent = if (parentId == null) root else proxies[parentId] ?: root

      parent.addChild(proxy)
      proxy.setSuiteStarted()

      proxies[id] = proxy
      console.resultsViewer.onSuiteStarted(proxy)
      publisher.onSuiteStarted(proxy)
   }

   fun handleTestSuiteFinished(message: TeamCityMessage, console: KotestSMTRunnerConsoleView) {
      val id = message["id"] ?: return
      val proxy = proxies[id] ?: return
      proxy.setFinished()
      console.resultsViewer.onSuiteFinished(proxy)
      publisher.onSuiteFinished(proxy)
   }

   fun handleTestStarted(message: TeamCityMessage, console: KotestSMTRunnerConsoleView) {
      val id = message["id"] ?: return
      val parentId = message["parent_id"] ?: return
      val name = message["name"] ?: return
      val location = message["locationHint"]
      println(message)
      val proxy = SMTestProxy(name, false, location)
      proxy.setLocator(KotestTestLocator())

      val parent = proxies[parentId] ?: console.resultsViewer.testsRootNode

      parent.addChild(proxy)
      proxy.setStarted()

      proxies[id] = proxy
      console.resultsViewer.onTestStarted(proxy)
      publisher.onTestStarted(proxy)
   }

   fun handleTestFinished(message: TeamCityMessage, console: KotestSMTRunnerConsoleView) {
      val id = message["id"] ?: return
      val duration = message["duration"]?.toLongOrNull() ?: 0L
      val proxy = proxies[id] ?: return
      proxy.setDuration(duration)
      proxy.setFinished()
      console.resultsViewer.onTestFinished(proxy)
      publisher.onTestFinished(proxy)
   }

}
