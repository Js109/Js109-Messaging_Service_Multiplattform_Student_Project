package de.uulm.automotive.cds.services

import com.rabbitmq.client.AMQP
import de.uulm.automotive.cds.entities.Message
import de.uulm.automotive.cds.entities.MessageSerializable
import de.uulm.automotive.cds.entities.TemplateMessage
import de.uulm.automotive.cds.models.dtos.MessageCompactDTO
import de.uulm.automotive.cds.repositories.MessageRepository
import org.hibernate.Hibernate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * A service class that takes care of sending messages via the amqp broker.
 * Relies on AmqpChannelService.
 *
 * @property amqpChannelService AmqpChannelService component used to communicate with the broker.
 */
@Component

class MessageService @Autowired constructor(val amqpChannelService: AmqpChannelService, val messageRepository: MessageRepository) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(MessageService::class.java)
    }

    /**
     * Publishes a messages to the broker.
     * If the message passed contains properties the topic will be ignored and header exchange will be used.
     * Otherwise topic exchange with the topic of the message will be used.
     * @param message The message to be published by the broker. If it has properties set those will be used for publishing, otherwise the topic of the message will be used for publishing.
     */
    fun sendMessage(message: Message) {
        val channel = amqpChannelService.openChannel()
        channel?.let {
            val messageSerializable =
                    MessageSerializable(
                            message.sender!!,
                            message.title!!,
                            message.content,
                            message.attachment,
                            message.logoAttachment,
                            message.links?.toTypedArray(),
                            message.locationData?.serialize(),
                            message.endtime,
                            message.messageDisplayProperties?.serialize()
                    )

            val properties = AMQP.BasicProperties.Builder()
            message.endtime?.millisFromCurrentTime()
                    ?.also { time -> if (time < 0) return }
                    ?.let { time -> addExpirationToProps(properties, time) }

            if (message.properties == null || message.properties?.size == 0) {
                it.basicPublish("amq.topic", message.topic, properties.build(), messageSerializable.toByteArray())
            } else {
                addHeaderProps(properties, message.properties)
                it.basicPublish("amq.headers", "", properties.build(), messageSerializable.toByteArray())
            }

            it.close()
        }
        if (channel == null) {
            logger.warn("Could not send message. Could not connect to broker.")
        }
    }

    /**
     * Calculates the amount of milliseconds between the given LocalDateTime and the current time
     */
    private fun LocalDateTime.millisFromCurrentTime(): Long? {
        return this.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()?.minus(System.currentTimeMillis())
    }

    /**
     * Adds an expiration date to the given AMQP property builder based on the current time and the given LocalDateTime if it is not noll null or in the past.
     * @param properties AMQP.BasicProperties.Builder to which the expiration will be added
     * @param expiration Duration in Long after which the message should not be send any more
     */
    private fun addExpirationToProps(properties: AMQP.BasicProperties.Builder, expiration: Long) {
        properties.expiration(expiration.toString())
    }

    /**
     * Adds the appropriate headers for message publishing on the header exchange based on the message properties to the given AMQP properties builder.
     * @param amqpPropertiesBuilder AMQP.BasicProperties.Builder to which the headers will be added
     * @param properties List of properties from the message to be published from which the headers are created
     */
    private fun addHeaderProps(amqpPropertiesBuilder: AMQP.BasicProperties.Builder, properties: List<String>?) {
        val bindingArgs = HashMap<String, Any>()
        properties?.forEach {
            bindingArgs[it] = ""
        }
        amqpPropertiesBuilder.headers(bindingArgs)
    }

    /**
     * This method filters the message-list by their sending timestamp.
     * The transactional context is required to be able to lazy load only the properties and links of messages,
     * which should be sent.
     *
     * @return List of messages which should be processed (sent)
     */
    @Transactional
    fun filterCurrentMessages(): List<Message> {
        val messages = messageRepository.findAllByIsSentFalseOrderByStarttimeAsc()
                .filter { it::class.java != TemplateMessage::class.java }
        return messages.filter { message: Message ->
            val starttime = message.starttime
            starttime == null || starttime < LocalDateTime.now()
        }.apply {
            forEach { message ->
                // triggers the loading of the lazy-fetch attributes
                Hibernate.initialize(message.links)
                Hibernate.initialize(message.properties)
            }
        }
    }

    /**
     * TODO
     *
     * @param topicName
     * @param propertyName
     * @param searchString
     * @param timeSpanBegin
     * @param timeSpanEnd
     * @param sender
     * @param content
     * @param title
     * @return
     */
    @Transactional
    fun filterMessages(topicName: String? = null, propertyName: String? = null, searchString: String? = null,
                       timeSpanBegin: LocalDateTime? = null, timeSpanEnd: LocalDateTime? = null,
                       sender: String? = null, content: String? = null, title: String? = null): Iterable<MessageCompactDTO> =
            messageRepository
                    .findAllFiltered(
                            topicName = if (topicName.isNullOrBlank()) null else topicName,
                            propertyName = if (propertyName.isNullOrBlank()) null else propertyName,
                            searchString = if (searchString.isNullOrBlank()) "" else searchString,
                            dateBegin = timeSpanBegin,
                            dateEnd = timeSpanEnd,
                            sender = if (sender.isNullOrBlank()) "" else sender,
                            content = if (content.isNullOrBlank()) "" else content,
                            title = if (title.isNullOrBlank()) "" else title
                    )
                    .filter { it::class.java != TemplateMessage::class.java }
                    .map { MessageCompactDTO.toDTO(it) }
}