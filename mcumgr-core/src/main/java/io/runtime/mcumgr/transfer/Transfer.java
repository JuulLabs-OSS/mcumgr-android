package io.runtime.mcumgr.transfer;

import org.jetbrains.annotations.Nullable;

public abstract class Transfer implements TransferCallback {

    @Nullable
    byte[] mData;

    int mOffset;

    Transfer(@Nullable byte[] data, int offset) {
        mData = data;
        mOffset = offset;
    }

    public abstract void reset();

    public abstract void send(int offset);

    public void sendNext() {
        send(mOffset);
    }

    @Nullable
    public byte[] getData() {
        return mData;
    }

    public void setData(@Nullable byte[] data) {
        mData = data;
    }

    public int getOffset() {
        return mOffset;
    }

    public void setOffset(int offset) {
        mOffset = offset;
    }

    public boolean isFinished() {
        return mData != null && mOffset == mData.length;
    }
}
