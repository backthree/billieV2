package com.nextdoor.nextdoor.domain.post.search.outbox.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class SqsDeleteListener {
    private final DeleteMessageConsumer consumer;

    @SqsListener("${sqs.queue.delete}")
    public void onMessage(String message) {
        try {
            log.debug("삭제 메시지 수신: {}", message);
            consumer.process(message);
        } catch (Exception e) {
            log.error("삭제 메시지 처리 중 오류 발생: {}", message, e);
        }
    }
}
