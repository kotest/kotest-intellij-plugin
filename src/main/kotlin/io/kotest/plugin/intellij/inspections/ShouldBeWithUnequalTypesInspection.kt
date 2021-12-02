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
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

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

      val testedOperations = listOf("shouldBe", "shouldNotBe")

      private val comparableTypes = setOf(
         object : TypeSet("Int", "Long") {},
         object : TypeSet("IntArray", "LongArray") {},
         object : TypeSet("Float", "Double") {},
         object : TypeSet("FloatArray", "DoubleArray") {},
      )

      abstract class TypeSet(vararg types: String) {
         val types = types.toList()

         fun contains(vararg type: KotlinType) =
            types.containsAll(type.map { it.toString() })
      }
   }
}
