package io.kotest.plugin.intellij.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
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

   /**
    * Fetches the type of [this] while ignoring nullability.
    * E.g. if this is a `[Boolean]?` we will get [Boolean].
    */
   private fun KtExpression.tryResolveType(): KotlinType? =
      try {
         analyze(BodyResolveMode.PARTIAL).getType(this)?.makeNotNullable()
      } catch (e: Exception) {
         null
      }

   companion object {
      infix fun KotlinType?.isComparableTo(other: KotlinType?): Boolean =
         when {
            this == null && other == null -> true
            this != null && other != null -> NewKotlinTypeChecker.Default.isSubtypeOf(this, other) ||
               NewKotlinTypeChecker.Default.isSubtypeOf(other, this) ||
               NewKotlinTypeChecker.Default.equalTypes(other, this) ||
               comparableTypes.any { it.contains(this, other) }
            else -> false
         }

      val testedOperations = listOf("shouldBe", "shouldNotBe")

      private val comparableTypes = setOf<TypeSet>(
         IntegerNumbers,
         FloatingPointNumbers,
      )

      object IntegerNumberArrays : TypeSet(listOf("IntArray", "LongArray"))
      object IntegerNumbers : TypeSet(listOf("Int", "Long"))
      object FloatingPointNumbers : TypeSet(listOf("Float", "Double"))
      object FloatingPointNumberArrays : TypeSet(listOf("FloatArray", "DoubleArray"))

      abstract class TypeSet(val typeNames: List<String>) {
         fun contains(vararg type: KotlinType) =
            this.typeNames.containsAll(type.map { it.toString() })
      }
   }
}
