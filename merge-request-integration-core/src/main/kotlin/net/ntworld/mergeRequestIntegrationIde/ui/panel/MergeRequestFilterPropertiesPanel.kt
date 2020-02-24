package net.ntworld.mergeRequestIntegrationIde.ui.panel

import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.UIUtil
import net.ntworld.mergeRequest.MergeRequestState
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequest.UserInfo
import net.ntworld.mergeRequest.query.GetMergeRequestFilter
import net.ntworld.mergeRequestIntegration.make
import net.ntworld.mergeRequestIntegrationIde.service.ApplicationService
import net.ntworld.mergeRequestIntegrationIde.task.FetchProjectMembersTask
import net.ntworld.mergeRequestIntegrationIde.ui.Component
import java.awt.event.ActionListener
import javax.swing.*

class MergeRequestFilterPropertiesPanel(
    private val applicationService: ApplicationService,
    private val ideaProject: IdeaProject,
    private val providerData: ProviderData,
    private val onChanged: (() -> Unit),
    private val onReady: (() -> Unit)
) : Component {
    var myWholePanel: JPanel? = null
    var myStateAll: JRadioButton? = null
    var myStateOpened: JRadioButton? = null
    var myStateMerged: JRadioButton? = null
    var myStateClosed: JRadioButton? = null
    var myAuthor: ComboBox<UserInfo>? = null
    var myAssignee: ComboBox<UserInfo>? = null
    var myApprover: ComboBox<UserInfo>? = null

    private var isFetched = false
    private val myData = mutableListOf<UserInfo>()
    private val myListener = object : FetchProjectMembersTask.Listener {
        override fun onError(exception: Exception) {
            isFetched = false
        }

        override fun taskStarted() {
            myData.clear()
        }

        override fun dataReceived(collection: List<UserInfo>) {
            myData.addAll(collection)
        }

        override fun taskEnded() {
            myAuthor!!.model = MyMemberModel(myData)
            if (providerData.hasAssigneeFeature) {
                myAssignee!!.model = MyMemberModel(myData)
            }
            if (providerData.hasApprovalFeature) {
                myApprover!!.model = MyMemberModel(myData)
            }
            onReady()
        }
    }
    private val myListRenderer = ListCellRenderer<UserInfo> { list, value, index, isSelected, cellHasFocus ->
        if (null === value) {
            JPanel()
        } else {
            val panel = UserInfoItemPanel(value)
            panel.setBackground(UIUtil.getListBackground(isSelected, cellHasFocus))
            panel.createComponent()
        }
    }
    private val myActionListener = ActionListener {
        onChanged()
    }

    init {
        val task = FetchProjectMembersTask(applicationService, ideaProject, providerData, true, myListener)
        if (!isFetched) {
            task.start()
        }
        myStateOpened!!.isSelected = true

        myAuthor!!.renderer = myListRenderer
        myAssignee!!.renderer = myListRenderer
        myApprover!!.renderer = myListRenderer

        myAuthor!!.isSwingPopup = false
        myAssignee!!.isSwingPopup = false
        myApprover!!.isSwingPopup = false

        myAuthor!!.addActionListener(myActionListener)
        myAssignee!!.addActionListener(myActionListener)
        myAssignee!!.addActionListener(myActionListener)
        myStateAll!!.addActionListener(myActionListener)
        myStateOpened!!.addActionListener(myActionListener)
        myStateMerged!!.addActionListener(myActionListener)
        myStateClosed!!.addActionListener(myActionListener)

        myAssignee!!.isEnabled = providerData.hasAssigneeFeature
        myApprover!!.isEnabled = providerData.hasApprovalFeature
    }

    override fun createComponent(): JComponent {
        return myWholePanel!!
    }

    fun buildFilter(search: String): GetMergeRequestFilter {
        return GetMergeRequestFilter.make(
            state = findState(),
            search = search.trim(),
            authorId = findMemberInComboBox(myAuthor!!),
            assigneeId = findMemberInComboBox(myAssignee!!),
            approverIds = listOf(findMemberInComboBox(myApprover!!))
        )
    }

    private fun findMemberInComboBox(comboBox: ComboBox<UserInfo>) : String {
        val selected = comboBox.selectedItem as UserInfo?
        if (!comboBox.isEnabled || null === selected) {
            return ""
        }
        return selected.id
    }

    private fun findState(): MergeRequestState {
        if (myStateOpened!!.isSelected) {
            return MergeRequestState.OPENED
        }
        if (myStateMerged!!.isSelected) {
            return MergeRequestState.MERGED
        }
        if (myStateClosed!!.isSelected) {
            return MergeRequestState.CLOSED
        }
        return MergeRequestState.ALL
    }

    private class MyMemberModel(
        private val data: List<UserInfo>
    ) : ComboBoxModel<UserInfo>, AbstractListModel<UserInfo>() {
        private var mySelected: UserInfo? = null

        override fun setSelectedItem(anItem: Any?) {
            mySelected = anItem as UserInfo?
            fireContentsChanged(this, -1, -1)
        }

        override fun getElementAt(index: Int): UserInfo {
            if (index < 0 || index >= data.size) {
                throw ArrayIndexOutOfBoundsException()
            }
            return data[index]
        }

        override fun getSelectedItem(): Any? {
            return mySelected
        }

        override fun getSize(): Int {
            return data.size
        }
    }
}