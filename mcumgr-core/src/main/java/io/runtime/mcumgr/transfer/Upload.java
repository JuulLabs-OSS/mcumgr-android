package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.UploadResponse;

public class Upload extends Transfer {
    private Uploader uploader;
    private UploadCallback callback;
    private McuMgrCallback<UploadResponse> writeCallback;
    Upload(byte[] data,
           @NotNull Uploader uploader,
           @Nullable UploadCallback callback,
           @NotNull McuMgrCallback<UploadResponse> writeCallback) {
        super(data, 0);
        this.uploader = uploader;
        this.callback = callback;
        this.writeCallback = writeCallback;
    }

    @Override
    public void next(int offset) {
        uploader.write(mData, offset, writeCallback);
    }

    @Override
    public void reset() {
        mOffset = 0;
    }

    @Override
    public void onProgressChanged(int current, int total, long timestamp) {
        callback.onProgressChanged(current, total, timestamp);
    }

    @Override
    public void onFailed(McuMgrException e) {
        callback.onUploadFailed(e);
    }

    @Override
    public void onFinished() {
        callback.onUploadFinished();
    }

    @Override
    public void onCanceled() {
        callback.onUploadCanceled();
    }
}
