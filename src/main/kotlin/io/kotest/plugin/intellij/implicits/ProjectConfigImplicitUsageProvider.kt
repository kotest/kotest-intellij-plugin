package io.kotest.plugin.intellij.implicits

import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import io.kotest.plugin.intellij.psi.isSubclass
import io.kotest.plugin.intellij.psi.toKtClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject

/**
 * Allows disabling highlighting of certain elements as unused when such elements are not referenced
 * from the code but are referenced in some other way.
 *
 * This [ImplicitUsageProvider] will mark project config classes as used.
 */
class ProjectConfigImplicitUsageProvider : ImplicitUsageProvider {

   private val fqn = FqName("io.kotest.core.config.AbstractProjectConfig")

   override fun isImplicitWrite(element: PsiElement): Boolean = false
   override fun isImplicitRead(element: PsiElement): Boolean = false

   /**
    * Marks subclasses of AbstractProjectConfig as used.
    */
   override fun isImplicitUsage(element: PsiElement): Boolean {
      return when (element) {
         is KtClassOrObject -> element.isSubclass(fqn)
         is KtLightClass -> element.toKtClass()?.isSubclass(fqn) ?: false
         else -> false
      }
   }
}
