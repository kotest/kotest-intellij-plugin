package io.kotest.plugin.intellij.inspections

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.nio.file.Paths

internal class ShouldBeWithUnequalTypesTest : LightJavaCodeInsightFixtureTestCase() {

   override fun getTestDataPath(): String {
      val path = Paths.get("./src/test/resources/").toAbsolutePath()
      return path.toString()
   }

   fun testInspection() {
      myFixture.configureByFile("/inspections/unequalTypes.kt")
      myFixture.enableInspections(ShouldBeWithUnequalTypesInspection::class.java)
      val highlight = myFixture.doHighlighting()
         .singleOrNull { it.description == "Comparing incompatible types String and Int" }

      highlight shouldNotBe null
      highlight!!.text shouldBe """"5" shouldBe 5"""
      highlight.severity shouldBe HighlightSeverity.WARNING
      highlight.startOffset shouldBe 191
      highlight.endOffset shouldBe 205
   }

   fun testInspectionWithNullable() {
      myFixture.configureByFile("/inspections/nullableUnequalTypes.kt")
      myFixture.enableInspections(ShouldBeWithUnequalTypesInspection::class.java)
      myFixture.doHighlighting()
         .filter { it?.description?.contains("Comparing incompatible types") == true } shouldHaveSize 0
   }
}
