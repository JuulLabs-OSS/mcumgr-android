package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.DownloadResponse;
import io.runtime.mcumgr.response.UploadResponse;

public class TransferManager extends McuManager implements TransferController {

    private TransferState mTransferState;
    private Transfer mTransfer;

    /**
     * Construct a McuManager instance.
     *
     * @param groupId     the group ID of this Mcu Manager instance.
     * @param transporter the transporter to use to send commands.
     */
    protected TransferManager(int groupId, @NotNull McuMgrTransport transporter) {
        super(groupId, transporter);
    }

    public synchronized boolean startUpload(@NotNull byte[] data,
                                            @NotNull Uploader uploader,
                                            @Nullable UploadCallback callback) {
        return startTransfer(new Upload(data, uploader, callback, mUploadCallback));
    }

    public synchronized boolean startDownload(@NotNull String name,
                                              @NotNull Downloader downloader,
                                              @Nullable DownloadCallback callback) {
        return startTransfer(new Download(name, downloader, callback, mDownloadCallback));
    }

    public synchronized boolean startTransfer(Transfer transfer) {
        if (mTransferState != TransferState.NONE) {
            return false;
        }
        mTransferState = TransferState.TRANSFER;
        mTransfer = transfer;
        mTransfer.next();
        return true;
    }

    @Override
    public synchronized void pause() {
        if (mTransferState == TransferState.TRANSFER) {
            mTransferState = TransferState.PAUSED;
        }
    }

    @Override
    public synchronized void resume() {
        if (mTransferState.isPaused()) {
            mTransferState = TransferState.TRANSFER;
            mTransfer.next();
        }
    }

    @Override
    public synchronized void cancel() {
        if (mTransferState == TransferState.PAUSED) {
            // If the transfer is paused call the callback immediately.
            mTransferState = TransferState.NONE;
            mTransfer.onCanceled();
        } else if (mTransferState == TransferState.TRANSFER) {
            // If the transfer is in mid transfer, set the state to NONE. The McuMgrCallback
            // which handles the response will call the callback and end the transfer.
            mTransferState = TransferState.NONE;
        }
    }

    @Override
    public boolean isPaused() {
        return mTransferState.isPaused();
    }

    @Override
    public boolean isInProgress() {
        return mTransferState.isInProgress();
    }

    //******************************************************************
    // Implementation
    //******************************************************************

    private synchronized void failTransfer(McuMgrException e) {
        mTransfer.onFailed(e);
        endTransfer();
    }

    private synchronized void restartTransfer() {
        resetTransfer();
        startTransfer(mTransfer);
    }

    private synchronized void resetTransfer() {
        mTransferState = TransferState.NONE;
        mTransfer.reset();
    }

    private synchronized void endTransfer() {
        mTransferState = TransferState.NONE;
        mTransfer = null;
    }

    private final McuMgrCallback<UploadResponse> mUploadCallback = new McuMgrCallback<UploadResponse>() {
        @Override
        public void onResponse(@NotNull UploadResponse response) {
            // Check for a McuManager error
            if (response.rc != 0) {
                failTransfer(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                return;
            }

            // Check if upload hasn't been cancelled.
            if (mTransferState == TransferState.NONE) {
                mTransfer.onCanceled();
                endTransfer();
                return;
            }

            // Set the transfer's offset
            mTransfer.setOffset(response.off);

            // Call the progress callback.
            mTransfer.onProgressChanged(response.off, mTransfer.getData().length,
                    System.currentTimeMillis());

            // Check if the upload has finished.
            if (mTransfer.isFinished()) {
                mTransferState = TransferState.NONE;
                mTransfer.onFinished();
                endTransfer();
                return;
            }

            // Send the next packet of upload mData from the mOffset provided in the response.
            mTransfer.next();
        }

        @Override
        public void onError(@NotNull McuMgrException error) {
            // Check if the exception is due to an insufficient MTU.
            if (error instanceof InsufficientMtuException) {
                InsufficientMtuException mtuErr = (InsufficientMtuException) error;

                // Set the MTU to the value specified in the error response.
                int mtu = mtuErr.getMtu();
                if (mMtu == mtu) {
                    mtu -= 1;
                }
                boolean isMtuSet = setUploadMtu(mtu);

                if (isMtuSet) {
                    // If the MTU has been set successfully, restart the upload.
                    restartTransfer();
                    return;
                }
            }
            // If the exception is not due to insufficient MTU fail the upload.
            failTransfer(error);
        }
    };

    private final McuMgrCallback<DownloadResponse> mDownloadCallback = new McuMgrCallback<DownloadResponse>() {
        @Override
        public void onResponse(@NotNull DownloadResponse response) {
            // Check for a McuManager error.
            if (response.rc != 0) {
                failTransfer(new McuMgrErrorException(McuMgrErrorCode.valueOf(response.rc)));
                return;
            }

            // Check if download hasn't been cancelled.
            if (mTransferState == TransferState.NONE) {
                mTransfer.onCanceled();
                endTransfer();
                return;
            }

//            // Set the transfer's offset
//            mTransfer.setOffset(response.off);

            // The first packet contains the file length.
            if (response.off == 0) {
                mTransfer.setData(new byte[response.len]);
            }

            // Copy received mData to the buffer.
            System.arraycopy(response.data, 0, mTransfer.getData(), response.off, response.data.length);
            mTransfer.setOffset(response.off + response.data.length);

            // Call the progress callback.
            mTransfer.onProgressChanged(mTransfer.getOffset(), mTransfer.getData().length,
                    System.currentTimeMillis());

            // Check if the download has finished.
            if (mTransfer.isFinished()) {
                mTransferState = TransferState.NONE;
                mTransfer.onFinished();
                endTransfer();
                return;
            }

            // Send the next packet of upload mData from the mOffset provided in the response.
            mTransfer.next();
        }

        @Override
        public void onError(@NotNull McuMgrException error) {
            // Check if the exception is due to an insufficient MTU.
            if (error instanceof InsufficientMtuException) {
                InsufficientMtuException mtuErr = (InsufficientMtuException) error;

                // Set the MTU to the value specified in the error response.
                int mtu = mtuErr.getMtu();
                if (mMtu == mtu) {
                    mtu -= 1;
                }
                boolean isMtuSet = setUploadMtu(mtu);

                if (isMtuSet) {
                    // If the MTU has been set successfully, restart the upload.
                    restartTransfer();
                    return;
                }
            }
            // If the exception is not due to insufficient MTU fail the upload.
            failTransfer(error);
        }
    };
}
