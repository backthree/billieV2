package com.nextdoor.nextdoor.domain.rentalreservation.service;

import com.nextdoor.nextdoor.domain.rentalreservation.exception.RentalScheduleException;
import com.nextdoor.nextdoor.domain.rentalreservation.job.RentalEndJob;
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
    private final RentalEndService rentalEndService; // 주입받기

    public void scheduleRentalEnd(Long rentalId) {
        try {
            JobDetail endJob = JobBuilder.newJob(RentalEndJob.class)
                    .withIdentity("rentalEndJob" + rentalId, "rentalJobs")
                    .usingJobData("rentalId", rentalId)
                    .usingJobData("rentalEndServiceBeanName", "rentalEndService") // 서비스 빈 이름 추가
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

            // Verify scheduler is running
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
