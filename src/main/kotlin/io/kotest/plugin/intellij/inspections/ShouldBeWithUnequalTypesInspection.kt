package io.kotest.plugin.intellij.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import io.kotest.plugin.intellij.psi.TypeSet
import io.kotest.plugin.intellij.psi.tryResolveType
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker

class ShouldBeWithUnequalTypesInspection : AbstractKotlinInspection() {

   override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
      return object : PsiElementVisitor() {
         override fun visitElement(element: PsiElement) {
            if (element is KtBinaryExpression) {
               if (element.operationReference.text in testedOperations) {
                  val lhsType = element.left?.tryResolveType()
                  val rhsType = element.right?.tryResolveType()

                  when (lhsType isComparableTo rhsType) {
                     true -> Unit // Let test compare the types
                     false -> {
                        holder.registerProblem(
                           element,
                           "Comparing incompatible types $lhsType and $rhsType",
                           ProblemHighlightType.WARNING,
                        )
                     }
                  }
               }
            }
         }
      }
   }

   companion object {
      val testedOperations = listOf("shouldBe", "shouldNotBe")

      private val comparableTypes = setOf(
         TypeSet("Int", "Long"),
         TypeSet("IntArray", "LongArray"),
         TypeSet("Float", "Double"),
         TypeSet("FloatArray", "DoubleArray"),
      )

      /**
       * Determines whether two types are considered comparable.
       * Everything is considered comparable with a Matcher
       * Otherwise, if LHS <:< RHS or RHS <:< LHS (where <:< means subtype or type), we consider them comparable
       * We can also manually mark types as comparable using [comparableTypes].
       */
      infix fun KotlinType?.isComparableTo(rhs: KotlinType?): Boolean =
         when {
            this == null && rhs == null -> true
            this != null && rhs != null ->
               rhs.isMatcher() ||
                  NewKotlinTypeChecker.Default.equalTypes(this, rhs) ||
                  NewKotlinTypeChecker.Default.isSubtypeOf(this, rhs) ||
                  NewKotlinTypeChecker.Default.isSubtypeOf(rhs, this) ||
                  comparableTypes.any { it.contains(this, rhs) }
            else -> false
         }

      // This requires the rhs to actually be of a type named "*Matcher*"
      //
      // Using `TypeUtils.getAllSupertypes` for an object implementing `Matcher` only gives `Any` as supertype for
      // some reason.
      //
      // Perhaps we can add annotations to all implementations of `Matcher<*>` using a compiler plugin? That could
      // be picked up here later
      private fun KotlinType.isMatcher() =
         toString().contains("Matcher")
   }
}
