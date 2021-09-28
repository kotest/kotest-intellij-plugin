package inspections

import io.kotest.core.script.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UnequalTypesTest : FunSpec({

   test("foo") {
      "5" shouldBe 5
   }
})
