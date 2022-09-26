package io.kotest.plugin.intellij

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import io.kotest.plugin.intellij.toolwindow.TagsFilename

fun findFiles(project: Project): List<VirtualFile> {
   return FilenameIndex
      .getVirtualFilesByName(TagsFilename, false, GlobalSearchScope.allScope(project))
      .toList()
}

fun getLocationForFile(
   project: Project,
   scope: GlobalSearchScope,
   name: String,
   lineNumber: Int
): PsiLocation<PsiElement>? {
   return FilenameIndex
      .getVirtualFilesByName(name, scope)
      .firstOrNull { it.isTestFile(project) }
      ?.toPsiFile(project)
      ?.elementAtLine(lineNumber)
      ?.toPsiLocation()
}
