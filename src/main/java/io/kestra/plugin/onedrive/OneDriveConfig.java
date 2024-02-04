package io.kestra.plugin.onedrive;

import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.inject.Singleton;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Singleton
@Getter
@ConfigurationProperties("kestra.storage.onedrive")
public class OneDriveConfig {

    String clientId;

    String clientSecret;

    String tenantId;

    String pemCertificate;

    String authorizationCode;

    String redirectUrl;

    List<String> scopes = Arrays.asList("User.Read");

}
