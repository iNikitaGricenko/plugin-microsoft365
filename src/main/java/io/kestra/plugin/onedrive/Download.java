package io.kestra.plugin.onedrive;

import com.microsoft.graph.models.DriveItem;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Plugin(
    examples = {
        @Example(
            code = {
                "from: \"onedrive://dir/file.csv\""
            }
        )
    }
)
@Schema(
    title = "Download a file from a OneDrive."
)
public class Download extends AbstractOneDrive implements RunnableTask<Download.Output> {

    @Schema(
        title = "The file to copy"
    )
    @PluginProperty(dynamic = true)
    private String from;

    @Override
    public Download.Output run(RunContext runContext) throws Exception {
        GraphServiceClient<Request> client = this.client(runContext);

        URI uri = encode(runContext, this.from);

        DriveItem item = client
                .me()
                .drive()
                .root()
                .itemWithPath(getPath(uri))
                .buildRequest()
                .get();

        DriveItemContentStreamRequest request = client.me()
            .drive()
            .items(item.id)
            .content()
            .buildRequest();

        File tempFile = runContext.tempFile(runContext.fileExtension(item.file.oDataType)).toFile();

        try (
            InputStream inputStream = request.get();
            OutputStream outputStream = Files.newOutputStream(tempFile.toPath())
        ){
            inputStream.transferTo(outputStream);
        }

        return Output
            .builder()
            .path(item.name)
            .uri(runContext.storage().putFile(tempFile))
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "The path on the bucket of the downloaded file"
        )
        private final String path;

        @Schema(
            title = "The url of the downloaded file on kestra storage "
        )
        private final URI uri;
    }

}
