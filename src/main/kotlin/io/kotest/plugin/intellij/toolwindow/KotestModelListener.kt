package io.kotest.plugin.intellij.toolwindow

import javax.swing.tree.TreeModel

interface KotestModelListener {
   fun setModel(treeModel: TreeModel)
}

