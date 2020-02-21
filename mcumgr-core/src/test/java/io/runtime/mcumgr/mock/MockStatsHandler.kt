package io.runtime.mcumgr.mock

import io.runtime.mcumgr.McuMgrErrorCode
import io.runtime.mcumgr.McuMgrHeader
import io.runtime.mcumgr.response.McuMgrResponse
import io.runtime.mcumgr.response.stat.McuMgrStatListResponse
import io.runtime.mcumgr.response.stat.McuMgrStatResponse
import io.runtime.mcumgr.util.CBOR

enum class McuMgrStatsCommand(val value: Int) {
    READ(0),
    LIST (1)
}

class MockStatsHandler(
    private val stats: Map<String, Map<String, Long>>
): McuMgrHandler {

    /**
     * Handle a request for the stats group
     */
    override fun <T : McuMgrResponse?> handle(
        header: McuMgrHeader,
        payload: ByteArray,
        responseType: Class<T>
    ): T {
        return when (header.commandId) {
            McuMgrStatsCommand.LIST.value -> handleStatsListRequest(header, responseType)
            McuMgrStatsCommand.READ.value -> handleStatsReadRequest(header, payload, responseType)
            else -> throw IllegalArgumentException("Unimplemented command with ID ${header.commandId}")
        }
    }

    /**
     * Handle a stats list request.
     */
    private fun <T : McuMgrResponse?> handleStatsListRequest(
        header: McuMgrHeader,
        responseType: Class<T>
    ): T {
        val response = McuMgrStatListResponse().apply {
            stat_list = stats.keys.toTypedArray()
        }
        val responsePayload = CBOR.toBytes(response)
        return buildMockResponse(header.toResponse(), responsePayload, responseType)
    }

    /**
     * Handle a stats read request.
     */
    private fun <T : McuMgrResponse?> handleStatsReadRequest(
        header: McuMgrHeader,
        payload: ByteArray,
        responseType: Class<T>
    ): T {
        val requestName = CBOR.getString(payload, "name")
        val fields = stats[requestName]
        val response = if (fields != null) {
            McuMgrStatResponse().apply {
                this.name = requestName
                this.fields = fields
            }
        } else {
            // If the stat group is not found, return an error code.
            McuMgrErrorResponse(McuMgrErrorCode.IN_VALUE)
        }
        val responsePayload = CBOR.toBytes(response)
        return buildMockResponse(header.toResponse(), responsePayload, responseType)
    }
}
