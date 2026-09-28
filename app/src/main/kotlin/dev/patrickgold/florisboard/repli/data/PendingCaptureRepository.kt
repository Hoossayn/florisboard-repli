package dev.patrickgold.florisboard.repli.data

import android.content.Context
import dev.patrickgold.florisboard.ime.nlp.latin.repli.EncryptedFileStore
import dev.patrickgold.florisboard.ime.nlp.latin.repli.readBoundedString
import dev.patrickgold.florisboard.ime.nlp.latin.repli.writeBoundedString
import dev.patrickgold.florisboard.repli.capture.MessageSide
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

data class PendingCapture(
    val profileId: String,
    val profileName: String,
    val messageSide: MessageSide,
    val messages: List<String>,
)

class PendingCaptureRepository(context: Context) {
    private val store = EncryptedFileStore(context.noBackupFilesDir)
    private val target = store.file(FILE_NAME)

    fun save(capture: PendingCapture) {
        store.write(target, KEY_ALIAS) { output ->
            output.writeInt(FILE_VERSION)
            output.writeBoundedString(capture.profileId)
            output.writeBoundedString(capture.profileName)
            output.writeUTF(capture.messageSide.name)
            output.writeInt(capture.messages.size.coerceAtMost(MAX_MESSAGES))
            capture.messages.take(MAX_MESSAGES).forEach { message ->
                output.writeBoundedString(message, MAX_STRING_BYTES)
            }
        }
    }

    fun get(): PendingCapture? =
        store.read(target, KEY_ALIAS) { input ->
            require(input.readInt() == FILE_VERSION)
            val profileId = input.readBoundedString(MAX_STRING_BYTES)
            val profileName = input.readBoundedString(MAX_STRING_BYTES)
            val side = MessageSide.valueOf(input.readUTF())
            val count = input.readInt().also { require(it in 0..MAX_MESSAGES) }
            PendingCapture(profileId, profileName, side, List(count) { input.readBoundedString(MAX_STRING_BYTES) })
        }

    fun clear() {
        store.delete(target)
    }

    private companion object {
        const val FILE_NAME = "pending_scrolling_capture.bin"
        const val FILE_VERSION = 1
        const val KEY_ALIAS = "assisted_pending_capture_key_v1"
        const val MAX_MESSAGES = 500
        const val MAX_STRING_BYTES = 16_000
    }
}
