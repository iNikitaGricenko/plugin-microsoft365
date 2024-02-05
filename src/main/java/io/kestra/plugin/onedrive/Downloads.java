package io.kestra.plugin.onedrive;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemSearchParameterSet;
import com.microsoft.graph.requests.DriveItemContentStreamRequest;
import com.microsoft.graph.requests.DriveItemSearchCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Plugin(
    examples = {
        @Example(
            title = "Download a list of files and move it to an archive folders",
            code = {
                "from: onedrive://my-bucket/kestra/files/",
            }
        )
    }
)
@Schema(
    title = "Download multiple files from a OneDrive."
)
public class Downloads extends AbstractOneDrive implements RunnableTask<Downloads.Output> {

    private String name;

    @Override
    public Downloads.Output run(RunContext runContext) throws Exception {
        GraphServiceClient<Request> client = this.client(runContext);

        DriveItemSearchCollectionPage collectionPage = client
            .me()
            .drive()
            .root()
            .search(DriveItemSearchParameterSet.newBuilder().withQ("name:" + name).build())
            .buildRequest()
            .get();

        List<File> files = collectionPage.getCurrentPage()
            .stream()
            .map(throwFunction(item -> download(runContext, client, item)))
            .toList();

        List<URI> uriList = files
            .stream()
            .map(throwFunction(file -> runContext.storage().putFile(file)))
            .toList();

        return Output
            .builder()
            .uris(uriList)
            .build();
    }

    @NotNull
    private static File download(RunContext runContext, GraphServiceClient<Request> client, DriveItem item) throws IOException {
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
        return tempFile;
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "The url of the downloaded files on kestra storage "
        )
        private final List<URI> uris;
    }

}
