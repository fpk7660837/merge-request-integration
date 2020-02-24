package net.ntworld.mergeRequestIntegrationIde.ui.toolWindowTab

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ui.ChangesTreeImpl
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import net.miginfocom.swing.MigLayout
import net.ntworld.mergeRequest.MergeRequest
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequestIntegrationIde.service.ApplicationService
import net.ntworld.mergeRequestIntegrationIde.service.ProjectEventListener
import net.ntworld.mergeRequestIntegrationIde.service.ProjectService
import net.ntworld.mergeRequestIntegrationIde.ui.Component
import net.ntworld.mergeRequestIntegrationIde.ui.mergeRequest.tab.commit.CommitChanges
import net.ntworld.mergeRequestIntegrationIde.ui.service.DisplayChangesService
import net.ntworld.mergeRequestIntegrationIde.ui.util.ToolbarUtil
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class ChangesToolWindowTab(
    private val applicationService: ApplicationService,
    private val ideaProject: Project
) : Component {
    private val myComponentEmpty = JPanel()
    private val myLabelEmpty = JLabel()
    private val myComponent = SimpleToolWindowPanel(true, true)
    private val myTree = MyTree(ideaProject)
    private val myTreeWrapper = ScrollPaneFactory.createScrollPane(myTree, true)
    private val myToolbar by lazy {
        val panel = JPanel(MigLayout("ins 0, fill", "[left]0[left, fill]push[right]", "center"))

        val leftActionGroup = DefaultActionGroup()
        val leftToolbar = ActionManager.getInstance().createActionToolbar(
            "${CommitChanges::class.java.canonicalName}/toolbar-left",
            leftActionGroup,
            true
        )

        panel.add(JPanel())
        panel.add(leftToolbar.component)
        panel.add(
            ToolbarUtil.createExpandAndCollapseToolbar(
                "${ChangesToolWindowTab::class.java.canonicalName}/toolbar-right",
                myTree
            )
        )
        panel
    }
    private val myProjectEventListener = object : ProjectEventListener {
        override fun startCodeReview(providerData: ProviderData, mergeRequest: MergeRequest) {
            showChanges()
        }

        override fun stopCodeReview(providerData: ProviderData, mergeRequest: MergeRequest) {
            hideChanges()
        }

        override fun codeReviewChangesSet(
            providerData: ProviderData,
            mergeRequest: MergeRequest,
            changes: Collection<Change>
        ) {
            setChanges(changes)
        }
    }
    private val myTreeSelectionListener = TreeSelectionListener {
        if (null === it) {
            return@TreeSelectionListener
        }

        val node = it.path.lastPathComponent
        if (node !is DefaultMutableTreeNode) {
            return@TreeSelectionListener
        }

        val change = node.userObject
        if (change !is Change) {
            return@TreeSelectionListener
        }

        DisplayChangesService.openChange(ideaProject, FileEditorManagerEx.getInstanceEx(ideaProject), change)
    }

    init {
        val projectService = applicationService.getProjectService(ideaProject)
        myLabelEmpty.text = "Changes will be displayed when you do Code Review"
        myComponentEmpty.background = JBColor.background()
        myComponentEmpty.layout = GridBagLayout()
        myComponentEmpty.add(myLabelEmpty)
        myComponent.toolbar = myToolbar
        myTree.addTreeSelectionListener(myTreeSelectionListener)

        if (projectService.isDoingCodeReview()) {
            setChanges(projectService.getCodeReviewChanges())
            showChanges()
        } else {
            hideChanges()
        }
        projectService.dispatcher.addListener(myProjectEventListener)
    }

    override fun createComponent(): JComponent = myComponent

    private fun hideChanges() {
        myTree.setChangesToDisplay(listOf())
        myToolbar.isVisible = false
        myComponent.setContent(myComponentEmpty)
    }

    private fun showChanges() {
        myToolbar.isVisible = true
        myComponent.setContent(myTreeWrapper)
    }

    private fun setChanges(changes: Collection<Change>) {
        ApplicationManager.getApplication().invokeLater {
            myTree.setChangesToDisplay(changes)
        }
    }

    private class MyTree(ideaProject: Project) : ChangesTreeImpl<Change>(
        ideaProject, false, false, Change::class.java
    ) {
        override fun buildTreeModel(changes: MutableList<out Change>): DefaultTreeModel {
            return TreeModelBuilder.buildFromChanges(myProject, grouping, changes, null)
        }
    }
}