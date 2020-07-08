package com.juul.mcumgr.serialization

import com.fasterxml.jackson.databind.node.ObjectNode
import com.juul.mcumgr.McuMgrResult
import com.juul.mcumgr.message.Format
import com.juul.mcumgr.message.Response
import okio.Buffer
import okio.BufferedSource

fun <T : Response> ByteArray.decode(format: Format, type: Class<T>): McuMgrResult<T> {
    val message = decode(format)
    return message.toResult(type)
}

fun ByteArray.decode(format: Format): Message =
    when (format) {
        Format.STANDARD -> decodeStandard()
        Format.COAP -> decodeCoap()
    }

private fun ByteArray.decodeStandard(): Message {
    val buffer = Buffer().write(this)
    val header = buffer.decodeHeader()
    val payload = buffer.readByteArray().decodePayload()
    return Message(header, payload)
}

private fun ByteArray.decodeCoap(): Message {
    // Parse the header out of the CBOR map and decode it
    val rawHeader = cbor.readTree(this).get("_h").binaryValue()
    val headerBuffer = Buffer().write(rawHeader)
    val header = headerBuffer.decodeHeader()
    val payload = decodePayload()
    return Message(header, payload)
}

private fun ByteArray.decodePayload(): ObjectNode = cbor.readTree(this) as ObjectNode

private fun BufferedSource.decodeHeader(): Header {
    val operation = readByte().toUnsignedInteger()
    val flags = readByte()
    val length = readShort().toUnsignedInteger()
    val group = readShort().toUnsignedInteger()
    val sequenceNumber = readByte().toUnsignedInteger()
    val command = readByte().toUnsignedInteger()
    return Header(operation, group, command, length, sequenceNumber, flags)
}

// Helpers

private fun Byte.toUnsignedInteger(): Int = toInt() and 0xff

private fun Short.toUnsignedInteger(): Int = toInt() and 0xffff
