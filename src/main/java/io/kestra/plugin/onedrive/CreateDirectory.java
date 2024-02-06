package io.kestra.plugin.onedrive;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemSearchParameterSet;
import com.microsoft.graph.models.Folder;
import com.microsoft.graph.requests.DriveItemSearchCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import okhttp3.Request;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URI;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Plugin(
    examples = {
        @Example(
            title = "Create a new directory with some options",
            code = {
                "name: \"my-directory\"",
                "ifExists: SKIP",
            }
        )
    }
)
@Schema(
    title = "Create a directory or update if it already exists."
)
public class CreateDirectory extends AbstractOneDrive implements RunnableTask<CreateDirectory.Output> {

    @Schema(
        title = "The directory name"
    )
    @PluginProperty(dynamic = true)
    private String name;

    @Builder.Default
    @Schema(
        title = "Policy to apply if a directory already exists."
    )
    private IfExists ifExists = IfExists.ERROR;

    @Override
    public CreateDirectory.Output run(RunContext runContext) throws Exception {
        GraphServiceClient<Request> client = this.client(runContext);

        DriveItem directory = new DriveItem();
        directory.name = name;
        directory.folder = new Folder();

        DriveItemSearchCollectionPage founded = client
            .me()
            .drive()
            .root()
            .search(DriveItemSearchParameterSet.newBuilder().withQ("name:" + name).build())
            .buildRequest()
            .get();

        if (founded == null || (founded != null && founded.getCurrentPage().size() == 0)) {
            DriveItem createdDirectory = createDirectory(client, directory);

            return Output.builder()
                .uri(URI.create(createdDirectory.webUrl))
                .created(Boolean.TRUE)
                .build();
        }

       if (this.ifExists.equals(IfExists.SKIP)) {
            return Output.builder()
                .uri(URI.create(founded.getCurrentPage().get(0).webUrl))
                .build();
        } else {
            throw new RuntimeException("Directory " + founded.getCurrentPage().get(0).name + " already exists");
        }

    }

    @Nullable
    private static DriveItem createDirectory(GraphServiceClient<Request> client, DriveItem directory) {
        return client.me()
            .drive()
            .items()
            .appRoot()
            .buildRequest()
            .post(directory);
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        private URI uri;

        @Schema(
            title = "If the directory was created."
        )
        @Builder.Default
        private Boolean created = false;
    }

    public enum IfExists {
        ERROR,
        SKIP
    }

}
