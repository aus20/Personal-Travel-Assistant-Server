package com.aus20

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import com.aus20.config.AmadeusPropHolder
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(AmadeusPropHolder::class)
@EnableScheduling
class PersonalTravelAssistantBackendApplication

fun main(args: Array<String>) {
    runApplication<PersonalTravelAssistantBackendApplication>(*args)
}