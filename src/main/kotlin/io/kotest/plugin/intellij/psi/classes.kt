package io.kotest.plugin.intellij.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAbstract

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

fun PsiElement.enclosingKtClassOrObject(): KtClassOrObject? =
   PsiTreeUtil.getParentOfType(this, KtClassOrObject::class.java)

/**
 * Returns true if this [KtClassOrObject] points to a runnable spec object.
 */
fun KtClassOrObject.isRunnableSpec(): Boolean = when (this) {
   is KtObjectDeclaration -> isSpec()
   is KtClass -> isSpec() && !isAbstract()
   else -> false
}

fun KtClassOrObject.takeIfRunnableSpec(): KtClassOrObject? = if (isRunnableSpec()) this else null
