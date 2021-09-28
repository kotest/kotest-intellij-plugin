package io.kotest.plugin.intellij.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class ShouldBeWithUnequalTypesInspection : AbstractKotlinInspection() {

   override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
      return object : PsiElementVisitor() {
         override fun visitElement(element: PsiElement) {
            if (element is KtBinaryExpression) {
               if (element.operationReference.text in testedOperations) {
                  val lhsType = element.left?.tryResolveType()
                  val rhsType = element.right?.tryResolveType()

                  if (lhsType != rhsType) {
                     holder.registerProblem(
                        element,
                        "Comparing incompatible types ${lhsType.toString()} and ${rhsType.toString()}",
                        ProblemHighlightType.WARNING,
                     )
                  }
               }
            }
         }
      }
   }

   /**
    * Fetches the type of [this] while ignoring nullability.
    * E.g. if this is a `[Boolean]?` we will get [Boolean].
    */
   private fun KtExpression.tryResolveType(): KotlinType? =
      try {
         this.resolveType().makeNotNullable()
      } catch (e: Exception) {
         null
      }

   companion object {
      val testedOperations = listOf("shouldBe", "shouldNotBe")
   }
}
