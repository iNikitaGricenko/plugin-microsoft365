package io.kestra.plugin.onedrive;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.*;
import com.google.common.base.Strings;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import okhttp3.Request;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractOneDrive extends Task {

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String clientId;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String clientSecret;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String tenantId;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String pemCertificate;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String authorizationCode;

    @Schema(
        title = ""
    )
    @PluginProperty(dynamic = true)
    String redirectUrl;

    @Schema(
        title = ""
    )
    @Builder.Default
    List<String> scopes = List.of("User.Read");

    GraphServiceClient<Request> client(RunContext runContext) throws IllegalVariableEvaluationException {
        return GraphServiceClient.builder()
            .authenticationProvider(this.credentials(runContext))
            .buildClient();
    }

    private IAuthenticationProvider credentials(RunContext runContext) throws IllegalVariableEvaluationException {
        TokenCredential credentials;

        String tenantId = runContext.render(this.tenantId);
        List<String> scopes = runContext.render(this.scopes);

        if (Strings.isNullOrEmpty(runContext.render(clientId)) || Strings.isNullOrEmpty(runContext.render(clientSecret))) {
            // TODO Throw exception
            throw new RuntimeException(); // TODO Change throwable
        }

        if (!Strings.isNullOrEmpty(tenantId) &&
            !Strings.isNullOrEmpty(runContext.render(this.redirectUrl)) &&
            !Strings.isNullOrEmpty(runContext.render(this.authorizationCode))) {
            credentials = getAuthorizationCodeCredential(runContext);
        } else if (!Strings.isNullOrEmpty(tenantId) && !Strings.isNullOrEmpty(runContext.render(this.pemCertificate))) {
            credentials = getClientCertificateCredential(runContext);
        } else {
            credentials = getClientSecretCredential(runContext);
        }

        if (scopes == null || credentials == null) {
            // TODO Throw exception
            throw new RuntimeException(); // TODO Change throwable
        }
        return new TokenCredentialAuthProvider(scopes, credentials);
    }

    private AuthorizationCodeCredential getAuthorizationCodeCredential(RunContext runContext) throws IllegalVariableEvaluationException {
        return new AuthorizationCodeCredentialBuilder()
            .clientId(runContext.render(this.clientId))
            .clientSecret(runContext.render(this.clientSecret))
            .tenantId(runContext.render(this.tenantId))
            .redirectUrl(runContext.render(this.redirectUrl))
            .authorizationCode(runContext.render(this.authorizationCode)).build();
    }

    private ClientCertificateCredential getClientCertificateCredential(RunContext runContext) throws IllegalVariableEvaluationException {
        return new ClientCertificateCredentialBuilder()
            .clientId(runContext.render(this.clientId))
            .tenantId(runContext.render(this.tenantId))
            .pemCertificate(runContext.render(this.pemCertificate)).build();
    }

    private ClientSecretCredential getClientSecretCredential(RunContext runContext) throws IllegalVariableEvaluationException {
        return new ClientSecretCredentialBuilder()
            .clientId(runContext.render(this.clientId))
            .clientSecret(runContext.render(this.clientSecret))
            .tenantId(runContext.render(this.tenantId)).build();
    }


    static URI encode(RunContext runContext, String path) throws IllegalVariableEvaluationException, URISyntaxException {
        return new URI(encode(runContext.render(path)));
    }

    static String encode(String path) {
        return path.replace(" ", "%20");
    }

    static String getPath(URI uri) {
        if (uri == null) {
            uri = URI.create("/");
        }

        parentTraversalValidation(uri);
        String path = uri.getPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path;
    }

    static void parentTraversalValidation(URI uri) {
        if (uri.toString().contains("..")) {
            throw new IllegalArgumentException("File should be accessed with their full path and not using relative '..' path.");
        }
    }

}
