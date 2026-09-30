package com.pvp.travelmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TravelmatchApplication {
	public static void main(String[] args) {
		SpringApplication.run(TravelmatchApplication.class, args);
	}
}
