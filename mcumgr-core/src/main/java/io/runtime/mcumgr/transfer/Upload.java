package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.UploadResponse;

public class Upload extends Transfer {

    @NotNull
    private Uploader mUploader;

    @Nullable
    private UploadCallback mUploadCallback;

    public Upload(byte[] data,
                  @NotNull Uploader uploader,
                  @Nullable UploadCallback callback) {
        super(data, 0);
        mUploader = uploader;
        mUploadCallback = callback;
    }

    @Override
    public McuMgrResponse send(int offset) throws McuMgrException {
        if (mData == null) {
            throw new NullPointerException("Upload data cannot be null!");
        }
        UploadResponse response = mUploader.write(mData, offset);
        // Check for a McuManager error.
        if (response.rc != 0) {
            throw new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc));
        }

        mOffset = response.off;

        return response;
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
