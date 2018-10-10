package io.runtime.mcumgr.transfer;

public interface TransferController {

    /**
     * Pause the transfer.
     */
    void pause();

    /**
     * Resume a paused transfer.
     */
    void resume();

    /**
     * Cancel the transfer.
     */
    void cancel();

    /**
     * Determine whether the transfer is paused.
     *
     * @return True if the transfer is paused, false otherwise.
     */
    boolean isPaused();

    /**
     * Determine whether the transfer is in progress.
     *
     * @return True if the transfer is in progress, false otherwise.
     */
    boolean isInProgress();
}
