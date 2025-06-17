package com.nextdoor.nextdoor.domain.rentalreservation.application.service;

import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.RentalScheduleException;
import com.nextdoor.nextdoor.domain.rentalreservation.infrastructure.job.RentalEndJob;
import lombok.RequiredArgsConstructor;
import org.quartz.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class RentalScheduleService {

    private final Scheduler scheduler;
    @Lazy
    private final RentalEndService rentalEndService;

    public void scheduleRentalEnd(Long rentalId) {
        try {
            JobDetail endJob = JobBuilder.newJob(RentalEndJob.class)
                    .withIdentity("rentalEndJob" + rentalId, "rentalJobs")
                    .usingJobData("rentalId", rentalId)
                    .usingJobData("rentalEndServiceBeanName", "rentalEndService")
                    .build();

            //테스트 : 스케줄러 생성 20초 후에 실행
            Date runDateTime = new Date(System.currentTimeMillis() + 20 * 1000);
            System.out.println("[DEBUG_LOG] Scheduling job for rental ID: " + rentalId + " to run at: " + runDateTime);

            Trigger endTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("rentalEndTrigger_" + rentalId, "rentalTriggers")
                    .startAt(runDateTime)
                    .build();

            scheduler.scheduleJob(endJob, endTrigger);
            System.out.println("[DEBUG_LOG] Job scheduled successfully for rental ID: " + rentalId);

            if (scheduler.isStarted()) {
                System.out.println("[DEBUG_LOG] Scheduler is running");
            } else {
                System.out.println("[DEBUG_LOG] Scheduler is NOT running");
            }
        } catch (SchedulerException e) {
            System.out.println("[DEBUG_LOG] Error scheduling job: " + e.getMessage());
            throw new RentalScheduleException("대여 종료 스케줄 생성 실패", e);
        }
    }
}
