package inspections

import io.kotest.core.script.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NullableUnequalTypesTest : FunSpec({

   test("foo") {
      val x: Int? = null
      x shouldBe 5
      5 shouldBe x
      x shouldBe 5L
      5L shouldBe x
   }
})
