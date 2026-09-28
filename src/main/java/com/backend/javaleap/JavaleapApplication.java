package com.backend.javaleap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * PadaShakti - Smart Urban Subway Kinetic Floor Energy Harvesting Network.
 * <p>
 * Entry point of the Spring Boot backend.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class JavaleapApplication {

	/** All timestamps are Indian Standard Time - same as the MySQL server. */
	public static final String APP_TIME_ZONE = "Asia/Kolkata";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(APP_TIME_ZONE));
		SpringApplication.run(JavaleapApplication.class, args);
	}
}
