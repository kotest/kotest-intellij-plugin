package io.kotest.plugin.intellij.teamcity

import jetbrains.buildServer.messages.serviceMessages.ServiceMessagesParser
import org.gradle.api.internal.tasks.testing.DefaultTestSuiteDescriptor
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread
import kotlin.sequences.forEach

//class TeamCityMessageParser {
//
//   private val prefix: String = "##teamcity"
//   private val regex = "$prefix\\[(.+?)(\\s.*)?]".toRegex()
//   private val propRegex = "(.+?)='(.+?)'".toRegex()
//
//   fun parse(input: String): TeamCityMessage? {
//      val match = regex.matchEntire(input.trim()) ?: return null
//      val properties = match.groupValues.getOrNull(2) ?: ""
//      return TeamCityMessage(
//         type = TeamCityMessageType.fromWireName(match.groupValues[1].trim()),
//         properties = propRegex.findAll(properties).associate {
//            val name = it.groupValues[1].trim()
//            val value = it.groupValues[2].trim()
//            Pair(name, value)
//         }
//      )
//   }
//}
//
//data class TeamCityMessage(
//   val type: TeamCityMessageType,
//   val properties: Map<String, String>,
//) {
//   operator fun get(name: String) = properties[name]
//}
//
//enum class TeamCityMessageType(private val wireName: String) {
//   TestSuiteStarted("testSuiteStarted"),
//   TestSuiteFinished("testSuiteFinished"),
//   TestStarted("testStarted"),
//   TestFinished("testFinished");
//
//   companion object {
//      fun fromWireName(input: String): TeamCityMessageType {
//         return TeamCityMessageType.entries.first { it.wireName == input }
//      }
//   }
//}


class TeamCityListener {

   // the intput stream will receive anything written to the output stream
   private val input = PipedInputStream()

   // the output stream should be attached to the java exec process to receive whatever is written to stdout
   val output = PipedOutputStream(input)

   // this parser is provided by the kotlin gradle plugin library and will parse teamcity messages
   private val parser = ServiceMessagesParser()

   private val root = DefaultTestSuiteDescriptor("root", "root")

   // the service message parser emits events to a callback implementation which we provide
   private val callback = KotestServiceMessageParserCallback(root, emptyList(), mutableListOf())

   /**
    * Starts a new thread which consumes the input stream and parses the teamcity messages.
    */
   fun start() {
      thread {
//         listeners.forEach {
//            it.beforeSuite(root)
//         }
         input.bufferedReader().useLines { lines ->
            // the lines here is a lazy sequence which will be fed lines as they arrive from std out
            lines.forEach { parser.parse(it, callback) }
         }
//         listeners.forEach {
//            it.afterSuite(root, DefaultTestResult(TestResult.ResultType.SUCCESS, 0, 0, 0, 0, 0, emptyList()))
//         }
      }
   }
}


private fun hasRtJar(): Boolean {
   return try {
      this::class.java.classLoader.loadClass("com.intellij.rt.execution.application.AppMainV2") != null
   } catch (_: ClassNotFoundException) {
      false
   }
}
