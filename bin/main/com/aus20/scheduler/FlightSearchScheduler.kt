package com.aus20.scheduler

import com.aus20.service.UserFlightSearchService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FlightSearchScheduler(
    private val userFlightSearchService: UserFlightSearchService
) {

    @Scheduled(cron = "0 0 0 * * ?") // Executes every day at midnight
    //@Scheduled(cron = "0 */10 * * * ?") // Her 10 dakikada bir çalışır
    //@Scheduled(cron = "0/30 * * * * ?") // Her 30 saniyede bir çalışır
    fun performScheduledSearches() {
        println("Starting scheduled flight search execution...")
        userFlightSearchService.executePeriodicSearches()
        println("Completed scheduled flight search execution.")
    }
}