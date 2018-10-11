package io.runtime.mcumgr.transfer;

import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.DownloadResponse;

public interface Downloader {
    DownloadResponse read(int offset) throws McuMgrException;
}
