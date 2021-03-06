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
package com.microsoftopentechnologies.intellij.helpers.azure.sdk;

import com.microsoft.windowsazure.exception.ServiceException;
import com.microsoftopentechnologies.intellij.components.MSOpenTechToolsApplication;
import com.microsoftopentechnologies.intellij.components.PluginSettings;
import com.microsoftopentechnologies.intellij.helpers.CallableSingleArg;
import com.microsoftopentechnologies.intellij.helpers.StringHelper;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationContext;
import com.microsoftopentechnologies.intellij.helpers.aadauth.AuthenticationResult;
import com.microsoftopentechnologies.intellij.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIHelper;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManager;
import com.microsoftopentechnologies.intellij.helpers.azure.rest.AzureRestAPIManagerImpl;
import com.microsoftopentechnologies.intellij.model.storage.*;
import com.microsoftopentechnologies.intellij.model.vm.*;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;

public class AzureSDKManagerADAuthDecorator implements AzureSDKManager {
    protected AzureSDKManager sdkManager;

    public AzureSDKManagerADAuthDecorator(AzureSDKManager sdkManager) {
        this.sdkManager = sdkManager;
    }

    private interface Func0<T> {
        abstract T run() throws AzureCmdException;
    }

    protected <T> T runWithRetry(String subscriptionId, Func0<T> func) throws AzureCmdException {
        try {
            return func.run();
        } catch (AzureCmdException e) {
            Throwable throwable = e.getCause();
            if (throwable == null)
                throw e;
            if (!(throwable instanceof ServiceException))
                throw e;

            ServiceException serviceException = (ServiceException) throwable;
            if (serviceException.getHttpStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                // attempt token refresh
                if (refreshAccessToken(subscriptionId)) {
                    // retry request
                    return func.run();
                }
            }

            throw e;
        }
    }

    private boolean refreshAccessToken(String subscriptionId) {
        PluginSettings settings = MSOpenTechToolsApplication.getCurrent().getSettings();
        AzureRestAPIManager apiManager = AzureRestAPIManagerImpl.getManager();
        AuthenticationResult token = apiManager.getAuthenticationTokenForSubscription(subscriptionId);

        // check if we have a refresh token to redeem
        if (token != null && !StringHelper.isNullOrWhiteSpace(token.getRefreshToken())) {
            AuthenticationContext context = null;
            try {
                context = new AuthenticationContext(settings.getAdAuthority());
                token = context.acquireTokenByRefreshToken(
                        token,
                        AzureRestAPIHelper.getTenantName(subscriptionId),
                        settings.getAzureServiceManagementUri(),
                        settings.getClientId());
            } catch (Exception e) {
                // if the error is HTTP status code 400 then we need to
                // do interactive auth
                if (e.getMessage().contains("HTTP status code 400")) {
                    try {
                        token = AzureRestAPIHelper.acquireTokenInteractive(subscriptionId, apiManager);
                    } catch (Exception ignored) {
                        token = null;
                    }
                } else {
                    token = null;
                }
            } finally {
                if (context != null) {
                    context.dispose();
                }
            }

            if (token != null) {
                apiManager.setAuthenticationTokenForSubscription(subscriptionId, token);
                return true;
            }
        }

        return false;
    }

    @NotNull
    @Override
    public List<CloudService> getCloudServices(@NotNull final String subscriptionId) throws AzureCmdException {
        return runWithRetry(subscriptionId, new Func0<List<CloudService>>() {
            @Override
            public List<CloudService> run() throws AzureCmdException {
                return sdkManager.getCloudServices(subscriptionId);
            }
        });
    }

    @NotNull
    @Override
    public List<VirtualMachine> getVirtualMachines(@NotNull final String subscriptionId) throws AzureCmdException {
        return runWithRetry(subscriptionId, new Func0<List<VirtualMachine>>() {
            @Override
            public List<VirtualMachine> run() throws AzureCmdException {
                return sdkManager.getVirtualMachines(subscriptionId);
            }
        });
    }

    @NotNull
    @Override
    public VirtualMachine refreshVirtualMachineInformation(@NotNull final VirtualMachine vm) throws AzureCmdException {
        return runWithRetry(vm.getSubscriptionId(), new Func0<VirtualMachine>() {
            @Override
            public VirtualMachine run() throws AzureCmdException {
                return sdkManager.refreshVirtualMachineInformation(vm);
            }
        });
    }

    @Override
    public void startVirtualMachine(@NotNull final VirtualMachine vm) throws AzureCmdException {
        runWithRetry(vm.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.startVirtualMachine(vm);
                return null;
            }
        });
    }

    @Override
    public void shutdownVirtualMachine(@NotNull final VirtualMachine vm, final boolean deallocate) throws AzureCmdException {
        runWithRetry(vm.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.shutdownVirtualMachine(vm, deallocate);
                return null;
            }
        });
    }

    @Override
    public void restartVirtualMachine(@NotNull final VirtualMachine vm) throws AzureCmdException {
        runWithRetry(vm.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.restartVirtualMachine(vm);
                return null;
            }
        });
    }

    @Override
    public void deleteVirtualMachine(@NotNull final VirtualMachine vm, final boolean deleteFromStorage) throws AzureCmdException {
        runWithRetry(vm.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.deleteVirtualMachine(vm, deleteFromStorage);
                return null;
            }
        });
    }

    @NotNull
    @Override
    public byte[] downloadRDP(@NotNull final VirtualMachine vm) throws AzureCmdException {
        return runWithRetry(vm.getSubscriptionId(), new Func0<byte[]>() {
            @Override
            public byte[] run() throws AzureCmdException {
                return sdkManager.downloadRDP(vm);
            }
        });
    }

    @NotNull
    @Override
    public List<StorageAccount> getStorageAccounts(@NotNull final String subscriptionId) throws AzureCmdException {
        return runWithRetry(subscriptionId, new Func0<List<StorageAccount>>() {
            @Override
            public List<StorageAccount> run() throws AzureCmdException {
                return sdkManager.getStorageAccounts(subscriptionId);
            }
        });
    }

    @NotNull
    @Override
    public List<VirtualMachineImage> getVirtualMachineImages(@NotNull final String subscriptionId) throws AzureCmdException {
        return runWithRetry(subscriptionId, new Func0<List<VirtualMachineImage>>() {
            @Override
            public List<VirtualMachineImage> run() throws AzureCmdException {
                return sdkManager.getVirtualMachineImages(subscriptionId);
            }
        });
    }

    @NotNull
    @Override
    public List<VirtualMachineSize> getVirtualMachineSizes(@NotNull final String subscriptionId) throws AzureCmdException {
        return runWithRetry(subscriptionId, new Func0<List<VirtualMachineSize>>() {
            @Override
            public List<VirtualMachineSize> run() throws AzureCmdException {
                return sdkManager.getVirtualMachineSizes(subscriptionId);
            }
        });
    }

    @NotNull
    @Override
    public List<Location> getLocations(@NotNull final String subscriptionId) throws AzureCmdException {
        return runWithRetry(subscriptionId, new Func0<List<Location>>() {
            @Override
            public List<Location> run() throws AzureCmdException {
                return sdkManager.getLocations(subscriptionId);
            }
        });
    }

    @NotNull
    @Override
    public List<AffinityGroup> getAffinityGroups(@NotNull final String subscriptionId) throws AzureCmdException {
        return runWithRetry(subscriptionId, new Func0<List<AffinityGroup>>() {
            @Override
            public List<AffinityGroup> run() throws AzureCmdException {
                return sdkManager.getAffinityGroups(subscriptionId);
            }
        });
    }

    @NotNull
    @Override
    public List<VirtualNetwork> getVirtualNetworks(@NotNull final String subscriptionId) throws AzureCmdException {
        return runWithRetry(subscriptionId, new Func0<List<VirtualNetwork>>() {
            @Override
            public List<VirtualNetwork> run() throws AzureCmdException {
                return sdkManager.getVirtualNetworks(subscriptionId);
            }
        });
    }

    @Override
    public void createStorageAccount(@NotNull final StorageAccount storageAccount) throws AzureCmdException {
        runWithRetry(storageAccount.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.createStorageAccount(storageAccount);
                return null;
            }
        });
    }

    @Override
    public void createCloudService(@NotNull final CloudService cloudService) throws AzureCmdException {
        runWithRetry(cloudService.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.createCloudService(cloudService);
                return null;
            }
        });
    }

    @Override
    public void createVirtualMachine(@NotNull final VirtualMachine virtualMachine,
                                     @NotNull final VirtualMachineImage vmImage,
                                     @NotNull final StorageAccount storageAccount,
                                     @NotNull final String virtualNetwork,
                                     @NotNull final String username,
                                     @NotNull final String password,
                                     @NotNull final byte[] certificate)
            throws AzureCmdException {
        runWithRetry(virtualMachine.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.createVirtualMachine(virtualMachine, vmImage, storageAccount, virtualNetwork,
                        username, password, certificate);
                return null;
            }
        });
    }

    @Override
    public void createVirtualMachine(@NotNull final VirtualMachine virtualMachine,
                                     @NotNull final VirtualMachineImage vmImage,
                                     @NotNull final String mediaLocation,
                                     @NotNull final String virtualNetwork,
                                     @NotNull final String username,
                                     @NotNull final String password,
                                     @NotNull final byte[] certificate)
            throws AzureCmdException {
        runWithRetry(virtualMachine.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.createVirtualMachine(virtualMachine, vmImage, mediaLocation, virtualNetwork,
                        username, password, certificate);
                return null;
            }
        });
    }

    @NotNull
    @Override
    public StorageAccount refreshStorageAccountInformation(@NotNull final StorageAccount storageAccount)
            throws AzureCmdException {
        return runWithRetry(storageAccount.getSubscriptionId(), new Func0<StorageAccount>() {
            @Override
            public StorageAccount run() throws AzureCmdException {
                return sdkManager.refreshStorageAccountInformation(storageAccount);
            }
        });
    }

    @Override
    public String createServiceCertificate(@NotNull final String subscriptionId, @NotNull final String serviceName,
                                           @NotNull final byte[] data, @NotNull final String password)
            throws AzureCmdException {
        return runWithRetry(subscriptionId, new Func0<String>() {
            @Override
            public String run() throws AzureCmdException {
                return sdkManager.createServiceCertificate(subscriptionId, serviceName, data, password);
            }
        });
    }

    @Override
    public void deleteStorageAccount(@NotNull final StorageAccount storageAccount) throws AzureCmdException {
        runWithRetry(storageAccount.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.deleteStorageAccount(storageAccount);
                return null;
            }
        });
    }

    @NotNull
    @Override
    public List<BlobContainer> getBlobContainers(@NotNull final StorageAccount storageAccount)
            throws AzureCmdException {
        return runWithRetry(storageAccount.getSubscriptionId(), new Func0<List<BlobContainer>>() {
            @Override
            public List<BlobContainer> run() throws AzureCmdException {
                return sdkManager.getBlobContainers(storageAccount);
            }
        });
    }

    @NotNull
    @Override
    public BlobContainer createBlobContainer(@NotNull final StorageAccount storageAccount,
                                             @NotNull final BlobContainer blobContainer)
            throws AzureCmdException {
        return runWithRetry(storageAccount.getSubscriptionId(), new Func0<BlobContainer>() {
            @Override
            public BlobContainer run() throws AzureCmdException {
                return sdkManager.createBlobContainer(storageAccount, blobContainer);
            }
        });
    }

    @Override
    public void deleteBlobContainer(@NotNull final StorageAccount storageAccount,
                                    @NotNull final BlobContainer blobContainer)
            throws AzureCmdException {
        runWithRetry(storageAccount.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.deleteBlobContainer(storageAccount, blobContainer);
                return null;
            }
        });
    }

    @NotNull
    @Override
    public BlobDirectory getRootDirectory(@NotNull final StorageAccount storageAccount,
                                          @NotNull final BlobContainer blobContainer)
            throws AzureCmdException {
        return runWithRetry(storageAccount.getSubscriptionId(), new Func0<BlobDirectory>() {
            @Override
            public BlobDirectory run() throws AzureCmdException {
                return sdkManager.getRootDirectory(storageAccount, blobContainer);
            }
        });
    }

    @NotNull
    @Override
    public List<BlobItem> getBlobItems(@NotNull final StorageAccount storageAccount,
                                       @NotNull final BlobDirectory blobDirectory)
            throws AzureCmdException {
        return runWithRetry(storageAccount.getSubscriptionId(), new Func0<List<BlobItem>>() {
            @Override
            public List<BlobItem> run() throws AzureCmdException {
                return sdkManager.getBlobItems(storageAccount, blobDirectory);
            }
        });
    }

    @NotNull
    @Override
    public BlobDirectory createBlobDirectory(@NotNull final StorageAccount storageAccount,
                                             @NotNull final BlobDirectory parentBlobDirectory,
                                             @NotNull final BlobDirectory blobDirectory)
            throws AzureCmdException {
        return runWithRetry(storageAccount.getSubscriptionId(), new Func0<BlobDirectory>() {
            @Override
            public BlobDirectory run() throws AzureCmdException {
                return sdkManager.createBlobDirectory(storageAccount, parentBlobDirectory, blobDirectory);
            }
        });
    }

    @NotNull
    @Override
    public BlobFile createBlobFile(@NotNull final StorageAccount storageAccount,
                                   @NotNull final BlobDirectory parentBlobDirectory,
                                   @NotNull final BlobFile blobFile)
            throws AzureCmdException {
        return runWithRetry(storageAccount.getSubscriptionId(), new Func0<BlobFile>() {
            @Override
            public BlobFile run() throws AzureCmdException {
                return sdkManager.createBlobFile(storageAccount, parentBlobDirectory, blobFile);
            }
        });
    }

    @Override
    public void deleteBlobFile(@NotNull final StorageAccount storageAccount,
                               @NotNull final BlobFile blobFile)
            throws AzureCmdException {
        runWithRetry(storageAccount.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.deleteBlobFile(storageAccount, blobFile);
                return null;
            }
        });
    }


    @Override
    public void uploadBlobFileContent(final @NotNull StorageAccount storageAccount,
                                      final @NotNull BlobContainer blobContainer,
                                      final @NotNull String filePath,
                                      final @NotNull InputStream content,
                                      final CallableSingleArg<Void, Long> processBlock,
                                      final long maxBlockSize,
                                      final long length) throws AzureCmdException {
        runWithRetry(storageAccount.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.uploadBlobFileContent(storageAccount, blobContainer, filePath, content, processBlock, maxBlockSize, length);
                return null;
            }
        });
    }

    @Override
    public void downloadBlobFileContent(@NotNull final StorageAccount storageAccount,
                                        @NotNull final BlobFile blobFile,
                                        @NotNull final OutputStream content)
            throws AzureCmdException {
        runWithRetry(storageAccount.getSubscriptionId(), new Func0<Void>() {
            @Override
            public Void run() throws AzureCmdException {
                sdkManager.downloadBlobFileContent(storageAccount, blobFile, content);
                return null;
            }
        });
    }
}