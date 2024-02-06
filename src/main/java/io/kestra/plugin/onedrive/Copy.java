package io.kestra.plugin.onedrive;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCopyParameterSet;
import com.microsoft.graph.requests.DriveItemContentStreamRequest;
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
@Schema(
    title = "Copy a file between directories",
    description = "Copy the file between Internal Storage or OneDrive file"
)
@Plugin(
    examples = {
        @Example(
            title = "Move a file between directory path",
            code = {
                "from: \"{{ inputs.file }}\"",
                "delete: true"
            }
        )
    }
)
public class Copy extends AbstractOneDrive implements RunnableTask<Copy.Output> {

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

    @Schema(
        title = "The destination path"
    )
    @Builder.Default
    private final boolean delete = false;

    @Override
    public Copy.Output run(RunContext runContext) throws Exception {
        GraphServiceClient<Request> client = this.client(runContext);

        URI from = encode(runContext, this.from);
        URI to = encode(runContext, this.to);

        DriveItem item = client
                .me()
                .drive()
                .root()
                .itemWithPath(getPath(from))
                .buildRequest()
                .get();

        DriveItem copied = client
            .me()
            .drive()
            .items(item.id)
            .copy(DriveItemCopyParameterSet.newBuilder().build())
            .buildRequest()
            .post();


        return Output
            .builder()
            .uri(new URI("onedrive://" + encode(copied.webUrl)))
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "The destination full uri",
            description = "The full url will be like `onedrive://{directory}/{path}/{file}`"
        )
        private URI uri;
    }

}
