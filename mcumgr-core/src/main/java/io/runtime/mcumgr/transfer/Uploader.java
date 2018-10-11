package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.response.UploadResponse;

public interface Uploader {
    void write(@NotNull byte[] data, int offset, @NotNull McuMgrCallback<UploadResponse> callback);
}
