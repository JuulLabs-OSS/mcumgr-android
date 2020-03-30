package io.runtime.mcumgr

import io.runtime.mcumgr.response.img.McuMgrImageStateResponse
import io.runtime.mcumgr.response.log.McuMgrLogResponse
import io.runtime.mcumgr.util.CBOR
import org.junit.Test
import java.io.IOException


class CBORTest {

    @Throws(IOException::class)
    @Test
    fun `text relaxed CBOR`() {
        val slot = CBOR.toBytes(McuMgrImageStateResponse.ImageSlot())
        println(CBOR.toObject(slot, McuMgrLogResponse.Entry::class.java))
    }
}