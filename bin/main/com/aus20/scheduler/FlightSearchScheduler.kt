package com.aus20.scheduler

import com.aus20.service.UserFlightSearchService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FlightSearchScheduler(
    private val userFlightSearchService: UserFlightSearchService
) {

    @Scheduled(cron = "0 0 0 * * ?") // Her gün 00:00'da çalışır
    //@Scheduled(cron = "0 */10 * * * ?") // Her 10 dakikada bir çalışır
    //@Scheduled(cron = "0/30 * * * * ?") // Her 30 saniyede bir çalışır
    //@Scheduled(cron = "0 0/4 * * * ?") // Her 4 dakikada bir çalışır
    //@Scheduled(cron = "0 0/2 * * * ?") // Her 2 dakikada bir çalışır
    //@Scheduled(cron = "0 0/1 * * * ?") // Her 1 dakikada bir çalışır
    fun performScheduledSearches() {
        println("Starting scheduled flight search execution...")
        userFlightSearchService.executePeriodicSearches()
        println("Completed scheduled flight search execution.")
    }
}