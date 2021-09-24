package com.chida.sampriti.protal.backend_ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

@SpringBootApplication
@ComponentScan(basePackages = {"com.chida.sampriti.protal.backend_ws"})
public class BackendWsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendWsApplication.class, args);
	}


}
