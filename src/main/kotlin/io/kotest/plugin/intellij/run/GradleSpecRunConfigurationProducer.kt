package io.kotest.plugin.intellij.run

import com.intellij.execution.Location
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.source.tree.LeafPsiElement
import io.kotest.plugin.intellij.psi.getSpecEntryPoint
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.plugins.gradle.execution.test.runner.TestClassGradleConfigurationProducer

class GradleSpecRunConfigurationProducer : TestClassGradleConfigurationProducer() {
   override fun getPsiClassForLocation(contextLocation: Location<*>): PsiClass? {
      val leaf = contextLocation.psiElement
      if (leaf is LeafPsiElement) {
         val spec = leaf.getSpecEntryPoint()
         if (spec is KtClass) {
            return spec.toLightClass()
         }
      }
      return super.getPsiClassForLocation(contextLocation)
   }
}
