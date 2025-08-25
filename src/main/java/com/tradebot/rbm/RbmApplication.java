package com.tradebot.rbm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RbmApplication {

	public static void main(String[] args) {
		SpringApplication.run(RbmApplication.class, args);
	}

}
