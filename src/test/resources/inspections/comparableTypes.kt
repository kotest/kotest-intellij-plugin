package inspections

import io.kotest.core.script.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.eval4j.double

class ComparableTypeTest : FunSpec({

   test("integer numbers") {
      1 shouldBe 1L
      1L shouldBe 1
      intArrayOf(1, 2, 3) shouldBe longArrayOf(1, 2, 3)
      longArrayOf(1, 2, 3) shouldBe intArrayOf(1, 2, 3)
   }

   test("decimal numbers") {
      1.0f shouldBe 1.0
      1.0 shouldBe 1.0f
      floatArrayOf(0.0f, 0.1f) shouldBe doubleArrayOf(0.0, 0.1)
      doubleArrayOf(0.0, 0.1) shouldBe floatArrayOf(0.0f, 0.1f)
   }
})
