package inspections

import io.kotest.core.script.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UnequalTypesTest : FunSpec({

   test("foo") {
      50 shouldBe circleArea(radius = 4)
   }
})


fun circleArea(radius: Int) = 3.14 * radius * radius
