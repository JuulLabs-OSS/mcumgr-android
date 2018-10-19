package io.runtime.mcumgr.crash;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public class CoreDumpTlv {

    private static final int OFFSET = 8;

    private List<CoreDumpTlvEntry> mEntries;

    public CoreDumpTlv(@NotNull List<CoreDumpTlvEntry> entries) {
        mEntries = entries;
    }

    @Nullable
    public CoreDumpTlvEntry getEntryOfType(int type) {
        for (CoreDumpTlvEntry entry : mEntries) {
            if (entry.getType() == type) {
                return entry;
            }
        }
        return null;
    }

    @NotNull
    public List<CoreDumpTlvEntry> getEntriesOfType(int type) {
        List<CoreDumpTlvEntry> entries = new ArrayList<>();
        for (CoreDumpTlvEntry entry : mEntries) {
            if (entry.getType() == type) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<CoreDumpTlvEntry> getEntries() {
        return mEntries;
    }

    public int getSize() {
        int size = 0;
        for (CoreDumpTlvEntry entry : mEntries) {
            size += entry.getSize();
        }
        return size;
    }

    @NotNull
    public static CoreDumpTlv fromBytes(@NotNull byte[] data) throws IOException {
        return fromBytes(data, OFFSET);
    }

    @NotNull
    public static CoreDumpTlv fromBytes(@NotNull byte[] data, int offset) throws IOException {
        List<CoreDumpTlvEntry> entries = new ArrayList<>();
        while (offset < data.length) {
            CoreDumpTlvEntry entry = CoreDumpTlvEntry.fromBytes(data, offset);
            entries.add(entry);
            offset += entry.getSize();
        }
        return new CoreDumpTlv(entries);
    }
}
