package de.uulm.automotive.cds

import de.uulm.automotive.cds.repositories.MessageRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * TODO
 *
 */
@Configuration
class ApplicationConfiguration {

    /**
     * TODO
     *
     * @param messageRepository
     */
    @Bean
    fun databaseInitializer(messageRepository: MessageRepository) = ApplicationRunner {

    }
}