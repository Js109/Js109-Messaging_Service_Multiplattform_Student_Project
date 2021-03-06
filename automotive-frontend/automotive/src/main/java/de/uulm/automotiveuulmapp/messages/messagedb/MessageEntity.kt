package de.uulm.automotiveuulmapp.messages.messagedb

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.uulm.automotive.cds.models.Alignment
import de.uulm.automotive.cds.models.FontFamily
import java.net.URL

@Entity
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val uid: Int?,
    val sender: String,
    val title: String,
    val messageText: String?,
    val attachment: ByteArray?,
    val logoAttachment: ByteArray?,
    val links: Array<URL>?,
    var favourite: Boolean = false,
    var read: Boolean = false,
    val fontColor: String?,
    val backgroundColor: String?,
    val fontFamily: FontFamily?,
    val alignment: Alignment?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEntity

        if (uid != other.uid) return false
        if (sender != other.sender) return false
        if (title != other.title) return false
        if (messageText != other.messageText) return false
        if (attachment != null) {
            if (other.attachment == null) return false
            if (!attachment.contentEquals(other.attachment)) return false
        } else if (other.attachment != null) return false
        if (logoAttachment != null) {
            if (other.logoAttachment == null) return false
            if (!logoAttachment.contentEquals(other.logoAttachment)) return false
        } else if (other.logoAttachment != null) return false
        if (links != null) {
            if (other.links == null) return false
            if (!links.contentEquals(other.links)) return false
        } else if (other.links != null) return false
        if (fontColor != other.fontColor) return false
        if (backgroundColor != other.backgroundColor) return false
        if (fontFamily != other.fontFamily) return false
        if (alignment != other.alignment) return false
        return true
    }

    override fun hashCode(): Int {
        var result = uid?.hashCode() ?: 0
        result = 31 * result + sender.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + messageText.hashCode()
        result = 31 * result + (attachment?.contentHashCode() ?: 0)
        result = 31 * result + (logoAttachment?.contentHashCode() ?: 0)
        result = 31 * result + (links?.contentHashCode() ?: 0)
        result = 31 * result + fontColor.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + fontFamily.hashCode()
        result = 31 * result + alignment.hashCode()
        return result
    }

}