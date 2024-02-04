package io.kestra.plugin.onedrive;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.*;
import com.google.common.base.Strings;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import io.micronaut.context.annotation.Factory;
import okhttp3.Request;

@Factory
@OneDriveStorageEnabled
public class OneDriveClientFactory {

    protected IAuthenticationProvider credentials(OneDriveConfig config) {
        TokenCredential credentials;

        if (Strings.isNullOrEmpty(config.clientId) || Strings.isNullOrEmpty(config.clientSecret)) {
            // TODO Throw exception
            throw new RuntimeException(); // TODO Change throwable
        }

        if (!Strings.isNullOrEmpty(config.tenantId) && !Strings.isNullOrEmpty(config.redirectUrl) && !Strings.isNullOrEmpty(config.authorizationCode)) {
            credentials = getAuthorizationCodeCredential(config);
        } else if (!Strings.isNullOrEmpty(config.tenantId) && !Strings.isNullOrEmpty(config.pemCertificate)) {
            credentials = getClientCertificateCredential(config);
        } else {
            credentials = getClientSecretCredential(config);
        }

        if (config.scopes == null || credentials == null) {
            // TODO Throw exception
            throw new RuntimeException(); // TODO Change throwable
        }
        return new TokenCredentialAuthProvider(config.scopes, credentials);
    }

    protected GraphServiceClient<Request> client(IAuthenticationProvider authenticationProvider) {
        return GraphServiceClient.builder()
            .authenticationProvider(authenticationProvider)
            .buildClient();
    }

    private AuthorizationCodeCredential getAuthorizationCodeCredential(OneDriveConfig config) {
        return new AuthorizationCodeCredentialBuilder()
            .clientId(config.clientId).clientSecret(config.clientSecret)
            .tenantId(config.tenantId).redirectUrl(config.redirectUrl)
            .authorizationCode(config.authorizationCode).build();
    }

    private ClientCertificateCredential getClientCertificateCredential(OneDriveConfig config) {
        return new ClientCertificateCredentialBuilder()
            .clientId(config.clientId).tenantId(config.tenantId)
            .pemCertificate(config.pemCertificate).build();
    }

    private ClientSecretCredential getClientSecretCredential(OneDriveConfig config) {
        return new ClientSecretCredentialBuilder()
            .clientId(config.clientId).clientSecret(config.clientSecret)
            .tenantId(config.tenantId).build();
    }

}
