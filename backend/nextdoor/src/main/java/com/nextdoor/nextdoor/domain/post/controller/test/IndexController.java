package com.nextdoor.nextdoor.domain.post.controller.test;

import com.nextdoor.nextdoor.domain.post.search.PostIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class IndexController {

    private final PostIndexService postIndexService;

    @PostMapping("/index-all")
    public ResponseEntity<String> indexAll() {
        postIndexService.reindexAll();
        return ResponseEntity.ok("전체 인덱싱 성공");
    }
}