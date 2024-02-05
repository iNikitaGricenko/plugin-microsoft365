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

//
//    @Override
//    public InputStream get(String tenantId, URI uri) throws IOException {
//        return client.me()
//            .drive()
//            .root()
//            .itemWithPath(uri.getPath())
//            .content()
//            .buildRequest()
//            .get();
//    }
//
//    @Override
//    public List<URI> allByPrefix(String tenantId, URI prefix, boolean includeDirectories) throws IOException {
//        DriveSearchCollectionPage driveSearchCollectionPage = client
//            .me()
//            .drive()
//            .search(DriveSearchParameterSet.newBuilder().withQ(getPath(prefix)).build())
//            .buildRequest()
//            .get();
//
//        List<URI> result = new ArrayList<URI>();
//        for (DriveItem driveItem : driveSearchCollectionPage.getCurrentPage()) {
//            result.add(URI.create(driveItem.webUrl));
//        }
//
//        return result;
//    }
//
//    @Override
//    public List<FileAttributes> list(String tenantId, URI uri) throws IOException {
//        return null;
//    }
//
//    @Override
//    public FileAttributes getAttributes(String tenantId, URI uri) throws IOException {
//        return null;
//    }
//
//    @Override
//    public URI put(String tenantId, URI uri, InputStream data) throws IOException {
//        DriveItem driveItem = client.me()
//            .drive()
//            .root()
//            .itemWithPath(getPath(uri))
//            .content()
//            .buildRequest()
//            .put(data.readAllBytes());
//
//        return URI.create(driveItem.webUrl);
//    }
//
//    @Override
//    public boolean delete(String tenantId, URI uri) throws IOException {
//        DriveItem delete = client.me()
//            .drive()
//            .items(getPath(uri))
//            .buildRequest()
//            .delete();
//
//        String state = delete.deleted.state;
//
//        return false;
//    }
//
//    @Override
//    public URI createDirectory(String tenantId, URI uri) throws IOException {
//        client.me()
//            .drive()
//            .buildRequest();
//
//        return null;
//    }
//
//    @Override
//    public URI move(String tenantId, URI from, URI to) throws IOException {
//        return null;
//    }
//
//    @Override
//    public List<URI> deleteByPrefix(String tenantId, URI storagePrefix) throws IOException {
//        return null;
//    }

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
            DriveItem driveItem = client.me()
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
