package net.ntworld.mergeRequestIntegrationIdeEE

import com.intellij.openapi.components.ServiceManager
import net.ntworld.mergeRequestIntegrationIde.ui.editor.AddCommentEditorActionBase

class AddCommentEditorAction : AddCommentEditorActionBase(
    ServiceManager.getService(EnterpriseApplicationService::class.java)
)
