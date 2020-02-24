package net.ntworld.mergeRequestIntegrationIdeCE

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import net.ntworld.mergeRequestIntegrationIde.ui.configuration.GitlabConnectionsConfigurableBase

class GitlabConnectionsConfigurable(myIdeaProject: Project) : GitlabConnectionsConfigurableBase(
    ServiceManager.getService(CommunityApplicationService::class.java),
    myIdeaProject
) {
    override fun getId(): String = "MRI:gitlab-ce"

    override fun getDisplayName(): String = "Gitlab"
}
