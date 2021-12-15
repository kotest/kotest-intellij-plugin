package inspections

import io.kotest.core.script.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.shouldBe

class MatcherTest : FunSpec({

   test("foo") {
      val x: Int? = null
      x shouldBe AlwaysPassMatcher
   }

   test("otherFoo") {
      5 shouldBe AnyComparableMatcher
   }
})

object AlwaysPassMatcher : Matcher<Int> {
   override fun test(value: Int) =
      MatcherResult(
         true,
         { "" },
         { "" }
      )
}

object AnyComparableMatcher : Matcher<Comparable<Int>> {
   override fun test(value: Comparable<Int>): MatcherResult {
      TODO("Not yet implemented")
   }
}

