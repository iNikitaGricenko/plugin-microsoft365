package io.kestra.plugin.onedrive;

import io.micronaut.context.annotation.Requires;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Requires(property = "kestra.storage.type", value = "onedrive")
public @interface OneDriveStorageEnabled {
}
