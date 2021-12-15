package io.kotest.plugin.intellij.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import io.kotest.plugin.intellij.psi.isComparableTo
import io.kotest.plugin.intellij.psi.tryResolveType
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtBinaryExpression

class ShouldBeWithUnequalTypesInspection : AbstractKotlinInspection() {
   val testedOperations = listOf("shouldBe", "shouldNotBe")

   override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
      return object : PsiElementVisitor() {
         override fun visitElement(element: PsiElement) {
            if (element is KtBinaryExpression) {
               if (element.operationReference.text in testedOperations) {
                  val lhsType = element.left?.tryResolveType()
                  val rhsType = element.right?.tryResolveType()

                  if (!lhsType.isComparableTo(rhsType)) {
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
