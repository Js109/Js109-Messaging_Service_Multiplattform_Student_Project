package de.uulm.automotive.cds.services

import com.fasterxml.jackson.databind.ObjectMapper
import de.uulm.automotive.cds.entities.Subscriptions
import de.uulm.automotive.cds.models.RabbitMQPropertyMetrics
import de.uulm.automotive.cds.models.RabbitMQTopicMetrics
import de.uulm.automotive.cds.models.dtos.MessageCompactDTO
import de.uulm.automotive.cds.models.dtos.MetricsDTO
import de.uulm.automotive.cds.models.dtos.MetricsFilterDTO
import de.uulm.automotive.cds.repositories.MessageRepository
import de.uulm.automotive.cds.repositories.SubscriptionsRepository
import khttp.structures.authorization.BasicAuthorization
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * TODO
 *
 */
@Component
class MetricsService @Autowired constructor(
        val messageRepository: MessageRepository,
        val messageService: MessageService,
        val subscriptionsRepository: SubscriptionsRepository,
        @Value("\${amq.broker.url}") private val address: String,
        @Value("\${amq.broker.http.port}") private val port: String,
        @Value("\${amq.broker.username}") private val username: String,
        @Value("\${amq.broker.password}") private val password: String
) {

    companion object {
        val mapper: ObjectMapper = ObjectMapper()
        val logger: Logger = LoggerFactory.getLogger(MetricsService::class.java)
    }

    /**
     * TODO
     *
     * @return
     */
    fun getTopicSubscriptionDistribution(): Map<String, Int> =
            subscriptionsRepository
                    .findAllFiltered(
                            dateBegin = LocalDate.now(),
                            dateEnd = LocalDate.now()
                    )
                    .filter {
                        it.topicName != null
                    }
                    .map { it.topicName!! to it.subscribers }
                    .toMap()

    /**
     * TODO
     *
     * @return
     */
    fun getPropertySubscriptionDistribution(): Map<String, Int> =
            subscriptionsRepository
                    .findAllFiltered(
                            dateBegin = LocalDate.now(),
                            dateEnd = LocalDate.now()
                    )
                    .filter {
                        it.propertyName != null
                    }
                    .map { it.propertyName!! to it.subscribers }
                    .toMap()

    /**
     * TODO
     *
     * @param metricsFilter
     * @return
     */
    fun getMetrics(metricsFilter: MetricsFilterDTO): MetricsDTO {
        val filterBeforeTimeSpan =
                metricsFilter.timeSpanBegin?.let { timeSpanBegin ->
                    MetricsFilterDTO(
                            metricsFilter.topicName,
                            metricsFilter.propertyName,
                            timeSpanEnd = timeSpanBegin.minusDays(1)
                    )
                }

        val filterAfterTimeSpan =
                metricsFilter.timeSpanEnd?.let { timeSpanEnd ->
                    MetricsFilterDTO(
                            metricsFilter.topicName,
                            metricsFilter.propertyName,
                            timeSpanBegin = timeSpanEnd.plusDays(1)
                    )
                }

        val filteredMessagesTimeSpan = messageService.filterMessages(
                topicName = metricsFilter.topicName,
                propertyName = metricsFilter.propertyName,
                timeSpanBegin = metricsFilter.timeSpanBegin
                        ?.let {
                            LocalDateTime.of(it, LocalTime.MIN)
                        },
                timeSpanEnd = metricsFilter.timeSpanEnd
                        ?.let {
                            LocalDateTime.of(it, LocalTime.MAX)
                        }

        )
        val filteredMessagesBeforeTimeSpan =
                when (filterBeforeTimeSpan) {
                    null -> listOf()
                    else -> messageService.filterMessages(
                            topicName = filterBeforeTimeSpan.topicName,
                            propertyName = filterBeforeTimeSpan.propertyName,
                            timeSpanBegin = filterBeforeTimeSpan.timeSpanBegin
                                    ?.let {
                                        LocalDateTime.of(it, LocalTime.MIN)
                                    },
                            timeSpanEnd = filterBeforeTimeSpan.timeSpanEnd
                                    ?.let {
                                        LocalDateTime.of(it, LocalTime.MAX)
                                    }
                    )
                }
        val filteredMessagesAfterTimeSpan =
                when (filterAfterTimeSpan) {
                    null -> listOf()
                    else -> messageService.filterMessages(
                            topicName = filterAfterTimeSpan.topicName,
                            propertyName = filterAfterTimeSpan.propertyName,
                            timeSpanBegin = filterAfterTimeSpan.timeSpanBegin
                                    ?.let {
                                        LocalDateTime.of(it, LocalTime.MIN)
                                    },
                            timeSpanEnd = filterAfterTimeSpan.timeSpanEnd
                                    ?.let {
                                        LocalDateTime.of(it, LocalTime.MAX)
                                    }
                    )
                }

        val filteredMessagesAllTime = filteredMessagesBeforeTimeSpan
                .union(filteredMessagesTimeSpan)
                .union(filteredMessagesAfterTimeSpan)

        val subscribersBegin: Int? =
                metricsFilter.timeSpanBegin
                        ?.let {
                            subscriptionsRepository.findByDateFiltered(
                                    date = it,
                                    topicName = metricsFilter.topicName,
                                    propertyName = metricsFilter.propertyName
                            )?.subscribers
                        }
        val subscribersEnd: Int? =
                metricsFilter.timeSpanEnd
                        ?.let {
                            subscriptionsRepository.findByDateFiltered(
                                    date = it,
                                    topicName = metricsFilter.topicName,
                                    propertyName = metricsFilter.propertyName
                            )?.subscribers
                        }

        return MetricsDTO(
                sentMessagesTotalAllTime = getTotalMessagesFilteredByIsSent(
                        filteredMessagesAllTime,
                        isSent = true
                ),
                sentMessagesTotalAtBegin = getTotalMessagesFilteredByIsSent(
                        filteredMessagesBeforeTimeSpan,
                        isSent = true
                ),
                sentMessagesTotalGain = getTotalMessagesFilteredByIsSent(
                        filteredMessagesTimeSpan,
                        isSent = true
                ),
                scheduledMessagesTotalAllTime = getTotalMessagesFilteredByIsSent(
                        filteredMessagesAllTime,
                        isSent = false
                ),
                scheduledMessagesTimeSpan = getTotalMessagesFilteredByIsSent(
                        filteredMessagesTimeSpan,
                        isSent = false
                ),
                subscriberTotalAllTime = subscriptionsRepository
                        .findByMaxDateFiltered()
                        ?.subscribers,
                subscriberTotalAtBegin = subscribersBegin,
                subscriberTotalGainOverTimespan =
                if (subscribersEnd != null && subscribersBegin != null)
                    subscribersEnd - subscribersBegin
                else
                    null,
                averageMessageLengthAllTime = getAverageMessageLength(
                        filteredMessagesAllTime
                ),
                averageMessageLengthTimeSpan = getAverageMessageLength(
                        filteredMessagesTimeSpan
                ),
                mostActiveSenderAllTime = getMostActiveSender(
                        filteredMessagesAllTime
                ),
                mostActiveSenderTimeSpan = getMostActiveSender(
                        filteredMessagesTimeSpan
                ),
                mostActiveWeekdayAllTime = getMostActiveWeekday(
                        filteredMessagesAllTime
                ),
                mostActiveWeekdayTimeSpan = getMostActiveWeekday(
                        filteredMessagesTimeSpan
                ),
                sentMessagesByTimeOfDayAllTime = getMessagesByTimeOfDayFilteredByIsSent(
                        filteredMessagesAllTime,
                        isSent = true
                ),
                sentMessagesByTimeOfDayTimeSpan = getMessagesByTimeOfDayFilteredByIsSent(
                        filteredMessagesTimeSpan,
                        isSent = true
                ),
                sentMessagesByDateTimeSpan = getMessagesByDateFilteredByIsSent(
                        filteredMessagesTimeSpan,
                        isSent = true
                ),
                scheduledMessagesByDateTimeSpan = getMessagesByDateFilteredByIsSent(
                        filteredMessagesTimeSpan,
                        isSent = false
                ),
                subscriberGainByDateTimeSpan = getSubscriberGainOverTimeSpan(
                        metricsFilter
                )
        )
    }

    /**
     * TODO
     *
     * @param messages
     * @param isSent
     * @return
     */
    private fun getTotalMessagesFilteredByIsSent(messages: Iterable<MessageCompactDTO>, isSent: Boolean): Int? =
            messages
                    .filter { it.isSent == isSent }
                    .count()

    /**
     * TODO
     *
     * @param messages
     * @return
     */
    private fun getAverageMessageLength(messages: Iterable<MessageCompactDTO>): Double? =
            if (messages.count() > 0) {
                messages
                        .map { it.content?.length ?: 0 }
                        .average()
            } else null

    /**
     * TODO
     *
     * @param messages
     * @return
     */
    private fun getMostActiveSender(messages: Iterable<MessageCompactDTO>): String? =
            messages
                    .map { it.sender }
                    .groupingBy { it }
                    .eachCount()
                    .maxBy { it.value }?.key

    /**
     * TODO
     *
     * @param messages
     * @return
     */
    private fun getMostActiveWeekday(messages: Iterable<MessageCompactDTO>): DayOfWeek? =
            messages
                    .map { it.starttime?.dayOfWeek }
                    .groupingBy { it }
                    .eachCount()
                    .maxBy { it.value }?.key

    /**
     * TODO
     *
     * @param messages
     * @param isSent
     * @return
     */
    private fun getMessagesByTimeOfDayFilteredByIsSent(messages: Iterable<MessageCompactDTO>, isSent: Boolean): Map<Int, Int> =
            messages
                    .filter { it.isSent == isSent }
                    .map { it.starttime?.hour }
                    .groupingBy { it }
                    .eachCount()
                    .filter { it.key != null } as Map<Int, Int>

    /**
     * TODO
     *
     * @param messages
     * @param isSent
     * @return
     */
    private fun getMessagesByDateFilteredByIsSent(messages: Iterable<MessageCompactDTO>, isSent: Boolean): Map<LocalDate, Int> =
            messages
                    .filter { it.isSent == isSent }
                    .map { it.starttime?.toLocalDate() }
                    .groupingBy { it }
                    .eachCount()
                    .filter { it.key != null } as Map<LocalDate, Int>

    /**
     * TODO
     *
     */
    fun getAndSaveRabbitMQMetrics() =
        try {
            val response = khttp.get(
                    url = "http://$address:$port/api/bindings",
                    auth = BasicAuthorization(username, password)
            )

            val metricsList: MutableList<JSONObject> = mutableListOf()
            response.jsonArray
                    .forEach {
                        metricsList.add(it as JSONObject)
                    }

            savePropertySubscriberForEachProperty(metricsList)
            saveTopicSubscriberForEachTopic(metricsList)
            saveTotalSubscriber(metricsList)
        } catch (exception: ConnectException) {
            logger.error("Could not connect to Broker.")
        }



    /**
     * TODO
     *
     * @param metricsList
     */
    private fun saveTopicSubscriberForEachTopic(metricsList: MutableList<JSONObject>) {
        metricsList
                .filter { it["source"] != null && it["source"] == "amq.topic" }
                .map {
                    RabbitMQTopicMetrics(
                            clientID = it["destination"] as String,
                            topicName = it["routing_key"] as String
                    )
                }
                .groupBy(
                        { it.topicName },
                        { it.clientID }
                )
                .mapValues { it.value.distinct() }
                .forEach {
                    if (subscriptionsRepository.findByDateFiltered(
                                    date = LocalDate.now(),
                                    topicName = it.key
                            ) == null
                    ) {
                        val currentDate = LocalDate.now()
                        subscriptionsRepository.save(
                                Subscriptions(
                                        date = currentDate,
                                        subscribers = it.value.count(),
                                        subscribersGainedLastDay = subscriptionsRepository
                                                .findByDateFiltered(
                                                        date = currentDate.minusDays(1),
                                                        topicName = it.key
                                                )
                                                ?.let { subs ->
                                                    it.value.count() - subs.subscribers
                                                },
                                        propertyName = null,
                                        topicName = it.key
                                ))
                    }
                }
    }

    /**
     * TODO
     *
     * @param metricsList
     */
    private fun savePropertySubscriberForEachProperty(metricsList: MutableList<JSONObject>) {
        metricsList
                .filter { it["source"] != null && it["source"] == "amq.headers" }
                .map { it["arguments"] }
                .mapNotNull {
                    metricsJsonMapToPropertyMetrics(
                            mapper.readValue(
                                    it.toString(),
                                    HashMap::class.java
                            ) as HashMap<String, String>
                    )
                }
                .groupBy(
                        { it.propertyName },
                        { it.clientID }
                )
                .mapValues { it.value.distinct() }
                .forEach {
                    if (subscriptionsRepository.findByDateFiltered(
                                    date = LocalDate.now(),
                                    propertyName = it.key
                            ) == null
                    ) {
                        val currentDate = LocalDate.now()
                        subscriptionsRepository.save(
                                Subscriptions(
                                        date = currentDate,
                                        subscribers = it.value.count(),
                                        subscribersGainedLastDay = subscriptionsRepository
                                                .findByDateFiltered(
                                                        date = currentDate.minusDays(1),
                                                        propertyName = it.key
                                                )
                                                ?.let { subs ->
                                                    it.value.count() - subs.subscribers
                                                },
                                        propertyName = it.key,
                                        topicName = null
                                ))
                    }
                }
    }

    /**
     * TODO
     *
     * @param map
     * @return
     */
    private fun metricsJsonMapToPropertyMetrics(map: HashMap<String, String>): RabbitMQPropertyMetrics? {
        var property: String? = null
        var clientID: String? = null

        map.forEach {
            if (it.key.startsWith("properties/"))
                property = it.key
            else if (it.key.startsWith("id/"))
                clientID = it.key
        }

        return if (property != null && clientID != null) {
            RabbitMQPropertyMetrics(
                    clientID = clientID!!,
                    propertyName = property!!
            )
        } else null
    }

    /**
     * TODO
     *
     * @param metricsList
     */
    private fun saveTotalSubscriber(metricsList: MutableList<JSONObject>) {
        val currentDate: LocalDate = LocalDate.now()
        if (subscriptionsRepository.findByDateFiltered(date = currentDate) == null) {
            getTotalSubscriberFromMetricsJsonMap(metricsList)
                    .let { currentSubscribers ->
                        subscriptionsRepository.save(Subscriptions(
                                date = LocalDate.now(),
                                subscribers = currentSubscribers,
                                subscribersGainedLastDay = subscriptionsRepository
                                        .findByDateFiltered(date = currentDate.minusDays(1))
                                        ?.let {
                                            currentSubscribers - it.subscribers
                                        },
                                propertyName = null,
                                topicName = null
                        ))
                    }

        }
    }

    /**
     * TODO
     *
     * @param metricsList
     * @return
     */
    private fun getTotalSubscriberFromMetricsJsonMap(metricsList: MutableList<JSONObject>): Int =
            metricsList
                    .filter {
                        it["source"] != null && it["source"] == "amq.headers"
                    }
                    .mapNotNull {
                        metricsJsonMapToPropertyMetrics(
                                mapper.readValue(
                                        it["arguments"].toString(),
                                        HashMap::class.java
                                ) as HashMap<String, String>
                        )?.clientID
                    }
                    .distinct()
                    .count()

    /**
     * TODO
     *
     * @param metricsFilter
     * @return
     */
    private fun getSubscriberGainOverTimeSpan(metricsFilter: MetricsFilterDTO): Map<LocalDate, Int> =
            subscriptionsRepository
                    .findAllFiltered(
                            dateBegin = metricsFilter.timeSpanBegin,
                            dateEnd = metricsFilter.timeSpanEnd,
                            propertyName = metricsFilter.propertyName,
                            topicName = metricsFilter.topicName
                    )
                    .mapNotNull {
                        it.subscribersGainedLastDay?.let { sgld ->
                            it.date to sgld
                        }
                    }
                    .toMap()
}