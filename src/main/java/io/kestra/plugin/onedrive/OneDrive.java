package io.kestra.plugin.onedrive;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveSearchParameterSet;
import com.microsoft.graph.requests.DriveSearchCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import io.kestra.core.storages.FileAttributes;
import io.kestra.core.storages.StorageInterface;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import okhttp3.Request;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Singleton
@OneDriveStorageEnabled
@Introspected
public class OneDrive implements StorageInterface {

    @Inject
    GraphServiceClient<Request> client;

    @Inject
    OneDriveConfig config;

    @Override
    public InputStream get(String tenantId, URI uri) throws IOException {
        return client.me()
            .drive()
            .root()
            .itemWithPath(uri.getPath())
            .content()
            .buildRequest()
            .get();
    }

    @Override
    public List<URI> allByPrefix(String tenantId, URI prefix, boolean includeDirectories) throws IOException {
        DriveSearchCollectionPage driveSearchCollectionPage = client
            .me()
            .drive()
            .search(DriveSearchParameterSet.newBuilder().withQ(getPath(prefix)).build())
            .buildRequest()
            .get();

        List<URI> result = new ArrayList<URI>();
        for (DriveItem driveItem : driveSearchCollectionPage.getCurrentPage()) {
            result.add(URI.create(driveItem.webUrl));
        }

        return result;
    }

    @Override
    public List<FileAttributes> list(String tenantId, URI uri) throws IOException {
        return null;
    }

    @Override
    public FileAttributes getAttributes(String tenantId, URI uri) throws IOException {
        return null;
    }

    @Override
    public URI put(String tenantId, URI uri, InputStream data) throws IOException {
        DriveItem driveItem = client.me()
            .drive()
            .root()
            .itemWithPath(getPath(uri))
            .content()
            .buildRequest()
            .put(data.readAllBytes());

        return URI.create(driveItem.webUrl);
    }

    @Override
    public boolean delete(String tenantId, URI uri) throws IOException {
        DriveItem delete = client.me()
            .drive()
            .items(getPath(uri))
            .buildRequest()
            .delete();

        String state = delete.deleted.state;

        return false;
    }

    @Override
    public URI createDirectory(String tenantId, URI uri) throws IOException {
        client.me()
            .drive()
            .buildRequest();

        return null;
    }

    @Override
    public URI move(String tenantId, URI from, URI to) throws IOException {
        return null;
    }

    @Override
    public List<URI> deleteByPrefix(String tenantId, URI storagePrefix) throws IOException {
        return null;
    }

    private String getPath(URI uri) {
        if (uri == null) {
            uri = URI.create("/");
        }

        parentTraversalValidation(uri);
        String path = uri.getPath();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path;
    }

    private void parentTraversalValidation(URI uri) {
        if (uri.toString().contains("..")) {
            throw new IllegalArgumentException("File should be accessed with their full path and not using relative '..' path.");
        }
    }

}
