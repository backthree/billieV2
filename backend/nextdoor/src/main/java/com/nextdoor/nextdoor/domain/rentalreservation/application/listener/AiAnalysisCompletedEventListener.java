package com.nextdoor.nextdoor.domain.rentalreservation.application.listener;

import com.nextdoor.nextdoor.domain.aianalysis.event.out.AiAnalysisCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.application.service.RentalImageAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiAnalysisCompletedEventListener {

    private final RentalImageAnalysisService rentalImageAnalysisService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("asyncExecutor")
    @EventListener
    public void handleAiAnalysisCompletedEvent(AiAnalysisCompletedEvent aiAnalysisCompletedEvent) {
        rentalImageAnalysisService.updateDamageAnalysis(
                aiAnalysisCompletedEvent.getRentalId(),
                aiAnalysisCompletedEvent.getDamageAnalysis()
        );
    }
}
