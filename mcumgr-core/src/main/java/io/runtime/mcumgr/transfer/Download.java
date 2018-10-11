package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.DownloadResponse;
import io.runtime.mcumgr.response.McuMgrResponse;

public class Download extends Transfer {

    @Nullable
    private String mName;

    @NotNull
    private Downloader mDownloader;

    @Nullable
    private DownloadCallback mDownloadCallback;

    public Download(@Nullable String name,
                    @NotNull Downloader downloader,
                    @Nullable DownloadCallback callback) {
        super(null, 0);
        mName = name;
        mDownloader = downloader;
        mDownloadCallback = callback;
    }

    @Override
    public McuMgrResponse send(int offset) throws McuMgrException {
        DownloadResponse response = mDownloader.read(offset);
        // Check for a McuManager error.
        if (response.rc != 0) {
            throw new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc));
        }

        // The first packet contains the file length.
        if (response.off == 0) {
            mData = new byte[response.len];
        }

        // Validate response body
        if (response.data == null) {
            throw new McuMgrException("Download response data is null.");
        }
        if (mData == null) {
            throw new McuMgrException("Download data is null.");
        }

        // Copy received mData to the buffer.
        System.arraycopy(response.data, 0, mData, response.off, response.data.length);
        mOffset = response.off + response.data.length;

        return response;
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
