package io.runtime.mcumgr.transfer;


import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.DownloadResponse;
import io.runtime.mcumgr.response.McuMgrResponse;

public abstract class Download extends Transfer {

    protected Download() {
        super(null, 0);
    }

    protected abstract DownloadResponse read(int offset) throws McuMgrException;

    @Override
    public McuMgrResponse send(int offset) throws McuMgrException {
        DownloadResponse response = read(offset);
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
}
