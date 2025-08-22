package com.nextdoor.nextdoor.domain.post.search.outbox;

import com.nextdoor.nextdoor.domain.post.search.outbox.event.PostDeleteEvent;
import com.nextdoor.nextdoor.domain.post.search.outbox.event.PostUpsertEvent;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqsPublisher {
    private final SqsAsyncClient sqsClient;
    private final Jsons jsons;
    private final MeterRegistry meterRegistry;

    @Value("${sqs.queue.upsert}")
    private String upsertQueueUrl;

    @Value("${sqs.queue.delete}")
    private String deleteQueueUrl;

    private final DistributionSummary upsertSize = DistributionSummary.builder("sqs.message.bytes")
            .description("SQS 메시지 크기").baseUnit("bytes")
            .publishPercentileHistogram().tag("type","UPSERT")
            .register(meterRegistry);

    private final DistributionSummary deleteSize = DistributionSummary.builder("sqs.message.bytes")
            .description("SQS 메시지 크기").baseUnit("bytes")
            .publishPercentileHistogram().tag("type","DELETE")
            .register(meterRegistry);

    public CompletableFuture<SendMessageResponse> sendDelete(String payload) {
        try {
            PostDeleteEvent ev = jsons.toDelete(payload);
            deleteSize.record(payload.getBytes(StandardCharsets.UTF_8).length);
            return sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(deleteQueueUrl)
                    .messageGroupId(String.valueOf(ev.getPostId()))
                    .messageDeduplicationId(dedupe(ev.getPostId(), ev.getVersion(), "DEL"))
                    .messageBody(payload)
                    .build());
        } catch (Exception e) {
            log.error("삭제 메시지 전송 중 오류 발생: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<SendMessageResponse> sendUpsert(String payload) {
        try {
            PostUpsertEvent ev = jsons.toUpsert(payload);
            upsertSize.record(payload.getBytes(StandardCharsets.UTF_8).length);
            return sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(upsertQueueUrl)
                    .messageGroupId(String.valueOf(ev.getPostId()))
                    .messageDeduplicationId(dedupe(ev.getPostId(), ev.getVersion(), "UPS"))
                    .messageBody(payload)
                    .build());
        } catch (Exception e) {
            log.error("업서트 메시지 전송 중 오류 발생: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String dedupe(Long id, Long v, String t) { return t + ":" + id + ":" + v; }
}