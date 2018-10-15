package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.UploadResponse;

public abstract class Upload extends Transfer {

    protected Upload(byte[] data) {
        super(data, 0);
    }

    protected abstract UploadResponse write(@NotNull byte[] data, int offset) throws McuMgrException;

    @Override
    public McuMgrResponse send(int offset) throws McuMgrException {
        if (mData == null) {
            throw new NullPointerException("Upload data cannot be null!");
        }
        UploadResponse response = write(mData, offset);
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
}
