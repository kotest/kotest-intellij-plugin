package inspections

import io.kotest.core.script.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SubtypesTest : FunSpec({

   test("foo") {
      Bar(5) shouldBe Foo(5)
      Foo(5) shouldBe Bar(5)
      Bar(3) shouldBe Bar(5)
   }
})

// Having these defined inline above causes test to fail
open class Foo(val x: Int)
class Bar(x: Int) : Foo(x)
