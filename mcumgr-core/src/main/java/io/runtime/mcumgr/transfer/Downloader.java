package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.response.DownloadResponse;

public interface Downloader {
    void read(int offset, @NotNull McuMgrCallback<DownloadResponse> callback);
}
