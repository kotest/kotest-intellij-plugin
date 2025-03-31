package io.kotest.plugin.intellij

import com.intellij.codeInsight.daemon.quickFix.ExternalLibraryResolver
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ExternalLibraryDescriptor
import com.intellij.util.ThreeState

class KotestExternalLibraryResolver : ExternalLibraryResolver() {

   companion object {
      val KotestDescriptor = ExternalLibraryDescriptor("io.kotest", "kotest-runner-junit5-jvm", "5.9.1", null, "5.9.1")
      val Specs = listOf("FunSpec", "BehaviorSpec", "StringSpec", "ShouldSpec", "AnnotationSpec", "ExpectSpec", "FeatureSpec", "DescribeSpec", "WordSpec", "FreeSpec")
   }

   override fun resolveClass(shortClassName: String, isAnnotation: ThreeState, contextModule: Module): ExternalClassResolveResult? {
      if (Specs.contains(shortClassName)) {
         return ExternalClassResolveResult("io.kotest.core.spec.style.$shortClassName", KotestDescriptor)
      }
      return null
   }
}
