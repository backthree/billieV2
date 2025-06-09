package com.nextdoor.nextdoor.domain.rentalreservation.job;

import com.nextdoor.nextdoor.domain.rentalreservation.service.RentalEndService;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
public class RentalEndJob extends QuartzJobBean {

    @Autowired
    private ApplicationContext applicationContext;

    public RentalEndJob() {
        // 빈 생성자
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        Long rentalId = dataMap.getLong("rentalId");
        String serviceBeanName = dataMap.getString("rentalEndServiceBeanName");

        // ApplicationContext에서 서비스 가져오기
        RentalEndService rentalEndService = applicationContext.getBean(serviceBeanName, RentalEndService.class);

        System.out.println("[DEBUG_LOG] RentalEndJob executing for rental ID: " + rentalId);
        rentalEndService.rentalEnd(rentalId);
    }
}
