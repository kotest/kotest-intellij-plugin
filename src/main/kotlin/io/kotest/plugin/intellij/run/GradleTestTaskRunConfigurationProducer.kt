package io.kotest.plugin.intellij.run

import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import io.kotest.plugin.intellij.gradle.GradleUtils
import io.kotest.plugin.intellij.psi.asKtClassOrObjectOrNull
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.plugins.gradle.execution.test.runner.TestClassGradleConfigurationProducer
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

/**
 * Runs a Kotest spec using the standard gradle `task` test.
 */
class GradleTestTaskRunConfigurationProducer : TestClassGradleConfigurationProducer() {

   override fun setupConfigurationFromContext(
      configuration: GradleRunConfiguration,
      context: ConfigurationContext,
      sourceElement: Ref<PsiElement?>
   ): Boolean {
      // if we have the kotest plugin then we shouldn't use this
      if (GradleUtils.hasKotestTask(context.module)) return false
      return super.setupConfigurationFromContext(configuration, context, sourceElement)
   }

   override fun isConfigurationFromContext(
      configuration: GradleRunConfiguration,
      context: ConfigurationContext
   ): Boolean {
      // if we have the kotest plugin then we shouldn't use this
      if (GradleUtils.hasKotestTask(context.module)) return false
      return super.isConfigurationFromContext(configuration, context)
   }

   override fun getPsiClassForLocation(contextLocation: Location<*>): PsiClass? {
      val leaf = contextLocation.psiElement
      if (leaf is LeafPsiElement) {
         val spec = leaf.asKtClassOrObjectOrNull()
         if (spec is KtClass) {
            return spec.toLightClass()
         }
      }
      return super.getPsiClassForLocation(contextLocation)
   }
}
