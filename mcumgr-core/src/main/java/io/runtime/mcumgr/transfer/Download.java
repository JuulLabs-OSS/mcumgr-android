package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.DownloadResponse;

public class Download extends Transfer {

    @Nullable
    private String mName;

    @NotNull
    private McuMgrCallback<DownloadResponse> mReadCallback;

    @NotNull
    private Downloader mDownloader;

    @Nullable
    private DownloadCallback mDownloadCallback;

    public Download(@Nullable String name,
                    @NotNull Downloader downloader,
                    @Nullable DownloadCallback callback,
                    @NotNull McuMgrCallback<DownloadResponse> readCallback) {
        super(null, 0);
        this.mName = name;
        this.mDownloader = downloader;
        this.mReadCallback = readCallback;
        this.mDownloadCallback = callback;
    }

    @Override
    public void send(int offset) {
        mDownloader.read(offset, mReadCallback);
    }

    @Override
    public void reset() {
        mOffset = 0;
        mData = null;
    }

    @Override
    public void onProgressChanged(int current, int total, long timestamp) {
        if (mDownloadCallback != null) {
            mDownloadCallback.onProgressChanged(current, total, timestamp);
        }
    }

    @Override
    public void onFailed(McuMgrException e) {
        if (mDownloadCallback != null) {
            mDownloadCallback.onDownloadFailed(e);
        }
    }

    @Override
    public void onFinished() {
        if (mData == null) {
            throw new NullPointerException("Downloaded data cannot be null!");
        }
        if (mDownloadCallback != null) {
            mDownloadCallback.onDownloadFinished(mName, mData);
        }
    }

    @Override
    public void onCanceled() {
        if (mDownloadCallback != null) {
            mDownloadCallback.onDownloadCanceled();
        }
    }
}
