package com.juul.mcumgr.serialization

import com.fasterxml.jackson.databind.node.ObjectNode
import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Request
import okio.Buffer
import okio.BufferedSink

fun Request.encode(format: Format, sequenceNumber: Int = 0): ByteArray {
    val header = Header(
        operation.value,
        group.value,
        command.value,
        0,
        sequenceNumber,
        0
    )
    val payload: ObjectNode = cbor.valueToTree(this)
    val message = Message(header, payload)
    return message.encode(format)
}

fun Message.encode(format: Format): ByteArray =
    when (format) {
        Format.STANDARD -> encodeStandard()
        Format.COAP -> encodeCoap()
    }

private fun Message.encodeStandard(): ByteArray =
    Buffer().apply {
        // Set the header's length to the size of the payload.
        val payloadBytes = encodePayload()
        val lengthSetHeader = header.copy(length = payloadBytes.size)
        encodeHeader(lengthSetHeader)
        write(payloadBytes)
    }.readByteArray()

private fun Message.encodeCoap(): ByteArray {
    val payloadBytes = encodePayload()
    val lengthSetHeader = header.copy(length = payloadBytes.size)
    val headerBytes = Buffer().apply {
        // Set the header's length to the size of the payload.
        encodeHeader(lengthSetHeader)
    }
    payload.put("_h", headerBytes.readByteArray())
    return encodePayload()
}

// Helpers

private fun Message.encodePayload(): ByteArray =
    cbor.writeValueAsBytes(payload)

private fun BufferedSink.encodeHeader(header: Header) {
    writeByte(header.operation)
    writeByte(header.flags.toInt())
    writeShort(header.length)
    writeShort(header.group)
    writeByte(header.sequenceNumber)
    writeByte(header.command)
}
