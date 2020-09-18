package io.runtime.mcumgr.transfer

import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.ImageManager
import io.runtime.mcumgr.response.UploadResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.ByteString.Companion.toByteString
import java.lang.IllegalStateException
import kotlin.coroutines.resume

private const val OP_WRITE = 2
private const val ID_UPLOAD = 1

fun ImageManager.windowUpload(
    data: ByteArray,
    windowCapacity: Int,
    callback: UploadCallback
): TransferController {
    val uploader = ImageUploader(data, this, windowCapacity)
    val upload = GlobalScope.launch {
        uploader.progress.onEach { progress ->
            callback.onUploadProgressChanged(
                progress.offset,
                progress.size,
                System.currentTimeMillis()
            )
        }
    }
    upload.invokeOnCompletion { throwable ->
        when (throwable) {
            null -> callback.onUploadCompleted()
            is CancellationException -> callback.onUploadCanceled()
            is McuMgrException -> callback.onUploadFailed(throwable)
            else -> callback.onUploadFailed(McuMgrException(throwable))
        }
    }

    return object : TransferController {
        override fun pause() = throw IllegalStateException("cannot pause window upload")
        override fun resume() = throw IllegalStateException("cannot resume window upload")
        override fun cancel() {
            upload.cancel()
        }
    }
}

internal class ImageUploader(
    private val imageData: ByteArray,
    private val imageManager: ImageManager,
    windowCapacity: Int = 1
) : Uploader(
    imageData,
    windowCapacity,
    imageManager.mtu,
    imageManager.scheme
) {

    private val truncatedHash =
        imageData.toByteString().sha256().toByteArray().copyOfRange(0, TRUNCATED_HASH_LEN)

    @Throws
    override suspend fun write(
        data: ByteArray,
        offset: Int,
        length: Int?
    ): UploadResult {
        val requestMap: MutableMap<String, Any> = mutableMapOf(
            "data" to data,
            "off" to offset
        )
        if (offset == 0) {
            requestMap["len"] = imageData.size
            requestMap["sha"] = truncatedHash
        }
        return imageManager.upload(requestMap)
    }
}

@Throws
private suspend fun ImageManager.upload(
    requestMap: Map<String, Any>
): UploadResult = suspendCancellableCoroutine { cont ->
    send(
        OP_WRITE,
        ID_UPLOAD,
        requestMap,
        UploadResponse::class.java,
        object : McuMgrCallback<UploadResponse> {
            override fun onResponse(response: UploadResponse) {
                cont.resume(UploadResult.Response(response, response.returnCode))
            }

            override fun onError(error: McuMgrException) {
                cont.resume(UploadResult.Failure(error))
            }
        }
    )
}
