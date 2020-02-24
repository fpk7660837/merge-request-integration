package net.ntworld.mergeRequestIntegrationIde.ui.configuration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.util.EventDispatcher
import net.ntworld.mergeRequest.Project
import net.ntworld.mergeRequest.api.ApiCredentials
import net.ntworld.mergeRequestIntegration.provider.github.request.GithubSearchRepositoriesRequest
import net.ntworld.mergeRequestIntegration.provider.github.transformer.GithubRepositoryTransformer
import net.ntworld.mergeRequestIntegrationIde.service.ApplicationService
import net.ntworld.mergeRequestIntegrationIde.ui.panel.ProjectPanel
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.intellij.openapi.project.Project as IdeaProject

class GithubProjectFinder(
    private val applicationService: ApplicationService,
    private val ideaProject: IdeaProject,
    private val myTerm: JTextField,
    private val myProjectList: JList<Project>
) : ProjectFinderUI {
    private var myIsTermTouched: Boolean = false
    private var mySelectedProject: Project? = null
    private var myCredentials: ApiCredentials? = null
    private val mySearchDispatcher = EventDispatcher.create(MySearchListener::class.java)
    private val myProjectChangedDispatcher = EventDispatcher.create(ProjectFinderUI.ProjectChangedListener::class.java)

    init {
        myTerm.text = "Search project..."
        myTerm.addFocusListener(object : FocusListener {
            override fun focusLost(e: FocusEvent?) {
                if (myTerm.text.isEmpty()) {
                    myTerm.text = "Search project..."
                    myIsTermTouched = false
                }
            }

            override fun focusGained(e: FocusEvent?) {
                if (!myIsTermTouched) {
                    myTerm.text = ""
                }
                myIsTermTouched = true
            }
        })

        myProjectList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        myProjectList.cellRenderer = object : ListCellRenderer<Project> {
            override fun getListCellRendererComponent(
                list: JList<out Project>?,
                value: Project?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component? {
                if (null === value) {
                    return JPanel()
                }
                return ProjectPanel(value, isSelected).getComponent()
            }
        }
        myProjectList.addListSelectionListener {
            myProjectChangedDispatcher.multicaster.projectChanged(getSelectedProjectId())
        }

        myTerm.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {
                if (myTerm.text.isNotEmpty()) {
                    triggerSearchTask(myTerm.text.trim())
                }
            }

            override fun insertUpdate(e: DocumentEvent?) = changedUpdate(e)

            override fun removeUpdate(e: DocumentEvent?) = changedUpdate(e)
        })

        mySearchDispatcher.addListener(object : MySearchListener {
            override fun searchFinished(term: String, projects: List<Project>) {
                if (myTerm.text == term) {
                    myProjectList.setListData(projects.toTypedArray())
                }
            }
        })
    }

    private fun triggerSearchTask(term: String) {
        myProjectChangedDispatcher.multicaster.projectChanged("")
        MySearchTask(applicationService, term, this).start()
    }

    override fun addProjectChangedListener(listener: ProjectFinderUI.ProjectChangedListener) {
        myProjectChangedDispatcher.addListener(listener)
    }

    override fun getSelectedProjectId(): String {
        val value = myProjectList.selectedValue
        return if (null === value) "" else value.id
    }

    override fun setSelectedProject(project: Project?) {
        if (null !== project) {
            val current = mySelectedProject
            if (null !== current && current.id == project.id) {
                return
            }

            myProjectList.setListData(arrayOf(project))
            myProjectList.selectedIndex = 0
            mySelectedProject = project
        }
    }

    override fun setEnabled(value: Boolean, credentials: ApiCredentials) {
        myTerm.isEnabled = value
        myProjectList.isEnabled = value
        myCredentials = credentials
    }

    private interface MySearchListener : EventListener {
        fun searchFinished(term: String, projects: List<Project>)
    }

    private class MySearchTask(
        private val applicationService: ApplicationService,
        private val term: String,
        private val self: GithubProjectFinder
    ) : Task.Backgroundable(self.ideaProject, "Searching github projects...", false) {
        fun start() {
            if (self.myIsTermTouched) {
                ProgressManager.getInstance().runProcessWithProgressAsynchronously(this, Indicator(this))
            }
        }

        override fun run(indicator: ProgressIndicator) {
            val credentials = self.myCredentials ?: return
            Thread.sleep(300)
            if (term != self.myTerm.text) {
                return
            }

            val out = applicationService.infrastructure.serviceBus() process GithubSearchRepositoriesRequest(
                credentials = credentials,
                term = term
            )

            if (!out.hasError()) {
                self.mySearchDispatcher.multicaster.searchFinished(
                    term, out.getResponse().repositories.map { GithubRepositoryTransformer.transform(it) }
                )
            }
        }

        private class Indicator(private val task: MySearchTask) : BackgroundableProcessIndicator(task)
    }
}
