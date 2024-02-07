package io.kestra.plugin.onedrive;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.utils.TestsUtils;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@Singleton
public class OneDriveTestUtils {

    @Inject
    private StorageInterface storageInterface;

    @Inject
    private RunContextFactory runContextFactory;

    @Value("${kestra.tasks.onedrive.directory}")
    private String directory;

    @Value("${kestra.tasks.onedrive.clientId}")
    private String clientId;

    @Value("${kestra.tasks.onedrive.clientSecret}")
    private String clientSecret;

    @Value("${kestra.tasks.onedrive.tenantId}")
    private String tenantId;

    @Value("${kestra.tasks.onedrive.redirectUrl}")
    private String redirectUrl;

    Upload.Output upload(String out) throws Exception {
        return this.upload(out, "application.yml");
    }

    Upload.Output upload(String out, String resource) throws Exception {
        URI source = storageInterface.put(
            null,
            new URI("/" + UUID.randomUUID()),
            new FileInputStream(new File(Objects.requireNonNull(UploadTest.class.getClassLoader()
                .getResource(resource))
                .toURI()))
                                         );

        Upload task = Upload.builder()
            .id(UploadTest.class.getSimpleName())
            .type(Upload.class.getName())
            .from(source.toString())
            .clientId(this.clientId)
            .clientSecret(this.clientSecret)
            .tenantId(this.tenantId)
            .redirectUrl(this.redirectUrl)
            .to("onedrive://{{inputs.directory}}/tasks/onderive/upload/" + out + "." + FilenameUtils.getExtension(resource))
            .build();

        return task.run(runContext(task));
    }

    RunContext runContext(Task task) {
        return TestsUtils.mockRunContext(
            this.runContextFactory,
            task,
            ImmutableMap.of(
                "directory", this.directory
                           )
                                        );
    }
}
