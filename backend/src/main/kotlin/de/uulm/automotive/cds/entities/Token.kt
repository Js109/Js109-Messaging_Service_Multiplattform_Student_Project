package de.uulm.automotive.cds.entities

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
/**
 * Maps unique registration Id from the Client to his corresponding QueueId.
 *
 * @property signUpToken SignUp token generated by the client.
 * @property queueId UUID generated by the backend as queueID for the client.
 * @property timeLastUsed Time this token was last used.
 * @property id
 */
class Token(
    var signUpToken: UUID,
    var queueId: UUID,
    var timeLastUsed: LocalDateTime,
    @Id @GeneratedValue var id: Long? = null
)