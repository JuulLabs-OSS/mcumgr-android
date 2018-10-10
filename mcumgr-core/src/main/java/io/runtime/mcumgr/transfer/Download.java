package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.DownloadResponse;

public class Download extends Transfer {
    private String name;
    private McuMgrCallback<DownloadResponse> readCallback;
    private Downloader downloader;
    private DownloadCallback callback;
    public Download(@NotNull String name,
                    @NotNull Downloader downloader,
                    @Nullable DownloadCallback callback,
                    @NotNull McuMgrCallback<DownloadResponse> readCallback) {
        super(null, 0);
        this.name = name;
        this.downloader = downloader;
        this.readCallback = readCallback;
        this.callback = callback;
    }

    @Override
    public void next(int offset) {
        downloader.read(offset, readCallback);
    }

    @Override
    public void reset() {
        mOffset = 0;
        mData = null;
    }

    @Override
    public void onProgressChanged(int current, int total, long timestamp) {
        callback.onProgressChanged(current, total, timestamp);
    }

    @Override
    public void onFailed(McuMgrException e) {
        callback.onDownloadFailed(e);
    }

    @Override
    public void onFinished() {
        callback.onDownloadFinished(name, mData);
    }

    @Override
    public void onCanceled() {
        callback.onDownloadCanceled();
    }
}
