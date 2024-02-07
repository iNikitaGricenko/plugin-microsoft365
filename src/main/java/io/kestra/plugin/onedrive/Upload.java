package io.kestra.plugin.onedrive;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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

import java.io.File;
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
                "from: \"{{ inputs.file }}\"",
                "to: \"onedrive://dir/file.csv\""
            }
        )
    }
)
@Schema(
    title = "Upload a file to a OneDrive."
)
public class Upload extends AbstractOneDrive implements RunnableTask<Upload.Output> {

    @Schema(
        title = "The file to copy"
    )
    @PluginProperty(dynamic = true)
    private String from;

    @Schema(
        title = "The destination path"
    )
    @PluginProperty(dynamic = true)
    private String to;

    @Override
    public Upload.Output run(RunContext runContext) throws Exception {
        GraphServiceClient<Request> client = this.client(runContext);

        URI from = encode(runContext, this.from);
        URI to = encode(runContext, this.to);

        try (InputStream data = runContext.storage().getFile(from)) {
            DriveItem upload = new DriveItem();
            upload.name = new File(to.getPath()).getName();
            upload.file = new com.microsoft.graph.models.File();

            JsonObject renameProperty = new JsonObject();
            renameProperty.add("rename", new JsonPrimitive(true));

            upload.additionalDataManager().put("@microsoft.graph.conflictBehavior", renameProperty);

            DriveItem driveItem = client
                .me()
                .drive()
                .root()
                .itemWithPath(getPath(to))
                .content()
                .buildRequest()
                .put(data.readAllBytes());

            if (driveItem != null && driveItem.size != null) {
                runContext.metric(Counter.of("file.size", driveItem.size));
            }

            return Output
                .builder()
                .uri(new URI("onedrive://" + encode(driveItem.webUrl)))
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        private URI uri;
    }

}
