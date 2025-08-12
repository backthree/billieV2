package com.nextdoor.nextdoor.domain.post.search;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReindexRunState {
    public enum Status { RUNNING, COMPLETED, ABORTED }

    private String runId;
    private Instant cutoff;
    private long lastId;
    private long processed;
    private Status status;
    private Instant startedAt;
    private Instant updatedAt;
    private String abortReason;
}