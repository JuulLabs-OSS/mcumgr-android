package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.UploadResponse;

public interface Uploader {
    UploadResponse write(@NotNull byte[] data, int offset) throws McuMgrException;
}
