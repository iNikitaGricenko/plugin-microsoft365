package io.kestra.plugin.onedrive;

import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@MicronautTest
public class UploadTest {

    @Inject
    private OneDriveTestUtils testUtils;

    @Value("${kestra.tasks.onedrive.directory}")
    private String directory;

    @Test
    void fromStorage() throws Exception {
        String out = UUID.randomUUID().toString();
        Upload.Output run = testUtils.upload(out);

        assertThat(run.getUri(), is(new URI("onedrive://" + directory + "/tasks/onedrive/upload/" + out + ".yml")));
    }
}
