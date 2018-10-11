package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.McuMgrCallback;

public interface Downloader {
    void read(int offset, @NotNull McuMgrCallback callback);
}
