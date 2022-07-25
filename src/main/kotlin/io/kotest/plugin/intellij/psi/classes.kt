package io.kotest.plugin.intellij.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.typeUtil.supertypes

/**
 * Returns the [KtClass] from this light class, otherwise null.
 */
fun KtLightClass.toKtClass(): KtClass? = kotlinOrigin?.toKtClass()

/**
 * Returns true if this [KtClassOrObject] is a descendent of the given class,
 */
fun KtClassOrObject.isSubclass(fqn: FqName): Boolean = getAllSuperClasses().contains(fqn)

/**
 * If this is an instance of [KtClass] returns this, otherwise returns null.
 */
fun KtClassOrObject.toKtClass(): KtClass? = if (this is KtClass) this else null

/**
 * Returns all [KtClass]s located in this [PsiFile]
 */
fun PsiFile.classes(): List<KtClass> {
   return this.getChildrenOfType<KtClass>().asList()
}

/**
 * Returns the first [KtClass] parent of this element.
 */
fun PsiElement.enclosingKtClass(): KtClass? = getStrictParentOfType()

/**
 * Recursively returns the list of classes and interfaces extended or implemented by the class.
 */
fun KtClassOrObject.getAllSuperClasses(): List<FqName> {
   return superTypeListEntries
      .mapNotNull { it.typeReference }
      .mapNotNull {
         val bindingContext = it.analyze()
         bindingContext.get(BindingContext.TYPE, it)
      }.flatMap {
         it.supertypes() + it
      }.mapNotNull {
         it.constructor.declarationDescriptor.classId
      }.map {
         val packageName = it.packageFqName
         val simpleName = it.relativeClassName
         FqName("$packageName.$simpleName")
      }.filterNot { it.toString() == "kotlin.Any" }
}
