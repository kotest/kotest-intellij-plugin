package io.kotest.plugin.intellij.toolwindow

import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.TreeUIHelper
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeSelectionModel

class TestFileTree(
   project: Project,
) : com.intellij.ui.treeStructure.Tree(),
   KotestTestExplorerService.ModelListener {

   private val testExplorerTreeSelectionListener = TestExplorerTreeSelectionListener(project)
   private val kotestTestExplorerService: KotestTestExplorerService =
      project.getService(KotestTestExplorerService::class.java)
   private var initialized = false
   private val filesInitiallyExpanded = mutableSetOf<String>()
   private var lastFileKey: String? = null
   private val savedExpandedByFile = mutableMapOf<String, Set<String>>()
   private val savedAllKeysByFile = mutableMapOf<String, Set<String>>()

   init {
      selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
      showsRootHandles = true
      isRootVisible = false
      cellRenderer = NodeRenderer()
      // enable speed search like in the Project tool window, using presentable text
      TreeUIHelper.getInstance().installTreeSpeedSearch(this, { path ->
         val node = path.lastPathComponent as? DefaultMutableTreeNode
         val descriptor = node?.userObject as? PresentableNodeDescriptor<*>
         descriptor?.presentation?.presentableText ?: node?.userObject?.toString() ?: ""
      }, false)
      // listens to changes in the selections
      addTreeSelectionListener(testExplorerTreeSelectionListener)
      kotestTestExplorerService.registerModelListener(this)
      initialized = true
   }

   override fun setModel(treeModel: TreeModel) {
      if (!initialized) {
         super.setModel(treeModel)
         return
      }
      val newFileKey = currentFileKey()

      // If switching away from a file, save its state first
      if (lastFileKey != null && lastFileKey != newFileKey) {
         savedExpandedByFile[lastFileKey!!] = collectExpandedPathKeys()
         savedAllKeysByFile[lastFileKey!!] = collectAllPathKeys()
      }

      val firstOpenForFile = newFileKey != null && !filesInitiallyExpanded.contains(newFileKey)
      val sameFile = newFileKey == lastFileKey

      // Prepare previous keys/expansion baselines
      val prevAllKeysForThisFile: Set<String> = when {
         firstOpenForFile -> emptySet()
         sameFile -> collectAllPathKeys()
         newFileKey != null -> savedAllKeysByFile[newFileKey] ?: emptySet()
         else -> emptySet()
      }
      val expandedKeysToRestore: Set<String> = when {
         firstOpenForFile -> emptySet()
         sameFile -> collectExpandedPathKeys()
         newFileKey != null -> savedExpandedByFile[newFileKey] ?: emptySet()
         else -> emptySet()
      }

      super.setModel(treeModel)

      // Compute added nodes relative to the previous snapshot of this file (if any)
      val newAllKeys = collectAllPathKeys()
      if (!firstOpenForFile) {
         val addedKeys = newAllKeys - prevAllKeysForThisFile
         if (addedKeys.isNotEmpty()) expandAncestorPrefixesFor(addedKeys)
      }

      if (firstOpenForFile) {
         // First time this file is shown in the tool window: expand everything
         expandAllNodes()
         newFileKey?.let { filesInitiallyExpanded.add(it) }
      } else {
         // Restore previous expansion state for this file
         if (expandedKeysToRestore.isNotEmpty()) expandPathsByKeys(expandedKeysToRestore)
      }

      // Update caches for this file and mark it as current
      if (newFileKey != null) {
         savedAllKeysByFile[newFileKey] = newAllKeys
         savedExpandedByFile[newFileKey] = collectExpandedPathKeys()
      }
      lastFileKey = newFileKey
   }

   fun markFileClosed(file: VirtualFile) {
      filesInitiallyExpanded.remove(file.path)
      savedExpandedByFile.remove(file.path)
      savedAllKeysByFile.remove(file.path)
      if (lastFileKey == file.path) lastFileKey = null
   }

   private fun currentFileKey(): String? = kotestTestExplorerService.currentFile?.path

}
