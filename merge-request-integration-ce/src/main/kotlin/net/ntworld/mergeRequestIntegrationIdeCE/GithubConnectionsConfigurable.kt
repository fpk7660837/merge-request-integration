package net.ntworld.mergeRequestIntegrationIdeCE

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import net.ntworld.mergeRequestIntegrationIde.ui.configuration.GithubConnectionsConfigurableBase

class GithubConnectionsConfigurable(myIdeaProject: Project): GithubConnectionsConfigurableBase(
    ServiceManager.getService(CommunityApplicationService::class.java),
    myIdeaProject
) {
    override fun getId(): String = "MRI:github-ce"

    override fun getDisplayName(): String = "Github"
}