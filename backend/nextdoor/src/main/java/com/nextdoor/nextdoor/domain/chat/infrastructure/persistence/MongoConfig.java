package com.nextdoor.nextdoor.domain.chat.infrastructure.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.nextdoor.nextdoor.domain.chat.infrastructure.persistence")
//기본 역할은 @EnableMongoRepositories 를 통해, 지정한 패키지(infrastructure/persistence)
//밑의 MongoRepository 인터페이스를 스캔·등록하도록 하는 것 입니다
public class MongoConfig {
    // 추가 커스터마이징이 필요없으면 이 클래스만 남겨두세요.
}
