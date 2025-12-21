package io.kotest.plugin.intellij.run.idea

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PackageScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import io.kotest.plugin.intellij.dependencies.ModuleDependencies

@Deprecated("Starting with Kotest 6 the preferred method is to run via gradle")
class PackageRunConfigurationProducer : LazyRunConfigurationProducer<KotestRunConfiguration>() {

   /**
    * Returns the [KotestConfigurationFactory] used to create [KotestRunConfiguration]s.
    */
   override fun getConfigurationFactory(): ConfigurationFactory = KotestConfigurationFactory(KotestConfigurationType())

   override fun isConfigurationFromContext(
      configuration: KotestRunConfiguration,
      context: ConfigurationContext
   ): Boolean = false

   override fun setupConfigurationFromContext(
      configuration: KotestRunConfiguration,
      context: ConfigurationContext,
      sourceElement: Ref<PsiElement>
   ): Boolean {

      // if we don't have the kotest engine on the classpath then we shouldn't use this producer
      if (!ModuleDependencies.hasKotest(context.module)) return false

      val index = ProjectRootManager.getInstance(context.project).fileIndex
      val dirservice = JavaDirectoryService.getInstance()
      val psiDirectory = sourceElement.get()
      if (psiDirectory is PsiJavaDirectoryImpl) {
         if (index.isInTestSourceContent(psiDirectory.virtualFile)) {
            val psiPackage = dirservice.getPackage(psiDirectory)
            if (psiPackage != null) {
               val psiClasses = findKotestSpecsByStyle(context.project, psiPackage.qualifiedName);
               val specs = psiClasses.joinToString(";") { it.qualifiedName.toString() }
               LOG.info("Found ${psiClasses.size} classes in package ${psiPackage.qualifiedName}")
               LOG.info(
                  """
                  Specs:
                     $specs
               """.trimIndent()
               )
               setupConfigurationModule(context, configuration)
               configuration.setPackageName(psiPackage.qualifiedName)
               configuration.setSpecsName(specs)
               configuration.name = generateName(psiPackage.qualifiedName)
               return true
            }
         }
      }
      return false
   }

   fun findKotestSpecsByStyle(
      project: Project,
      targetPackageName: String
   ): List<PsiClass> {
      val kotestStyles = setOf(
         "io.kotest.core.spec.style.FreeSpec",
         "io.kotest.core.spec.style.ExpectSpec",
         "io.kotest.core.spec.style.WordSpec",
         "io.kotest.core.spec.style.BehaviorSpec",
         "io.kotest.core.spec.style.ShouldSpec",
         "io.kotest.core.spec.style.StringSpec",
         "io.kotest.core.spec.style.FunSpec",
         "io.kotest.core.spec.style.FeatureSpec",
         "io.kotest.core.spec.style.DescribeSpec",
         "io.kotest.core.spec.style.AnnotationSpec",
      )
      val facade = JavaPsiFacade.getInstance(project)
      val targetPackage = facade.findPackage(targetPackageName) ?: return emptyList()
      val packageScope = PackageScope(targetPackage, true, false)
      val libraryScope = GlobalSearchScope.allScope(project)
      val foundClasses = mutableListOf<PsiClass>()
      for (styleFqn in kotestStyles) {
         val styleClass = facade.findClass(styleFqn, libraryScope) ?: continue
         val query = ClassInheritorsSearch.search(styleClass, packageScope, true)
         foundClasses.addAll(query.findAll())
      }
      return foundClasses.distinct().filter { it.language.id == "kotlin" }
   }
   private fun setupConfigurationModule(context: ConfigurationContext, configuration: KotestRunConfiguration): Boolean {
      val template = context.runManager.getConfigurationTemplate(configurationFactory)
      val contextModule = context.module
      val predefinedModule = (template.configuration as ModuleBasedConfiguration<*, *>).configurationModule.module
      if (predefinedModule != null) {
         configuration.setModule(predefinedModule)
         return true
      }
      val module = findModule(configuration, contextModule)
      if (module != null) {
         configuration.setModule(module)
         return true
      }
      return false
   }

   private fun findModule(configuration: KotestRunConfiguration, contextModule: Module?): Module? {
      if (configuration.configurationModule.module == null && contextModule != null) {
         return contextModule
      }
      return null
   }

   companion object {
      private val LOG = Logger.getInstance(PackageRunConfigurationProducer::class.java)
   }
}
