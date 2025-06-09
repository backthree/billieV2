package com.nextdoor.nextdoor.domain.rentalreservation.listener;

import com.nextdoor.nextdoor.domain.aianalysis.event.out.AiAnalysisCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiAnalysisCompletedEventListener {

    private final RentalService rentalService;
    private final SimpMessagingTemplate messagingTemplate;

    @Async("asyncExecutor")
    @EventListener
    public void handleAiAnalysisCompletedEvent(AiAnalysisCompletedEvent aiAnalysisCompletedEvent) {
        rentalService.updateDamageAnalysis(
                aiAnalysisCompletedEvent.getRentalId(),
                aiAnalysisCompletedEvent.getDamageAnalysis()
        );
    }
}