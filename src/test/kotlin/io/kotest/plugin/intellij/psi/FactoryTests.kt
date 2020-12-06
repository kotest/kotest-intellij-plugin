package io.kotest.plugin.intellij.psi

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import io.kotest.core.spec.style.funSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.nio.file.Paths

fun foo(a:String) = funSpec {  }

class FactoryTests : LightJavaCodeInsightFixtureTestCase() {

   override fun getTestDataPath(): String {
      val path = Paths.get("./src/test/resources/").toAbsolutePath()
      return path.toString()
   }

   fun testFactoryDetection() {
      val psiFile = myFixture.configureByFile("/factory_definition.kt")
      val leaf = psiFile.findElementAt(120)!!
      val func = leaf.parent
      func.shouldBeInstanceOf<KtNamedFunction>()
      func.getFactoryName() shouldBe "factory"
   }

   fun testFactoryDetectionWithArgs() {
      val psiFile = myFixture.configureByFile("/factory_definition.kt")
      val leaf = psiFile.findElementAt(176)!!
      val func = leaf.parent
      func.shouldBeInstanceOf<KtNamedFunction>()
      func.getFactoryName() shouldBe "factoryWithArgs"
   }
}
