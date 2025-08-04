package com.nextdoor.nextdoor.domain.post.search.messaging.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
@RequiredArgsConstructor
@Slf4j
public class SqsMessageListener {

    private final MessageConsumer messageConsumer;

    @Value("${sqs.queue.name}")
    private String queueName;

    @SqsListener("${sqs.queue.name}")
    public void receiveMessage(String message) throws Exception {
        messageConsumer.processMessage(message);
    }
}
