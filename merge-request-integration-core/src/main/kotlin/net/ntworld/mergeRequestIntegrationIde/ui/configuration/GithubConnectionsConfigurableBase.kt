package net.ntworld.mergeRequestIntegrationIde.ui.configuration

import net.ntworld.mergeRequest.Project
import net.ntworld.mergeRequest.api.ApiConnection
import net.ntworld.mergeRequest.api.ApiCredentials
import net.ntworld.mergeRequestIntegration.provider.github.Github
import net.ntworld.mergeRequestIntegration.provider.github.GithubUtil
import net.ntworld.mergeRequestIntegration.provider.github.request.GithubFindCurrentUserRequest
import net.ntworld.mergeRequestIntegration.provider.github.request.GithubFindRepositoryRequest
import net.ntworld.mergeRequestIntegration.provider.github.transformer.GithubRepositoryTransformer
import net.ntworld.mergeRequestIntegration.provider.github.vo.GithubProjectId
import net.ntworld.mergeRequestIntegrationIde.exception.InvalidConnectionException
import net.ntworld.mergeRequestIntegrationIde.internal.ApiCredentialsImpl
import com.intellij.openapi.project.Project as IdeaProject
import net.ntworld.mergeRequestIntegrationIde.service.ApplicationService

open class GithubConnectionsConfigurableBase(
    private val applicationService: ApplicationService,
    private val ideaProject: IdeaProject
) : AbstractConnectionsConfigurable(applicationService, ideaProject) {
    override fun getId(): String = "MRI:github"

    override fun getDisplayName(): String = "Github"

    override fun makeProviderInfo() = Github

    override fun findNameFromId(id: String): String = Companion.findNameFromId(id)

    override fun findIdFromName(name: String): String = Companion.findIdFromName(name)

    override fun makeConnection(): ConnectionUI {
        return GithubConnection(applicationService, ideaProject)
    }

    override fun validateConnection(connection: ApiConnection): Boolean {
        return connection.url.isNotEmpty() && connection.login.isNotEmpty() && connection.token.isNotEmpty()
    }

    override fun findProject(credentials: ApiCredentials): Project? {
        val out = applicationService.infrastructure.serviceBus() process GithubFindRepositoryRequest(
            credentials = credentials,
            repositoryId = GithubProjectId.parseId(credentials.projectId).toString()
        )
        val response = out.getResponse()
        return if (response.isSuccess) {
            GithubRepositoryTransformer.transform(response.repository)
        } else {
            null
        }
    }

    override fun assertConnectionIsValid(connection: ApiConnection) {
        val out = applicationService.infrastructure.serviceBus() process GithubFindCurrentUserRequest(
            credentials = ApiCredentialsImpl(
                url = connection.url,
                login = connection.login,
                token = connection.token,
                ignoreSSLCertificateErrors = connection.ignoreSSLCertificateErrors,
                info = "",
                projectId = "",
                version = ""
            )
        )

        val error = out.getResponse().error
        if (null !== error) {
            throw InvalidConnectionException(error.message)
        }
    }

    companion object {
        private const val PREFIX = "github:"

        fun findNameFromId(id: String): String {
            if (id.startsWith(PREFIX)) {
                return id.substring(PREFIX.length)
            }
            return id
        }

        fun findIdFromName(name: String): String {
            return "$PREFIX$name"
        }
    }
}