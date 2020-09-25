package io.runtime.mcumgr.transport.ble

import io.runtime.mcumgr.McuManager
import io.runtime.mcumgr.McuMgrHeader
import io.runtime.mcumgr.McuMgrScheme
import io.runtime.mcumgr.ble.callback.SmpProtocolSession
import io.runtime.mcumgr.ble.callback.SmpTransaction
import io.runtime.mcumgr.response.McuMgrResponse
import io.runtime.mcumgr.response.dflt.McuMgrEchoResponse
import io.runtime.mcumgr.util.CBOR
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class SmpProtocolSessionTest {

    private val session = SmpProtocolSession()

    private val echoTransaction = object : SmpTransaction {

        val result = Channel<ByteArray>(Channel.RENDEZVOUS)

        override fun send(data: ByteArray) {
            val header = McuMgrHeader.fromBytes(data)
            val payload = data.copyOfRange(8, data.size)
            val echo = CBOR.getString(payload, "d")
            val response = McuManager.buildPacket(
                McuMgrScheme.BLE,
                3, header.flags, header.groupId, header.sequenceNum, header.commandId,
                mapOf("r" to echo)
            )
            session.receive(response)
        }

        override fun onResponse(data: ByteArray) {
            result.offer(data)
        }

        override fun onFailure(e: Throwable) {
            result.close(e)
        }
    }

    @Test
    fun `send and receive, success`() = runBlocking {
        val echo = "Hello!"
        val request = McuManager.buildPacket(
            McuMgrScheme.BLE,
            0, 0, 0, 0, 0,
            mapOf("d" to echo)
        )
        session.send(request, echoTransaction)
        val responseData = echoTransaction.result.receive()
        val response = McuMgrResponse.buildResponse(
            McuMgrScheme.BLE,
            responseData,
            McuMgrEchoResponse::class.java
        )
        assertEquals(echo, response.r)
    }

    @Test
    fun `send, counter increase and rollover`() = runBlocking {
        val echo = "Hello!"
        val request = newEchoRequest(echo)
        repeat(256) { i ->
            session.send(request, echoTransaction)
            val responseData = echoTransaction.result.receive()
            val response = McuMgrResponse.buildResponse(
                McuMgrScheme.BLE,
                responseData,
                McuMgrEchoResponse::class.java
            )
            assertEquals(echo, response.r)
            val expected = if (i < 256) {
                i
            } else {
                0
            }
            assertEquals(expected, response.header?.sequenceNum)
        }

    }
}

private fun newEchoRequest(echo: String): ByteArray {
    return McuManager.buildPacket(
        McuMgrScheme.BLE,
        0, 0, 0, 0, 0,
        mapOf("d" to echo)
    )
}
