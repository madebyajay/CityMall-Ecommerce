package com.citymall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@ConfigurationPropertiesScan("com.citymall.config")
public class CustomerServiceCitymallApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceCitymallApplication.class, args);
		System.out.println("Customer Service is running Successfully : welcome to citymall");
	}

}
