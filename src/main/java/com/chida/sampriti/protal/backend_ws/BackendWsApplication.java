package com.chida.sampriti.protal.backend_ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.File;

import static com.chida.sampriti.protal.backend_ws.constant.FileConstant.USER_FOLDER;

@SpringBootApplication
@ComponentScan(basePackages = {"com.chida.sampriti.protal.backend_ws"})
public class BackendWsApplication {

	public static void main(String[] args) {

		SpringApplication.run(BackendWsApplication.class, args);
		new File(USER_FOLDER).mkdirs();
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
