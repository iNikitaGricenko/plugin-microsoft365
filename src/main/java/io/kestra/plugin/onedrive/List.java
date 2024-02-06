package io.kestra.plugin.onedrive;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemSearchParameterSet;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.DriveItemContentStreamRequest;
import com.microsoft.graph.requests.DriveItemSearchCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
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
import java.util.ArrayList;
import java.util.Collection;

import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Plugin(
    examples = {
        @Example(
            title = "List files in a bucket",
            code = {
                "from: \"onedrive://my_directory/dir/\""
            }
        )
    }
)
@Schema(
    title = "List file on a OneDrive directory."
)
public class List extends AbstractOneDrive implements RunnableTask<List.Output> {

    @Schema(
        title = "The directory to list"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private String from;

    @Override
    public List.Output run(RunContext runContext) throws Exception {
        GraphServiceClient<Request> client = this.client(runContext);

        URI from = encode(runContext, this.from);
        Collection<DriveItem> items = new ArrayList<>();

        DriveItem directory = client.me()
            .drive()
            .root()
            .itemWithPath(getPath(from))
            .buildRequest()
            .get();

        DriveItemCollectionPage childItems = client.me()
            .drive()
            .items(directory.id)
            .children()
            .buildRequest()
            .get();

        for (DriveItem childItem : childItems.getCurrentPage()) {
            if (childItem.folder == null) {
                items.add(childItem);
            }
        }

        return Output
            .builder()
            .items(items)
            .build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "The list of the files in OneDrive directory"
        )
        private final Collection<DriveItem> items;
    }

}
