package io.runtime.mcumgr.transfer;

public enum TransferState {
    NONE, TRANSFER, PAUSED;

    public boolean isInProgress() {
        return this != NONE;
    }

    public boolean isPaused() {
        return this == PAUSED;
    }
}
