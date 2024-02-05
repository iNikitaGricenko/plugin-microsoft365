package io.kestra.plugin.onedrive;

import com.microsoft.graph.models.DriveItem;
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
            code = {
                "uri: \"onedrive://dir/file.csv\""
            }
        )
    }
)
@Schema(
    title = "Delete a file from a OneDrive."
)
public class Delete extends AbstractOneDrive implements RunnableTask<Delete.Output> {

    @Schema(
        title = "The file to delete"
    )
    @PluginProperty(dynamic = true)
    private String uri;

    @Schema(
        title = "Raise an error if the file is not found"
    )
    @PluginProperty
    @Builder.Default
    private final Boolean errorOnMissing = false;

    @Override
    public Delete.Output run(RunContext runContext) throws Exception {
        GraphServiceClient<Request> client = this.client(runContext);

        URI uri = encode(runContext, this.uri);

        DriveItem deleted = client.me()
            .drive()
            .items(getPath(uri))
            .buildRequest()
            .delete();

        String state = deleted.deleted.state;

        return Output
            .builder()
            .uri(uri)
            .deleted(state != null)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "The deleted uri"
        )
        private final URI uri;

        @Schema(
            title = "If the files was really deleted"
        )
        private final Boolean deleted;
    }

}
