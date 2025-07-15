package com.nextdoor.nextdoor;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ElasticsearchDataAutoConfiguration.class})
@EnableAsync
@EnableRabbit
public class NextdoorApplication {

	public static void main(String[] args) {
		SpringApplication.run(NextdoorApplication.class, args);
	}

}
