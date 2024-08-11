package io.kotest.plugin.intellij

import io.kotest.matchers.shouldBe
import io.kotest.plugin.intellij.teamcity.TeamCityMessage
import io.kotest.plugin.intellij.teamcity.TeamCityMessageParser
import io.kotest.plugin.intellij.teamcity.TeamCityMessageType
import org.junit.Test

class TeamCityMessageParserTest {

   @Test
   fun happyPath() {
      TeamCityMessageParser().parse("""##teamcity[testFinished name='enums' id='pck.Test/enums' duration='1' locationHint='kotest:class://pck.Test:59']""") shouldBe TeamCityMessage(
         TeamCityMessageType.TestFinished,
         mapOf(
            "id" to "pck.Test/enums",
            "duration" to "1",
            "locationHint" to "kotest:class://pck.Test:59",
         )
      )
   }

   @Test
   fun spacesInIds() {
      val input =
         """##teamcity[testStarted name='support GenericEnumSymbol' id='com.sksamuel.centurion.avro.decoders.EnumDecoderTest/support GenericEnumSymbol' parent_id='com.sksamuel.centurion.avro.decoders.EnumDecoderTest' locationHint='kotest:class://com.sksamuel.centurion.avro.decoders.EnumDecoderTest:13']"""
      TeamCityMessageParser().parse(input) shouldBe TeamCityMessage(
         TeamCityMessageType.TestStarted,
         mapOf(
            "id" to "com.sksamuel.centurion.avro.decoders.EnumDecoderTest/support GenericEnumSymbol",
            "parent_id" to "com.sksamuel.centurion.avro.decoders.EnumDecoderTest",
            "locationHint" to "kotest:class://com.sksamuel.centurion.avro.decoders.EnumDecoderTest:13",
         )
      )
   }
}
