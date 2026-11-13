package com.example.apimonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiMonitoringSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiMonitoringSystemApplication.class, args);
	}

}
