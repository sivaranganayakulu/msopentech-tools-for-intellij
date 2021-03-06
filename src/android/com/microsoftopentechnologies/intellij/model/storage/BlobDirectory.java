/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.microsoftopentechnologies.intellij.model.storage;

import com.microsoftopentechnologies.intellij.model.ServiceTreeItem;
import org.jetbrains.annotations.NotNull;

public class BlobDirectory implements ServiceTreeItem, BlobItem {
    private boolean loading;
    private String name;
    private String uri;
    private String containerName;
    private String path;
    private String subscriptionId;

    public BlobDirectory(@NotNull String name,
                         @NotNull String uri,
                         @NotNull String containerName,
                         @NotNull String path,
                         @NotNull String subscriptionId) {
        this.name = name;
        this.uri = uri;
        this.containerName = containerName;
        this.path = path;
        this.subscriptionId = subscriptionId;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getUri() {
        return uri;
    }

    public void setUri(@NotNull String uri) {
        this.uri = uri;
    }

    @NotNull
    @Override
    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(@NotNull String containerName) {
        this.containerName = containerName;
    }

    @NotNull
    @Override
    public String getPath() {
        return path;
    }

    public void setPath(@NotNull String path) {
        this.path = path;
    }

    @NotNull
    @Override
    public BlobItemType getItemType() {
        return BlobItemType.BlobDirectory;
    }

    @NotNull
    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }
}