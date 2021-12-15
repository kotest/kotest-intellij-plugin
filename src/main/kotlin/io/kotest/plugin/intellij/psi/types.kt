package io.kotest.plugin.intellij.psi

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
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
