package io.kotest.plugin.intellij.psi

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

/**
 * Fetches the type of [this] while ignoring nullability.
 * E.g. if this is a `[Boolean]?` we will get [Boolean].
 */
internal fun KtExpression.tryResolveType(): KotlinType? =
   try {
      analyze(BodyResolveMode.PARTIAL).getType(this)?.makeNotNullable()
   } catch (e: Exception) {
      null
   }

/**
 * Helper class for constructing a set of types, and testing whether a given [KotlinType] is part of the set
 * Construction is done using type names atm, but an additional constructor accepting [KotlinType]s could be added
 */
internal class TypeSet(vararg typeNames: String) {
   val types = typeNames.toSet()

   fun contains(vararg type: KotlinType) =
      types.containsAll(type.map { it.toString() })
}

private val defaultComparableTypes = setOf(
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
internal fun KotlinType?.isComparableTo(
   rhs: KotlinType?,
   comparableTypes: Set<TypeSet> = defaultComparableTypes
): Boolean =
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
