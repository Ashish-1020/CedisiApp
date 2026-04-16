package com.example.ragApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RagAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagAppApplication.class, args);
	}

}
