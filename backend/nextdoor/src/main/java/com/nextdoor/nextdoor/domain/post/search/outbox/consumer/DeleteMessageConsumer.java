package com.nextdoor.nextdoor.domain.post.search.outbox.consumer;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.VersionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextdoor.nextdoor.domain.post.search.outbox.Jsons;
import com.nextdoor.nextdoor.domain.post.search.outbox.event.PostDeleteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Profile("worker")
@Slf4j
public class DeleteMessageConsumer {
    private final ElasticsearchAsyncClient es;
    private final Jsons jsons;

    public void process(String msg) {
        try {
            PostDeleteEvent e = jsons.toDelete(msg);
            log.debug("삭제 메시지 처리 중 (게시물 ID={})", e.getPostId());

            es.delete(d -> d
                .index("posts")
                .id(String.valueOf(e.getPostId()))
                .version(e.getVersion())
                .versionType(VersionType.ExternalGte)
            ).join();

            log.info("게시물 문서 삭제 완료 (게시물 ID={})", e.getPostId());
        } catch (Exception e) {
            log.error("삭제 메시지 처리 중 오류 발생: {}", msg, e);
        }
    }
}
