package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.UploadResponse;

public class Upload extends Transfer {

    @NotNull
    private Uploader mUploader;

    @Nullable
    private UploadCallback mUploadCallback;

    @NotNull
    private McuMgrCallback<UploadResponse> mWriteCallback;

    public Upload(byte[] data,
                  @NotNull Uploader uploader,
                  @Nullable UploadCallback callback,
                  @NotNull McuMgrCallback<UploadResponse> writeCallback) {
        super(data, 0);
        mUploader = uploader;
        mUploadCallback = callback;
        mWriteCallback = writeCallback;
    }

    @Override
    public void send(int offset) {
        if (mData == null) {
            throw new NullPointerException("Upload data cannot be null!");
        }
        mUploader.write(mData, offset, mWriteCallback);
    }

    @Override
    public void reset() {
        mOffset = 0;
    }

    @Override
    public void onProgressChanged(int current, int total, long timestamp) {
        if (mUploadCallback != null) {
            mUploadCallback.onProgressChanged(current, total, timestamp);
        }
    }

    @Override
    public void onFailed(McuMgrException e) {
        if (mUploadCallback != null) {
            mUploadCallback.onUploadFailed(e);
        }
    }

    @Override
    public void onFinished() {
        if (mUploadCallback != null) {
            mUploadCallback.onUploadFinished();
        }
    }

    @Override
    public void onCanceled() {
        if (mUploadCallback != null) {
            mUploadCallback.onUploadCanceled();
        }
    }
}
