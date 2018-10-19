package io.runtime.mcumgr.crash;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import io.runtime.mcumgr.util.ByteUtil;
import io.runtime.mcumgr.util.Endian;

@SuppressWarnings({"unused", "WeakerAccess"})
public class CoreDumpHeader {

    private static final int OFFSET = 0;

    private int mMagic;
    private int mSize;

    public CoreDumpHeader(int magic, int size) {
        mMagic = magic;
        mSize = size;
    }

    public int getMagic() {
        return mMagic;
    }

    public int getSize() {
        return mSize;
    }

    public static CoreDumpHeader fromBytes(@NotNull byte[] data) throws IOException {
        return fromBytes(data, OFFSET);
    }

    public static CoreDumpHeader fromBytes(@NotNull byte[] data, int offset) throws IOException {
        int magic = ByteUtil.byteArrayToUnsignedInt(data, offset, Endian.LITTLE, 4);
        if (magic != CoreDump.MAGIC) {
            throw new IOException("Illegal magic number: actual=" +
                    String.format("0x%x", magic) + ", expected=" +
                    String.format("0x%x", CoreDump.MAGIC));
        }
        int size = ByteUtil.byteArrayToUnsignedInt(data, offset + 4, Endian.LITTLE, 4);
        return new CoreDumpHeader(magic, size);
    }
}
