package com.citymall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableCaching
public class ProductServiceCitymallApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductServiceCitymallApplication.class, args);
		System.out.println("Product Service is started : welcome to citymall");
	}

}
